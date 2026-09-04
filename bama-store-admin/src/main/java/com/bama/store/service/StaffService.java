package com.bama.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bama.store.common.BusinessException;
import com.bama.store.common.OrderNoUtil;
import com.bama.store.dto.StaffCreateRequest;
import com.bama.store.entity.Staff;
import com.bama.store.entity.StaffRole;
import com.bama.store.mapper.StaffMapper;
import com.bama.store.mapper.StaffRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 员工管理
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffMapper staffMapper;
    private final StaffRoleMapper staffRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /** 分页查询员工 */
    public Page<Staff> page(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<Staff> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Staff::getName, keyword).or().like(Staff::getPhone, keyword);
        }
        wrapper.orderByDesc(Staff::getId);
        return staffMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /** 新增员工并分配角色 */
    @Transactional
    public Long create(StaffCreateRequest req) {
        Long exists = staffMapper.selectCount(
                new LambdaQueryWrapper<Staff>().eq(Staff::getPhone, req.getPhone()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该手机号已被使用");
        }

        Staff staff = new Staff();
        staff.setStaffNo(generateStaffNo());
        staff.setName(req.getName());
        staff.setPhone(req.getPhone());
        staff.setPassword(passwordEncoder.encode(req.getPassword()));
        staff.setStoreId(req.getStoreId());
        staff.setStatus(1);
        staffMapper.insert(staff);

        for (Long roleId : req.getRoleIds()) {
            staffRoleMapper.insert(new StaffRole(staff.getId(), roleId));
        }
        return staff.getId();
    }

    /** 启用/停用 */
    public void updateStatus(Long staffId, Integer status) {
        Staff staff = new Staff();
        staff.setId(staffId);
        staff.setStatus(status);
        staffMapper.updateById(staff);
    }

    /** 重置密码 */
    public void resetPassword(Long staffId, String newPassword) {
        Staff staff = new Staff();
        staff.setId(staffId);
        staff.setPassword(passwordEncoder.encode(newPassword));
        staffMapper.updateById(staff);
    }

    private String generateStaffNo() {
        return "BM-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy")) + "-"
                + OrderNoUtil.generate("").substring(8);
    }
}
