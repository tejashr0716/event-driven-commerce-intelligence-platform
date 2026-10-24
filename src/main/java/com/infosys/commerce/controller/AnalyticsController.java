package com.infosys.commerce.controller;

import com.infosys.commerce.domain.dto.AnalyticsSummaryDto;
import com.infosys.commerce.service.AnalyticsPipelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsPipelineService analyticsPipelineService;

    public AnalyticsController(AnalyticsPipelineService analyticsPipelineService) {
        this.analyticsPipelineService = analyticsPipelineService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDto> getSummary() {
        return ResponseEntity.ok(analyticsPipelineService.computeOperationalMetrics());
    }
}
