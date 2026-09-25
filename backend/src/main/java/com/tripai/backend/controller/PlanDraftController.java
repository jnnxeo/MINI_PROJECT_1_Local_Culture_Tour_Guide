package com.tripai.backend.controller;

import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.DraftTitleRequest;
import com.tripai.backend.domain.dto.DraftTitleResponse;
import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.PlanDraftService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 나의 일정 — 저장 전 초안 API (요구사항 정의서 08 API-PLAN-002~008)
 */
@RestController
@RequestMapping("/api/plans/drafts")
public class PlanDraftController {

    private final PlanDraftService planDraftService;

    public PlanDraftController(PlanDraftService planDraftService) {
        this.planDraftService = planDraftService;
    }

    /** API-PLAN-002 저장 전 일정 초안 조회 */
    @GetMapping("/{draftId}")
    public ResponseEntity<ApiResponse<DraftResponse>> getDraft(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planDraftService.getDraft(userId, draftId)));
    }

    /** API-PLAN-005 저장 전 일정 제목 변경 */
    @PatchMapping("/{draftId}/title")
    public ResponseEntity<ApiResponse<DraftTitleResponse>> updateTitle(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId,
            @Valid @RequestBody DraftTitleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(planDraftService.updateTitle(userId, draftId, request.title())));
    }

    /**
     * draftId 에 숫자가 아닌 값이 오면 400 (SEC-005).
     * 공통 GlobalExceptionHandler 에는 아직 이 처리가 없어 500 이 나가서, 이 컨트롤러에서만 먼저 막는다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT.getMessage()));
    }
}
