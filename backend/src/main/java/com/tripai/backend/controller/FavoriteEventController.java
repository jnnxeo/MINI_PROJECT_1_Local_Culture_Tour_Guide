package com.tripai.backend.controller;

import lombok.RequiredArgsConstructor;

import com.tripai.backend.domain.dto.EventListResponse;
import com.tripai.backend.global.response.ApiResponse;

import com.tripai.backend.service.FavoriteEventService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites/events")
@RequiredArgsConstructor
public class FavoriteEventController {

    private final FavoriteEventService favoriteEventService;

    // 2. 관심 행사 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<EventListResponse>> getFavoriteEvents(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        EventListResponse response = favoriteEventService.getFavoriteEvents(userId, page, size);

        return ResponseEntity.status(HttpStatus.OK)
                            .body(ApiResponse.success(response));
    }

    // 5. 관심 행사 삭제
    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Void>> deleteFavoriteEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable String eventId
    ) {
        Integer response = favoriteEventService.deleteFavoriteEvent(userId, eventId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                            .body(ApiResponse.success(null));
    }
}
