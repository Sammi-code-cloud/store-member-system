package com.bama.store.service;

import com.bama.store.entity.Permission;
import com.bama.store.entity.Role;
import com.bama.store.mapper.PermissionMapper;
import com.bama.store.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色与权限查询
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    /** 所有角色 */
    public List<Role> listRoles() {
        return roleMapper.selectList(null);
    }

    /** 所有权限 */
    public List<Permission> listPermissions() {
        return permissionMapper.selectList(null);
    }
}
