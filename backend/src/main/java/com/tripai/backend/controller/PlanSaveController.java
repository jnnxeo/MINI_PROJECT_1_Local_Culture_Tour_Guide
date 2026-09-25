package com.tripai.backend.controller;

import com.tripai.backend.domain.dto.PlanSaveRequest;
import com.tripai.backend.domain.dto.PlanSaveResponse;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.PlanDraftService;
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
 * 나의 일정 — 초안을 저장 일정으로 확정 (요구사항 정의서 08 API-PLAN-009)
 * 저장 일정 목록·상세·수정·삭제(API-PLAN-010~013)는 내 여행 담당이라 여기 두지 않는다.
 */
@RestController
@RequestMapping("/api/plans")
public class PlanSaveController {

    private final PlanDraftService planDraftService;

    public PlanSaveController(PlanDraftService planDraftService) {
        this.planDraftService = planDraftService;
    }

    /** API-PLAN-009 일정 초안을 저장 일정으로 확정 — 성공 시 201 */
    @PostMapping
    public ResponseEntity<ApiResponse<PlanSaveResponse>> save(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlanSaveRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(planDraftService.saveDraft(userId, request.draftId())));
    }

    @ExceptionHandler(PlanRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlanRule(PlanRuleException exception) {
        return ResponseEntity.status(exception.getStatus()).body(ApiResponse.failure(exception.getMessage()));
    }
}
