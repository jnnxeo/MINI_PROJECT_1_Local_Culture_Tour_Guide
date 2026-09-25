package com.tripai.backend.service;

import com.tripai.backend.domain.dto.DraftItemRequest;
import com.tripai.backend.domain.dto.DraftItemsResponse;
import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.DraftTitleResponse;
import com.tripai.backend.domain.dto.MapPointResponse;
import com.tripai.backend.domain.dto.PlanItemResponse;
import com.tripai.backend.domain.dto.RecommendationReasonResponse;
import com.tripai.backend.domain.entity.ItemTargetView;
import com.tripai.backend.domain.entity.PlanItemView;
import com.tripai.backend.domain.entity.TripItem;
import com.tripai.backend.domain.entity.TripPlan;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.repository.PlanDraftMapper;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
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

    /**
     * API-PLAN-006 일정 항목·시간·방문 순서 일괄 수정 (TRIP-004·005).
     * 초안의 항목을 모두 보내야 하고, 항목 추가·삭제는 API-PLAN-007·008 로 한다.
     * 명세 오류 코드: 없는 항목·장소 404, 시간 중복·순서 오류 등 규칙 위반 400.
     * itemId 가 바뀌지 않도록 행을 지우지 않고 순번을 잠깐 미룬 뒤 다시 쓴다 (seq_order UNIQUE).
     */
    @Transactional
    public DraftItemsResponse updateItems(Long userId, Long draftId, List<DraftItemRequest> requests) {
        TripPlan plan = findOwnedDraft(userId, draftId);
        Map<Long, PlanItemView> current = planDraftMapper.findItemsByPlanId(draftId).stream()
                .collect(Collectors.toMap(PlanItemView::getTripItemId, Function.identity()));

        Set<Long> requestedIds = new HashSet<>();
        for (DraftItemRequest request : requests) {
            if (!current.containsKey(request.itemId())) {
                throw new PlanRuleException(HttpStatus.NOT_FOUND, "일정 항목을 찾을 수 없습니다.");
            }
            if (!requestedIds.add(request.itemId())) {
                throw new PlanRuleException(HttpStatus.BAD_REQUEST, "같은 항목이 두 번 들어 있습니다.");
            }
        }
        if (requestedIds.size() != current.size()) {
            throw new PlanRuleException(HttpStatus.BAD_REQUEST, "일정의 모든 항목을 보내 주세요. 추가·삭제는 따로 요청합니다.");
        }

        List<TripItem> updates = new ArrayList<>();
        List<PlanItemRules.Candidate> candidates = new ArrayList<>();
        List<Long> changedTargetIds = new ArrayList<>();

        for (DraftItemRequest request : requests) {
            PlanItemView before = current.get(request.itemId());
            LocalTime startTime = LocalTime.parse(request.startTime());
            boolean sameTarget = request.type().equals(before.getItemType())
                    && request.placeId().equals(contentIdOf(before));

            String name;
            boolean timeFixed;
            String aiReason;

            if (sameTarget) {
                name = before.getName();
                timeFixed = Boolean.TRUE.equals(before.getTimeFixYn());
                aiReason = before.getAiReason();
                if (timeFixed && !startTime.equals(before.getStartTime())) {
                    throw new PlanRuleException(HttpStatus.BAD_REQUEST,
                            name + "은(는) 시작 시간이 정해진 행사라 시간을 바꿀 수 없습니다.");
                }
            } else {
                // 장소가 바뀐 항목: 새 대상이 있는지 확인하고, 이전 장소의 추천 이유는 지운다 (AI-007)
                ItemTargetView target = findTarget(request.type(), request.placeId(), plan, HttpStatus.BAD_REQUEST);
                name = target.getName();
                timeFixed = target.getEventStartTime() != null;
                aiReason = null;
                if (timeFixed && !startTime.equals(target.getEventStartTime())) {
                    throw new PlanRuleException(HttpStatus.BAD_REQUEST,
                            name + "은(는) " + format(target.getEventStartTime()) + "에 시작하는 행사입니다.");
                }
                changedTargetIds.add(request.itemId());
            }

            candidates.add(new PlanItemRules.Candidate(
                    request.type(), request.placeId(), name, startTime, request.durationMin(), request.sequence()));
            updates.add(TripItem.builder()
                    .tripItemId(request.itemId())
                    .tripPlanId(draftId)
                    .seqOrder(request.sequence())
                    .itemType(request.type())
                    .eventContentId("EVENT".equals(request.type()) ? request.placeId() : null)
                    .placeContentId("PLACE".equals(request.type()) ? request.placeId() : null)
                    .startTime(startTime)
                    .durationMin(request.durationMin())
                    .aiReason(aiReason)
                    .timeFixYn(timeFixed)
                    .build());
        }

        PlanItemRules.check(candidates, plan.getAnchorContentId(), plan.getVisitStartTime(), plan.getVisitEndTime(), true)
                .ifPresent(result -> {
                    throw new PlanRuleException(HttpStatus.BAD_REQUEST, result.message());
                });

        planDraftMapper.shiftSeqOrders(draftId);
        if (!changedTargetIds.isEmpty()) {
            planDraftMapper.clearContentIds(draftId, changedTargetIds);
        }
        updates.forEach(planDraftMapper::updateItem);

        List<PlanItemResponse> items = planDraftMapper.findItemsByPlanId(draftId).stream()
                .map(this::toItemResponse)
                .toList();
        return new DraftItemsResponse(draftId, items, toMapPoints(items));
    }

    /**
     * 일정에 넣을 행사·맛집이 있는지 확인한다. 없거나 비표출이면 404.
     * 행사는 여행 날짜에 진행 중이어야 한다 (EVENT-004 날짜 충돌) — 이때 응답 코드는 API마다 다르다.
     */
    private ItemTargetView findTarget(String type, String contentId, TripPlan plan, HttpStatus dateConflictStatus) {
        if ("PLACE".equals(type)) {
            return planDraftMapper.findPlaceTarget(contentId)
                    .filter(target -> Boolean.TRUE.equals(target.getDisplayYn()))
                    .orElseThrow(() -> new PlanRuleException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."));
        }

        ItemTargetView event = planDraftMapper.findEventTarget(contentId)
                .filter(target -> Boolean.TRUE.equals(target.getDisplayYn()))
                .orElseThrow(() -> new PlanRuleException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));

        if (plan.getTripDate().isBefore(event.getEventStartDate()) || plan.getTripDate().isAfter(event.getEventEndDate())) {
            throw new PlanRuleException(dateConflictStatus,
                    event.getName() + "은(는) 여행 날짜(" + plan.getTripDate() + ")에 진행하지 않는 행사입니다.");
        }
        return event;
    }

    private static String contentIdOf(PlanItemView item) {
        return "EVENT".equals(item.getItemType()) ? item.getEventContentId() : item.getPlaceContentId();
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

        List<MapPointResponse> mapPoints = toMapPoints(itemResponses);

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

    private static List<MapPointResponse> toMapPoints(List<PlanItemResponse> items) {
        return items.stream()
                .filter(item -> item.lat() != null && item.lng() != null)
                .map(item -> new MapPointResponse(item.sequence(), item.lat(), item.lng()))
                .toList();
    }

    private PlanItemResponse toItemResponse(PlanItemView item) {
        String placeId = contentIdOf(item);

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
