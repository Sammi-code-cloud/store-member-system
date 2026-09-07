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
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) java.time.LocalDate date,
                                                @RequestParam(required = false) Long roomId,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long memberId) {
        return Result.success(PageResult.of(reservationService.page(pageNum, pageSize, status, date, roomId, keyword, memberId)));
    }

    /** 创建预定 */
    @PostMapping
    @PreAuthorize("hasAuthority('reservation:manage')")
    public Result<String> create(@RequestBody Reservation reservation) {
        return Result.success(reservationService.create(reservation));
    }

    /** 核销预定 */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public Result<Void> confirm(@PathVariable Long id) { reservationService.confirm(id); return Result.success(); }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public Result<Void> reject(@PathVariable Long id, @RequestBody CancelRequest body) { reservationService.reject(id, body.reason()); return Result.success(); }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('reservation:verify')")
    public Result<Void> verify(@PathVariable Long id) {
        reservationService.verify(id, SecurityUtil.staffId());
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('reservation:verify')")
    public Result<Void> complete(@PathVariable Long id) { reservationService.complete(id); return Result.success(); }

    public record CancelRequest(String reason) {}
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody CancelRequest body) { reservationService.cancel(id, body.reason()); return Result.success(); }

    public record RescheduleRequest(java.time.LocalDate date, String startTime) {}
    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasAuthority('reservation:manage')")
    public Result<Void> schedule(@PathVariable Long id, @RequestBody RescheduleRequest body) { reservationService.reschedule(id, body.date(), body.startTime()); return Result.success(); }
}
