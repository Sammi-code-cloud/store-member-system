package com.bama.store.controller;

import com.bama.store.common.Result;
import com.bama.store.dto.LoginRequest;
import com.bama.store.dto.LoginResponse;
import com.bama.store.security.LoginStaff;
import com.bama.store.security.SecurityUtil;
import com.bama.store.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 员工登录 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /** 获取当前登录员工信息 */
    @GetMapping("/me")
    public Result<LoginStaff> me() {
        return Result.success(SecurityUtil.current());
    }
}
