package com.tripai.backend.service;

import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.DraftTitleResponse;
import com.tripai.backend.domain.dto.MapPointResponse;
import com.tripai.backend.domain.dto.PlanItemResponse;
import com.tripai.backend.domain.dto.RecommendationReasonResponse;
import com.tripai.backend.domain.entity.PlanItemView;
import com.tripai.backend.domain.entity.TripPlan;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.repository.PlanDraftMapper;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanDraftService {

    // DDL v3.2.1 은 당일 여행만 다룬다 (숙박 제외)
    static final String DAY_TRIP = "DAY_TRIP";

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final PlanDraftMapper planDraftMapper;

    public PlanDraftService(PlanDraftMapper planDraftMapper) {
        this.planDraftMapper = planDraftMapper;
    }

    /**
     * API-PLAN-002 저장 전 일정 초안 조회.
     * 다른 사람 일정이면 403(SEC-004), 없거나 이미 저장된 일정이면 404(초안 없음).
     */
    @Transactional(readOnly = true)
    public DraftResponse getDraft(Long userId, Long draftId) {
        TripPlan plan = findOwnedDraft(userId, draftId);
        List<PlanItemView> items = planDraftMapper.findItemsByPlanId(draftId);
        return toDraftResponse(plan, items);
    }

    /** API-PLAN-005 저장 전 일정 제목 변경 (TRIP-003) — 앞뒤 공백은 빼고 저장 */
    @Transactional
    public DraftTitleResponse updateTitle(Long userId, Long draftId, String title) {
        findOwnedDraft(userId, draftId);
        String trimmed = title.trim();
        planDraftMapper.updateTitle(draftId, trimmed);
        return new DraftTitleResponse(draftId, trimmed);
    }

    private TripPlan findOwnedDraft(Long userId, Long draftId) {
        TripPlan plan = planDraftMapper.findPlanById(draftId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));

        if (!Objects.equals(plan.getUserId(), userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (Boolean.TRUE.equals(plan.getSaveYn())) {
            throw new CustomException(ErrorCode.PLAN_NOT_FOUND);
        }

        return plan;
    }

    private DraftResponse toDraftResponse(TripPlan plan, List<PlanItemView> items) {
        List<PlanItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();

        List<RecommendationReasonResponse> reasons = items.stream()
                .filter(item -> item.getAiReason() != null)
                .map(item -> new RecommendationReasonResponse(item.getTripItemId(), item.getAiReason()))
                .toList();

        List<MapPointResponse> mapPoints = itemResponses.stream()
                .filter(item -> item.lat() != null && item.lng() != null)
                .map(item -> new MapPointResponse(item.sequence(), item.lat(), item.lng()))
                .toList();

        DraftResponse.Conditions conditions = new DraftResponse.Conditions(
                plan.getTripDate(),
                format(plan.getVisitStartTime()),
                format(plan.getVisitEndTime()),
                null,
                null,
                plan.getTransportMd()
        );

        return new DraftResponse(
                plan.getTripPlanId(),
                plan.getTitle(),
                plan.getTripDate(),
                DAY_TRIP,
                new DraftResponse.SelectedEvent(plan.getAnchorContentId(), plan.getAnchorEventName()),
                conditions,
                itemResponses,
                reasons,
                mapPoints
        );
    }

    private PlanItemResponse toItemResponse(PlanItemView item) {
        String placeId = "EVENT".equals(item.getItemType())
                ? item.getEventContentId()
                : item.getPlaceContentId();

        String breakTime = item.getBreakOpenTime() != null && item.getBreakCloseTime() != null
                ? format(item.getBreakOpenTime()) + "~" + format(item.getBreakCloseTime())
                : null;

        return new PlanItemResponse(
                item.getTripItemId(),
                item.getItemType(),
                placeId,
                item.getSeqOrder(),
                format(item.getStartTime()),
                item.getDurationMin(),
                item.getName(),
                item.getAddr(),
                toDouble(item.getMapy()),
                toDouble(item.getMapx()),
                item.getImageUrl(),
                format(item.getOpenTime()),
                breakTime,
                format(item.getCloseTime()),
                Boolean.TRUE.equals(item.getTimeFixYn())
        );
    }

    private static String format(LocalTime time) {
        return time == null ? null : time.format(HH_MM);
    }

    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
