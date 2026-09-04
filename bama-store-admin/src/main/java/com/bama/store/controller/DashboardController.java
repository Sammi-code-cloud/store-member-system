package com.bama.store.controller;

import com.bama.store.common.Result;
import com.bama.store.service.DashboardService;
import com.bama.store.vo.DashboardVo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据概览
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<DashboardVo> overview() {
        return Result.success(dashboardService.overview());
    }
}
