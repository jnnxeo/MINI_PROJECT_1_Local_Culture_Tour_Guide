package com.tripai.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripai.backend.domain.dto.plan.CreateEventDraftRequest;
import com.tripai.backend.domain.dto.plan.EventPlanDetailResponse;
import com.tripai.backend.domain.dto.plan.EventPlanItemResponse;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.EventPlanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** EVENT-004: 선택 행사로 당일 일정 초안을 만든다. 저장 일정 API는 Plan/내 여행 담당이 제공한다. */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class EventPlanController {
    private final EventPlanService service;

    public record RecommendedDraft(
            Long draftId,
            String title,
            String tripType,
            SelectedEvent selectedEvent,
            List<EventPlanItemResponse> items,
            List<RecommendationReason> recommendationReasons
    ) {}

    public record SelectedEvent(String eventId, String title) {}

    public record RecommendationReason(Long itemId, String reason) {}

    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<RecommendedDraft>> createDraft(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateEventDraftRequest request
    ) {
        EventPlanDetailResponse draft = service.createDraft(userId, request);
        EventPlanItemResponse anchor = draft.items().stream()
                .filter(item -> "EVENT".equals(item.type())
                        && draft.anchorEventId().equals(item.contentId()))
                .findFirst().orElseThrow();
        List<RecommendationReason> reasons = draft.items().stream()
                .filter(item -> item.aiReason() != null)
                .map(item -> new RecommendationReason(item.itemId(), item.aiReason()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new RecommendedDraft(
                        draft.planId(), draft.title(), "DAY_TRIP",
                        new SelectedEvent(anchor.contentId(), anchor.name()),
                        draft.items(), reasons)));
    }
}
