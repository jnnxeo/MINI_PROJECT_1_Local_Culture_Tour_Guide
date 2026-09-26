package com.tripai.backend.controller;

import com.tripai.backend.domain.dto.RestaurantSearchResponse;
import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.PlaceSearchException;
import com.tripai.backend.service.PlaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 맛집 후보 조회 (요구사항 정의서 08 API-PLACE-001)
 * 숙박 후보(API-PLACE-002)는 DDL v3.2.1 범위(당일 여행·숙박 제외) 밖이라 두지 않는다.
 */
@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    /**
     * API-PLACE-001 행사 주변 맛집 후보 조회.
     * category 는 DDL 에 음식 분류 컬럼이 없어 아직 받지 않는다.
     */
    @GetMapping("/restaurants")
    public ResponseEntity<ApiResponse<RestaurantSearchResponse>> searchRestaurants(
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                placeService.searchRestaurants(eventId, lat, lng, radius, page, size)));
    }

    @ExceptionHandler(PlaceSearchException.class)
    public ResponseEntity<ApiResponse<Void>> handleSearch(PlaceSearchException exception) {
        return ResponseEntity.status(exception.getStatus()).body(ApiResponse.failure(exception.getMessage()));
    }

    /** lat=abc 처럼 숫자 자리에 문자가 오면 400 (공통 핸들러에서는 500이 나가서 여기서 먼저 처리) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT.getMessage()));
    }
}
