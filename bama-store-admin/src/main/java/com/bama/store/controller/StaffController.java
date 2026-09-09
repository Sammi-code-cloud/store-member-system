package com.bama.store.controller;

import com.bama.store.common.PageResult;
import com.bama.store.common.Result;
import com.bama.store.dto.StaffCreateRequest;
import com.bama.store.entity.Staff;
import com.bama.store.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping("/{id}/wechat-code")
    @PreAuthorize("hasAuthority('staff:manage')")
    public Result<?> wechatCode(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Result.success(staffService.bindCode(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('staff:view')")
    public Result<PageResult<Staff>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(staffService.page(pageNum, pageSize, keyword)));
    }

    /** 录入员工 */
    @PostMapping
    @PreAuthorize("hasAuthority('staff:manage')")
    public Result<Long> create(@Valid @RequestBody StaffCreateRequest request) {
        return Result.success(staffService.create(request));
    }

    /** 启用/停用 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('staff:manage')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        staffService.updateStatus(id, status);
        return Result.success();
    }

    /** 重置密码 */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('staff:manage')")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody PasswordRequest body) {
        staffService.resetPassword(id, body.password());
        return Result.success();
    }

    public record PasswordRequest(String password) {}
    public record UpdateRequest(String name, String phone, java.util.List<Long> roleIds, java.util.List<Long> storeIds) {}
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('staff:manage')")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateRequest body) {
        staffService.update(id, body.name(), body.phone(), body.roleIds(), body.storeIds()); return Result.success();
    }
}
