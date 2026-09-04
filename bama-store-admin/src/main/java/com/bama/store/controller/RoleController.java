package com.bama.store.controller;

import com.bama.store.common.Result;
import com.bama.store.entity.Permission;
import com.bama.store.entity.Role;
import com.bama.store.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色与权限
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /** 角色列表（新增员工时选择） */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('staff:view')")
    public Result<List<Role>> roles() {
        return Result.success(roleService.listRoles());
    }

    /** 权限列表 */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('role:view')")
    public Result<List<Permission>> permissions() {
        return Result.success(roleService.listPermissions());
    }
}
