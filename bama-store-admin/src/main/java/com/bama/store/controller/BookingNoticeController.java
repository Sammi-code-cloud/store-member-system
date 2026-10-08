package com.bama.store.controller;
import com.bama.store.common.Result;
import com.bama.store.security.SecurityUtil;
import com.bama.store.service.BookingNotices;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/customer/booking-notices") @RequiredArgsConstructor
public class BookingNoticeController {
    private final BookingNotices notices;
    public record Registration(String code) {}
    @GetMapping public Result<?> settings() { SecurityUtil.customerId();return Result.success(notices.settings()); }
    @PostMapping public Result<?> register(@RequestBody Registration input) { notices.register(SecurityUtil.customerId(),input.code());return Result.success(); }
}
