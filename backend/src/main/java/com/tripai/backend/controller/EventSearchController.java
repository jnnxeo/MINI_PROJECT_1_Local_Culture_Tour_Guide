package com.tripai.backend.controller;

import com.tripai.backend.domain.dto.EventSearchResponse;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.EventSearchService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventSearchController {

    private final EventSearchService eventSearchService;

    public EventSearchController(EventSearchService eventSearchService) {
        this.eventSearchService = eventSearchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<EventSearchResponse>> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String freeYn,
            @RequestParam(defaultValue = "startDateAsc") String sort,
            @RequestParam(defaultValue = "0") String page,
            @RequestParam(defaultValue = "10") String size
    ) {
        return ResponseEntity.ok(ApiResponse.success(eventSearchService.searchEvents(
                keyword, month, category, district, freeYn, sort, page, size)));
    }
}
