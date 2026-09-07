package com.bama.store.controller;
import com.bama.store.common.Result;
import com.bama.store.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;
@RestController
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reports;
    @GetMapping("/api/reports")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<Map<String,Object>> report(@RequestParam(required=false) LocalDate startDate, @RequestParam(required=false) LocalDate endDate) {
        return Result.success(reports.report(startDate, endDate));
    }
}
