package com.bama.store.controller;

import com.bama.store.common.Result;
import com.bama.store.service.CustomerAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/auth")
@RequiredArgsConstructor
public class CustomerAuthController {
    private final CustomerAuthService service;
    public record Credentials(String username, String password, String name) {}
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Credentials body) {
        return Result.success(service.register(body.username(), body.password(), body.name()));
    }
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Credentials body) {
        return Result.success(service.login(body.username(), body.password()));
    }
}
