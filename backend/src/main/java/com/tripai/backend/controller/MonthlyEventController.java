package com.tripai.backend.controller;

import com.tripai.backend.domain.dto.MonthlyEventResponse;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.MonthlyEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/months")
public class MonthlyEventController {

    private final MonthlyEventService monthlyEventService;

    public MonthlyEventController(MonthlyEventService monthlyEventService) {
        this.monthlyEventService = monthlyEventService;
    }

    @GetMapping("/{month}")
    public ResponseEntity<ApiResponse<MonthlyEventResponse>> getMonthlyEvents(
            @PathVariable String month,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") String page,
            @RequestParam(defaultValue = "10") String size
    ) {
        return ResponseEntity.ok(ApiResponse.success(monthlyEventService.getMonthlyEvents(month, category, page, size)));
    }
}
