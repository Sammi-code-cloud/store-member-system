package com.bama.store.controller;

import com.bama.store.common.Result;
import com.bama.store.service.StaffWechatInvitations;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Employee binding is independent of the customer phone-verification flow. */
@RestController
@RequestMapping("/api/wechat/staff")
@RequiredArgsConstructor
public class StaffWechatController {
    private final StaffWechatInvitations invitations;

    public record BindRequest(String ticket, String code, String phone) {}

    @PostMapping("/bind")
    public Result<?> bind(@RequestBody BindRequest body, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Result.success(invitations.bind(body.ticket(), body.phone(), body.code()));
    }
}
