package com.client.lms.dashboard.controller;

import com.client.lms.dashboard.dto.response.DashboardStatsResponse;
import com.client.lms.dashboard.service.DashboardStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "Admin home page counts")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardStatsController {

    private final DashboardStatsService stats;

    @Operation(summary = "Counts of users, classrooms, files, processing jobs and logins")
    @GetMapping("/stats")
    public DashboardStatsResponse stats() {
        return stats.stats();
    }
}
