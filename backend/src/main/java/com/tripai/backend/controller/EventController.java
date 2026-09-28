package com.tripai.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripai.backend.domain.dto.event.EventDetailResponse;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public ApiResponse<EventDetailResponse> getEventDetail(
            @PathVariable String eventId
    ) {
        return ApiResponse.success(eventService.getEventDetail(eventId));
    }
}