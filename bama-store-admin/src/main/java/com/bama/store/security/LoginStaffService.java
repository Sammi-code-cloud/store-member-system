package com.bama.store.security;

import com.bama.store.entity.Staff;
import com.bama.store.mapper.PermissionMapper;
import com.bama.store.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;

/**
 * 根据员工ID装配登录态（权限集合）
 */
@Service
@RequiredArgsConstructor
public class LoginStaffService {

    private final StaffMapper staffMapper;
    private final PermissionMapper permissionMapper;
    private final com.bama.store.service.StaffStoreService staffStores;

    /** 加载登录态；员工不存在或已停用返回 null */
    public LoginStaff load(Long staffId) {
        Staff staff = staffMapper.selectById(staffId);
        if (staff == null || staff.getStatus() == null || staff.getStatus() != 1) {
            return null;
        }
        LoginStaff loginStaff = new LoginStaff();
        loginStaff.setStaffId(staff.getId());
        loginStaff.setStaffNo(staff.getStaffNo());
        loginStaff.setName(staff.getName());
        loginStaff.setPhone(staff.getPhone());
        loginStaff.setStoreId(staff.getStoreId());
        loginStaff.setPermissions(new HashSet<>(permissionMapper.selectPermissionCodesByStaffId(staffId)));
        loginStaff.setStoreIds(staffStores.accessible(staff, loginStaff.getPermissions()));
        return loginStaff;
    }
}
