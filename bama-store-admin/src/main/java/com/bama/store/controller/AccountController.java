package com.bama.store.controller;

import com.bama.store.common.Result;
import com.bama.store.dto.ChargeConfirmRequest;
import com.bama.store.dto.RechargeRequest;
import com.bama.store.security.LoginStaff;
import com.bama.store.security.SecurityUtil;
import com.bama.store.service.AccountService;
import com.bama.store.service.PayCodeService;
import com.bama.store.vo.ChargeResultVo;
import com.bama.store.vo.MemberChargeInfoVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 会员账户：储值、付款码、扫码扣款
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final PayCodeService payCodeService;

    /** 会员储值 */
    @PostMapping("/account/recharge")
    @PreAuthorize("hasAuthority('account:recharge')")
    public Result<Void> recharge(@Valid @RequestBody RechargeRequest request) {
        LoginStaff staff = SecurityUtil.current();
        accountService.recharge(request, staff.getStaffId(), staff.getStoreId());
        return Result.success();
    }

    /**
     * 生成会员付款码（正式环境由顾客端小程序调用；此处供联调测试）
     */
    @PostMapping("/paycode/generate")
    @PreAuthorize("hasAuthority('charge:scan')")
    public Result<Map<String, String>> generatePayCode(@RequestParam Long memberId) {
        return Result.success(Map.of("payCode", payCodeService.generate(memberId)));
    }

    /** 扫码解析付款码，返回会员信息 */
    @PostMapping("/charge/resolve")
    @PreAuthorize("hasAuthority('charge:scan')")
    public Result<MemberChargeInfoVo> resolve(@RequestParam String payCode) {
        return Result.success(accountService.resolvePayCode(payCode));
    }

    /** 按完整手机号查询可扣款会员，沿用收款权限。 */
    @GetMapping("/charge/member")
    @PreAuthorize("hasAuthority('charge:scan')")
    public Result<MemberChargeInfoVo> memberByPhone(@RequestParam String phone) {
        return Result.success(accountService.resolvePhone(phone));
    }

    /** 确认扣款（扣会员卡余额） */
    @PostMapping("/charge/confirm")
    @PreAuthorize("hasAuthority('charge:scan')")
    public Result<ChargeResultVo> confirm(@Valid @RequestBody ChargeConfirmRequest request) {
        LoginStaff staff = SecurityUtil.current();
        return Result.success(accountService.charge(request, staff.getStaffId(), staff.getStoreId(), staff.getName()));
    }
}
