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
    private final com.bama.store.service.ReservationService reservations;

    @GetMapping
    @PreAuthorize("hasAuthority('member:view')")
    public Result<PageResult<Member>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String level,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) java.time.LocalDate startDate,
                                           @RequestParam(required = false) java.time.LocalDate endDate) {
        return Result.success(PageResult.of(memberService.page(pageNum, pageSize, keyword, level, status, startDate, endDate)));
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

    public record UpdateRequest(String name, String remark, Integer status) {}
    @GetMapping("/{id}/reservations")
    @PreAuthorize("hasAuthority('member:view')")
    public Result<PageResult<com.bama.store.entity.Reservation>> reservations(@PathVariable Long id, @RequestParam(defaultValue="1") long pageNum, @RequestParam(defaultValue="10") long pageSize) {
        memberService.getById(id);
        return Result.success(PageResult.of(reservations.page(pageNum, pageSize, null, null, null, null, id)));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('member:manage')")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateRequest body) {
        memberService.update(id, body.name(), body.remark(), body.status()); return Result.success();
    }
}
