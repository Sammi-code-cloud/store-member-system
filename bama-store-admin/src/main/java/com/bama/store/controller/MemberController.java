package com.bama.store.controller;

import com.bama.store.common.PageResult;
import com.bama.store.common.Result;
import com.bama.store.entity.Member;
import com.bama.store.entity.MemberAccount;
import com.bama.store.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 会员管理
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    @PreAuthorize("hasAuthority('member:view')")
    public Result<PageResult<Member>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(memberService.page(pageNum, pageSize, keyword)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('member:view')")
    public Result<Member> detail(@PathVariable Long id) {
        return Result.success(memberService.getById(id));
    }

    @GetMapping("/{id}/account")
    @PreAuthorize("hasAuthority('member:view')")
    public Result<MemberAccount> account(@PathVariable Long id) {
        return Result.success(memberService.getAccount(id));
    }
}
