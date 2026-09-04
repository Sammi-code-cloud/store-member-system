package com.bama.store.controller;

import com.bama.store.common.PageResult;
import com.bama.store.common.Result;
import com.bama.store.entity.Reservation;
import com.bama.store.security.SecurityUtil;
import com.bama.store.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 茶室预定
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    @PreAuthorize("hasAuthority('reservation:view')")
    public Result<PageResult<Reservation>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(reservationService.page(pageNum, pageSize, status)));
    }

    /** 创建预定 */
    @PostMapping
    @PreAuthorize("hasAuthority('reservation:manage')")
    public Result<String> create(@RequestBody Reservation reservation) {
        return Result.success(reservationService.create(reservation));
    }

    /** 核销预定 */
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('reservation:verify')")
    public Result<Void> verify(@PathVariable Long id) {
        reservationService.verify(id, SecurityUtil.staffId());
        return Result.success();
    }
}
