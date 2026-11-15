package com.chubb.claims.api;

import com.chubb.claims.api.dto.ExposureResponse;
import com.chubb.claims.api.dto.WorkloadResponse;
import com.chubb.claims.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/workload")
    public WorkloadResponse getWorkload() {
        return WorkloadResponse.from(dashboard.workload());
    }

    @GetMapping("/exposure")
    public ExposureResponse getExposure() {
        return ExposureResponse.from(dashboard.exposure());
    }
}
