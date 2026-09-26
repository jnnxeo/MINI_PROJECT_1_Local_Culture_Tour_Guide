package com.tripai.backend.controller;

import lombok.RequiredArgsConstructor;

import com.tripai.backend.domain.dto.EventListResponse;
import com.tripai.backend.global.response.ApiResponse;

import com.tripai.backend.service.FavoriteEventService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;

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

        if(page < 0){
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (size != 5 && size != 10 && size != 20) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        System.out.println("favorite event controller getevnets list");

        EventListResponse response = favoriteEventService.getFavoriteEvents(userId, page, size);
        return ResponseEntity.status(HttpStatus.OK)
                            .body(ApiResponse.success(response));
    }

    // 5. 관심 행사 삭제
    @DeleteMapping("/{eventContentId}")
    public ResponseEntity<ApiResponse<Void>> deleteFavoriteEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable String eventContentId
    ) {
        favoriteEventService.deleteFavoriteEvent(userId, eventContentId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                            .body(ApiResponse.success(null));
    }
}
