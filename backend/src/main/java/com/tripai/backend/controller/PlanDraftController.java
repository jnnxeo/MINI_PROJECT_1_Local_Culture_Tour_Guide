package com.tripai.backend.controller;

import com.tripai.backend.domain.dto.DraftConditionsRequest;
import com.tripai.backend.domain.dto.DraftConditionsResponse;
import com.tripai.backend.domain.dto.DraftItemAddRequest;
import com.tripai.backend.domain.dto.DraftItemAddResponse;
import com.tripai.backend.domain.dto.DraftItemsRequest;
import com.tripai.backend.domain.dto.DraftItemsResponse;
import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.DraftTitleRequest;
import com.tripai.backend.domain.dto.DraftTitleResponse;
import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.PlanDraftService;
import com.tripai.backend.service.PlanRecommendService;
import com.tripai.backend.service.PlanRuleException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 나의 일정 — 저장 전 초안 API (요구사항 정의서 08 API-PLAN-002~008)
 * 조건 수정(003)·다시 추천(004)은 초안 생성(001)과 같은 추천 규칙이라 PlanRecommendService 에서 처리한다.
 */
@RestController
@RequestMapping("/api/plans/drafts")
public class PlanDraftController {

    private final PlanDraftService planDraftService;
    private final PlanRecommendService planRecommendService;

    public PlanDraftController(PlanDraftService planDraftService, PlanRecommendService planRecommendService) {
        this.planDraftService = planDraftService;
        this.planRecommendService = planRecommendService;
    }

    /** 저장한 일정을 원본을 유지한 채 편집 가능한 초안으로 복사한다. */
    @PostMapping("/from-saved/{planId}")
    public ResponseEntity<ApiResponse<DraftResponse>> copySavedPlan(
            @AuthenticationPrincipal Long userId, @PathVariable Long planId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(planDraftService.copySavedPlan(userId, planId)));
    }

    /** API-PLAN-002 저장 전 일정 초안 조회 */
    @GetMapping("/{draftId}")
    public ResponseEntity<ApiResponse<DraftResponse>> getDraft(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planDraftService.getDraft(userId, draftId)));
    }

    /** 저장 일정 편집 화면을 떠날 때 임시 초안을 폐기한다. */
    @DeleteMapping("/{draftId}")
    public ResponseEntity<Void> discardDraft(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId
    ) {
        planDraftService.discardDraft(userId, draftId);
        return ResponseEntity.noContent().build();
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

    /** API-PLAN-006 일정 항목·시간·방문 순서 일괄 수정 */
    @PutMapping("/{draftId}/items")
    public ResponseEntity<ApiResponse<DraftItemsResponse>> updateItems(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId,
            @Valid @RequestBody DraftItemsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(planDraftService.updateItems(userId, draftId, request.items())));
    }

    /** API-PLAN-007 맛집 일정에 추가 */
    @PostMapping("/{draftId}/items")
    public ResponseEntity<ApiResponse<DraftItemAddResponse>> addItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId,
            @Valid @RequestBody DraftItemAddRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(planDraftService.addItem(userId, draftId, request)));
    }

    /** API-PLAN-008 일정 항목 삭제 — 성공 시 204 No Content */
    @DeleteMapping("/{draftId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId,
            @PathVariable Long itemId
    ) {
        planDraftService.deleteItem(userId, draftId, itemId);
        return ResponseEntity.noContent().build();
    }

    /** API-PLAN-003 일정 추천 조건 수정 — 명세 오류 코드: 입력값 오류 400, 초안 없음 404 (추천 후보 부족은 422) */
    @PatchMapping("/{draftId}/conditions")
    public ResponseEntity<ApiResponse<DraftConditionsResponse>> updateConditions(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId,
            @Valid @RequestBody DraftConditionsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(planRecommendService.updateConditions(userId, draftId, request)));
    }

    /** API-PLAN-004 수정한 조건으로 일정 다시 추천 — 명세 오류 코드: 추천 후보 부족 422 */
    @PostMapping("/{draftId}/regenerate")
    public ResponseEntity<ApiResponse<DraftResponse>> regenerate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planRecommendService.regenerate(userId, draftId)));
    }

    /** 일정 편집 규칙 위반 — 명세의 API별 오류 코드(400·404·409·422)로 응답 */
    @ExceptionHandler(PlanRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlanRule(PlanRuleException exception) {
        return ResponseEntity.status(exception.getStatus()).body(ApiResponse.failure(exception.getMessage()));
    }

    /**
     * draftId·itemId 에 숫자가 아닌 값이 오면 400 (SEC-005).
     * 공통 GlobalExceptionHandler 에는 아직 이 처리가 없어 500 이 나가서, 이 컨트롤러에서만 먼저 막는다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT.getMessage()));
    }
}
