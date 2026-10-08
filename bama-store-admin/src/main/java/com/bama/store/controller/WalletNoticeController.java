package com.bama.store.controller;
import com.bama.store.common.Result;
import com.bama.store.security.SecurityUtil;
import com.bama.store.service.WalletNotices;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/customer/wallet-notices") @RequiredArgsConstructor
public class WalletNoticeController {
    private final WalletNotices notices;
    public record Registration(String code) {}
    @GetMapping public Result<?> settings() { SecurityUtil.customerId();return Result.success(notices.settings()); }
    @PostMapping public Result<?> register(@RequestBody Registration input) { notices.register(SecurityUtil.customerId(),input.code());return Result.success(); }
}
