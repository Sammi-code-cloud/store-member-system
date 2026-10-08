package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bama.store.common.BusinessException;
import com.bama.store.common.OrderNoUtil;
import com.bama.store.dto.StaffCreateRequest;
import com.bama.store.entity.*;
import com.bama.store.mapper.*;
import com.bama.store.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StaffService {
    private final StaffMapper staffMapper;
    private final BusinessDictionary businessDictionary;
    private final StaffRoleMapper staffRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissions;
    private final PermissionMapper permissions;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final StaffStoreService staffStores;
    private final StoreMapper stores;
    private final WechatClient wechat;
    private final StaffWechatInvitations invitations;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Transactional
    public void updateWxpusher(Long id, String value) {
        businessDictionary.requireEnabled();
        Staff staff = require(id);
        String uid = value == null ? "" : value.trim();
        if (!uid.isEmpty() && !uid.matches("UID_[A-Za-z0-9_-]{1,100}"))
            throw new BusinessException("请填写正确的 WxPusher UID（UID_ 开头），或清空以关闭通知");
        // Serialize edits for this staff member, including concurrent configuration requests.
        jdbc.queryForObject("SELECT id FROM t_staff WHERE id=? FOR UPDATE", Long.class, id);
        jdbc.update("DELETE FROM t_staff_wxpusher WHERE store_id=? AND staff_id=?", SecurityUtil.storeId(), id);
        if (!uid.isEmpty()) jdbc.update("INSERT INTO t_staff_wxpusher(store_id,staff_id,uid) VALUES(?,?,?)", SecurityUtil.storeId(), id, uid);
        audit.record("预约通知设置", staff.getStaffNo(), uid.isEmpty() ? "关闭当前门店预约通知" : "更新当前门店预约通知 UID");
    }

    @Transactional
    public Map<String, Object> bindCode(Long id) {
        Staff staff = require(id);
        if (!Integer.valueOf(1).equals(staff.getStatus())) throw new BusinessException("员工已停用，无法生成绑定码");
        return Map.of("image", wechat.staffBindCode(invitations.create(id)), "staffName", staff.getName(), "expiresIn", 600);
    }

    public Page<Staff> page(long pageNum, long pageSize, String keyword) {
        var assigned = staffStores.staffInStore(SecurityUtil.storeId());
        var q = new LambdaQueryWrapper<Staff>().and(w -> {
            w.eq(Staff::getStoreId, SecurityUtil.storeId());
            if (!assigned.isEmpty()) w.or().in(Staff::getId, assigned);
        });
        if (keyword != null && !keyword.isBlank()) q.and(w -> w.like(Staff::getName, keyword).or().like(Staff::getPhone, keyword).or().like(Staff::getStaffNo, keyword));
        var page = staffMapper.selectPage(new Page<Staff>(Math.max(1, pageNum), Math.min(200, Math.max(1, pageSize))), q.orderByDesc(Staff::getId));
        for (Staff staff : page.getRecords()) {
            var noticeUids = jdbc.queryForList("SELECT uid FROM t_staff_wxpusher WHERE store_id=? AND staff_id=?", String.class, SecurityUtil.storeId(), staff.getId());
            staff.setWxpusherUid(noticeUids.isEmpty() ? "" : noticeUids.get(0));
            var ids = staffRoleMapper.selectList(new LambdaQueryWrapper<StaffRole>().eq(StaffRole::getStaffId, staff.getId())).stream().map(StaffRole::getRoleId).toList();
            staff.setRoleIds(ids); staff.setRoleNames(ids.isEmpty() ? List.of() : roleMapper.selectBatchIds(ids).stream().map(Role::getName).toList());
            staff.setStoreIds(staffStores.assigned(staff));
            staff.setStoreNames(stores.selectBatchIds(staff.getStoreIds()).stream().map(Store::getName).toList());
            staff.setAllStores(permissions.selectPermissionCodesByStaffId(staff.getId()).contains("store:all"));
        }
        return page;
    }

    private void validate(String name, String phone, List<Long> roleIds, Long id) {
        if (name == null || name.isBlank() || name.trim().length() > 32) throw new BusinessException("请填写 1–32 字的姓名");
        if (phone == null || !phone.matches("1[3-9]\\d{9}")) throw new BusinessException("请填写正确的手机号");
        if (staffMapper.selectCount(new LambdaQueryWrapper<Staff>().eq(Staff::getPhone, phone).ne(id != null, Staff::getId, id)) > 0) throw new BusinessException("该手机号已被使用");
        if (roleIds == null || roleIds.isEmpty() || roleIds.contains(null)) throw new BusinessException("请至少分配一个角色");
        var distinct = new HashSet<>(roleIds);
        if (roleMapper.selectBatchIds(distinct).size() != distinct.size()) throw new BusinessException("选择的角色不存在");
        var permissionIds = rolePermissions.selectList(new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, distinct)).stream().map(RolePermission::getPermissionId).distinct().toList();
        var codes = permissionIds.isEmpty() ? List.<String>of() : permissions.selectBatchIds(permissionIds).stream().map(Permission::getCode).toList();
        if (!SecurityUtil.current().getPermissions().containsAll(codes)) throw new BusinessException("不能分配超出本人权限的角色");
        if (SecurityUtil.staffId().equals(id) && !codes.contains("staff:manage")) throw new BusinessException("不能移除自己的员工管理权限");
    }

    private Staff require(Long id) {
        Staff staff = staffMapper.selectById(id);
        if (staff == null) throw new BusinessException("员工不存在");
        if (!staffStores.assigned(staff).contains(SecurityUtil.storeId()))
            throw new BusinessException(com.bama.store.common.ResultCode.FORBIDDEN);
        staffStores.requireManageable(staff);
        var targetCodes = permissions.selectPermissionCodesByStaffId(id);
        if (!SecurityUtil.current().getPermissions().containsAll(targetCodes))
            throw new BusinessException(com.bama.store.common.ResultCode.FORBIDDEN);
        return staff;
    }
    private void assign(Long id, List<Long> roleIds) {
        staffRoleMapper.delete(new LambdaQueryWrapper<StaffRole>().eq(StaffRole::getStaffId, id));
        for (Long roleId : new HashSet<>(roleIds)) staffRoleMapper.insert(new StaffRole(id, roleId));
    }
    private void password(String value) {
        if (value == null || value.length() < 8 || value.length() > 64) throw new BusinessException("密码需为 8–64 位");
    }

    @Transactional
    public Long create(StaffCreateRequest req) {
        businessDictionary.requireEnabled();
        validate(req.getName(), req.getPhone(), req.getRoleIds(), null); password(req.getPassword());
        var storeIds = staffStores.validate(req.getStoreIds() == null ? List.of(SecurityUtil.storeId()) : req.getStoreIds(), SecurityUtil.storeId());
        Staff staff = new Staff(); staff.setStaffNo(OrderNoUtil.generate("BM")); staff.setName(req.getName().trim()); staff.setPhone(req.getPhone());
        staff.setPassword(passwordEncoder.encode(req.getPassword())); staff.setStoreId(SecurityUtil.storeId()); staff.setStatus(1);
        staffMapper.insert(staff); assign(staff.getId(), req.getRoleIds());
        staffStores.assign(staff.getId(), storeIds);
        audit.record("添加员工", staff.getStaffNo(), staff.getName() + "，角色 " + req.getRoleIds() + "，分店 " + storeIds); return staff.getId();
    }

    @Transactional
    public void update(Long id, String name, String phone, List<Long> roleIds, List<Long> requestedStores) {
        businessDictionary.requireEnabled();
        Staff staff = require(id); validate(name, phone, roleIds, id);
        var beforeStores = staffStores.assigned(staff);
        var storeIds = staffStores.validate(requestedStores == null ? beforeStores : requestedStores, staff.getStoreId());
        String before = staff.getName() + " / " + staff.getPhone() + " / 角色 " + staffRoleMapper.selectList(new LambdaQueryWrapper<StaffRole>().eq(StaffRole::getStaffId, id)).stream().map(StaffRole::getRoleId).toList();
        staff.setName(name.trim()); staff.setPhone(phone); staffMapper.updateById(staff); assign(id, roleIds);
        staffStores.assign(id, storeIds);
        audit.record("编辑员工", staff.getStaffNo(), before + " / 分店 " + beforeStores + " → " + name.trim() + " / " + phone + " / 角色 " + roleIds + " / 分店 " + storeIds);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        businessDictionary.requireEnabled();
        Staff staff = require(id);
        if (status == null || status != 0 && status != 1) throw new BusinessException("状态无效");
        if (id.equals(SecurityUtil.staffId()) && status == 0) throw new BusinessException("不能停用当前登录账号");
        int before = staff.getStatus(); staff.setStatus(status); staffMapper.updateById(staff);
        audit.record("员工状态", staff.getStaffNo(), before + " → " + status);
    }

    @Transactional
    public void resetPassword(Long id, String value) {
        businessDictionary.requireEnabled();
        Staff staff = require(id); password(value); staff.setPassword(passwordEncoder.encode(value)); staffMapper.updateById(staff);
        audit.record("重置员工密码", staff.getStaffNo(), "已重置密码");
    }
}
