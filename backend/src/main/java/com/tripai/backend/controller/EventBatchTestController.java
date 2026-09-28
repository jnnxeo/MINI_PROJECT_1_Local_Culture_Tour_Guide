package com.tripai.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.EventBatchService;

import lombok.RequiredArgsConstructor;

/**
 * 배치를 수동으로 즉시 실행해 보는 테스트용 엔드포인트.
 * 로그인(JWT)이 필요하다: Authorization: Bearer {토큰}
 * TODO: 운영 전에 삭제하거나 관리자 권한으로 제한한다.
 */
@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class EventBatchTestController {

    private final EventBatchService eventBatchService;

    @PostMapping("/collect")
    public ApiResponse<Void> collectNow() {
        eventBatchService.collectAndSave();
        return ApiResponse.emptySuccess();
    }
}