package com.tripai.backend.controller;

import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.PlanRecommendRequest;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.PlanRecommendService;
import com.tripai.backend.service.PlanRuleException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 나의 일정 — 문화행사 1개 기준 일정 초안 생성 (요구사항 정의서 08 API-PLAN-001)
 * 메인·행사 상세(2팀 화면)에서 호출하고, 받은 draftId 로 /trips/draft 에 들어온다.
 */
@RestController
@RequestMapping("/api/plans")
public class PlanRecommendController {

    private final PlanRecommendService planRecommendService;

    public PlanRecommendController(PlanRecommendService planRecommendService) {
        this.planRecommendService = planRecommendService;
    }

    /**
     * API-PLAN-001 — 성공 시 201. 응답은 초안 조회(API-PLAN-002)와 같은 모양이라
     * 명세 필드(draftId, title, tripType, selectedEvent, items, recommendationReasons)를 모두 포함한다.
     * 명세 오류 코드: 행사일 불일치 400, 추천 후보 부족 422. 없는 행사는 404.
     */
    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<DraftResponse>> recommend(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlanRecommendRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(planRecommendService.recommend(userId, request)));
    }

    @ExceptionHandler(PlanRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlanRule(PlanRuleException exception) {
        return ResponseEntity.status(exception.getStatus()).body(ApiResponse.failure(exception.getMessage()));
    }
}
