package com.tripai.backend.controller;

import java.util.List;
import java.time.LocalTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripai.backend.domain.dto.plan.CreateEventDraftRequest;
import com.tripai.backend.domain.dto.plan.EventPlanDetailResponse;
import com.tripai.backend.domain.dto.plan.EventPlanItemResponse;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.EventPlanService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/** 행사 상세에서 시작하는 신규 초안과 저장 일정의 행사 추가. */
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

    public record AddSavedEventRequest(@NotBlank String eventId, LocalTime startTime) {}

    public record UpdateSavedEventTimeRequest(@jakarta.validation.constraints.NotNull LocalTime startTime) {}

    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<EventPlanDetailResponse>> getSavedPlan(
            @AuthenticationPrincipal Long userId, @PathVariable long planId) {
        return ResponseEntity.ok(ApiResponse.success(service.getSavedPlan(userId, planId)));
    }

    @PostMapping("/{planId}/events")
    public ResponseEntity<ApiResponse<EventPlanDetailResponse>> addEventToSavedPlan(
            @AuthenticationPrincipal Long userId, @PathVariable long planId,
            @Valid @RequestBody AddSavedEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                service.addEventToSavedPlan(userId, planId, request.eventId(), request.startTime())));
    }

    @PatchMapping("/{planId}/events/{itemId}")
    public ResponseEntity<ApiResponse<EventPlanDetailResponse>> updateSavedEventTime(
            @AuthenticationPrincipal Long userId, @PathVariable long planId,
            @PathVariable long itemId, @Valid @RequestBody UpdateSavedEventTimeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateSavedEventTime(userId, planId, itemId, request.startTime())));
    }

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
