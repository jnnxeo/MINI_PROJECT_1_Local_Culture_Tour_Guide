package com.tripai.backend.service;

import com.tripai.backend.domain.entity.ItemTargetView;
import com.tripai.backend.domain.entity.PlanItemView;
import com.tripai.backend.domain.entity.RecommendEventView;
import com.tripai.backend.domain.entity.TripItem;
import com.tripai.backend.domain.entity.TripPlan;
import com.tripai.backend.repository.PlanDraftMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 테스트용 메모리 Mapper. trip_item 의 DDL 제약도 같이 검사한다.
 * - UK_TRIP_ITEM_SEQ (trip_plan_id, seq_order)
 * - UK_TRIP_ITEM_EVENT / UK_TRIP_ITEM_PLACE
 * - CK_TRIP_ITEM_SEQ (seq_order > 0)
 */
class FakePlanDraftMapper implements PlanDraftMapper {

    record Display(String name, String addr, String mapx, String mapy,
                   LocalTime openTime, LocalTime closeTime, LocalTime breakOpenTime, LocalTime breakCloseTime) {
    }

    final Map<Long, TripPlan> plans = new HashMap<>();
    final Map<Long, TripItem> rows = new LinkedHashMap<>();
    final Map<String, ItemTargetView> places = new HashMap<>();
    final Map<String, ItemTargetView> events = new HashMap<>();
    final Map<String, Display> displays = new HashMap<>();
    final Map<String, String> cuisines = new HashMap<>();
    final Map<Long, String> titles = new HashMap<>();
    final Map<String, RecommendEventView> recommendEvents = new HashMap<>();

    void addRow(TripItem item) {
        rows.put(item.getTripItemId(), item);
        checkConstraints(item.getTripPlanId());
    }

    @Override
    public Optional<TripPlan> findPlanById(Long tripPlanId) {
        return Optional.ofNullable(plans.get(tripPlanId));
    }

    @Override
    public Optional<TripPlan> findPlanByIdForUpdate(Long tripPlanId) {
        return findPlanById(tripPlanId);
    }

    @Override
    public List<PlanItemView> findItemsByPlanId(Long tripPlanId) {
        return rows.values().stream()
                .filter(row -> row.getTripPlanId().equals(tripPlanId))
                .sorted(Comparator.comparing(TripItem::getSeqOrder))
                .map(this::toView)
                .toList();
    }

    @Override
    public int updateTitle(Long tripPlanId, String title) {
        titles.put(tripPlanId, title);
        // 실제 DB 처럼 초안 행의 제목도 바꾼다 (조건 수정이 지금 제목을 읽어 판단한다)
        TripPlan plan = plans.get(tripPlanId);
        if (plan != null) {
            try {
                var field = TripPlan.class.getDeclaredField("title");
                field.setAccessible(true);
                field.set(plan, title);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return 1;
    }

    @Override
    public Optional<ItemTargetView> findPlaceTarget(String contentId) {
        return Optional.ofNullable(places.get(contentId));
    }

    @Override
    public Optional<ItemTargetView> findEventTarget(String eventContentId) {
        return Optional.ofNullable(events.get(eventContentId));
    }

    @Override
    public int shiftSeqOrders(Long tripPlanId) {
        rows.replaceAll((id, row) -> row.getTripPlanId().equals(tripPlanId)
                ? copy(row).seqOrder(row.getSeqOrder() + 1000).build()
                : row);
        checkConstraints(tripPlanId);
        return 1;
    }

    @Override
    public int clearContentIds(Long tripPlanId, List<Long> itemIds) {
        itemIds.forEach(id -> rows.computeIfPresent(id, (key, row) ->
                copy(row).eventContentId(null).placeContentId(null).build()));
        return itemIds.size();
    }

    @Override
    public int updateItem(TripItem item) {
        TripItem before = rows.get(item.getTripItemId());
        if (before == null || !before.getTripPlanId().equals(item.getTripPlanId())) {
            return 0;
        }
        rows.put(item.getTripItemId(), item);
        checkConstraints(item.getTripPlanId());
        return 1;
    }

    private long nextItemId = 500L;

    @Override
    public int insertItem(TripItem item) {
        TripItem saved = copy(item).tripItemId(nextItemId++).build();
        rows.put(saved.getTripItemId(), saved);
        checkConstraints(saved.getTripPlanId());
        // MyBatis useGeneratedKeys 처럼 넘겨받은 객체에 번호를 채운다
        try {
            var field = TripItem.class.getDeclaredField("tripItemId");
            field.setAccessible(true);
            field.set(item, saved.getTripItemId());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return 1;
    }

    @Override
    public int updateSeqOrder(Long tripPlanId, Long tripItemId, Integer seqOrder) {
        TripItem row = rows.get(tripItemId);
        if (row == null || !row.getTripPlanId().equals(tripPlanId)) {
            return 0;
        }
        rows.put(tripItemId, copy(row).seqOrder(seqOrder).build());
        checkConstraints(tripPlanId);
        return 1;
    }

    @Override
    public int deleteItem(Long tripPlanId, Long tripItemId) {
        TripItem row = rows.get(tripItemId);
        if (row == null || !row.getTripPlanId().equals(tripPlanId)) {
            return 0;
        }
        rows.remove(tripItemId);
        return 1;
    }

    @Override
    public int markSaved(Long tripPlanId) {
        TripPlan plan = plans.get(tripPlanId);
        if (plan == null || Boolean.TRUE.equals(plan.getSaveYn())) {
            return 0;
        }
        plans.put(tripPlanId, TripPlan.builder()
                .tripPlanId(plan.getTripPlanId())
                .userId(plan.getUserId())
                .anchorContentId(plan.getAnchorContentId())
                .anchorEventName(plan.getAnchorEventName())
                .anchorStartDate(plan.getAnchorStartDate())
                .anchorEndDate(plan.getAnchorEndDate())
                .title(plan.getTitle())
                .tripDate(plan.getTripDate())
                .visitStartTime(plan.getVisitStartTime())
                .visitEndTime(plan.getVisitEndTime())
                .saveYn(true)
                .aiYn(plan.getAiYn())
                .transportMd(plan.getTransportMd())
                .foodPreference(plan.getFoodPreference())
                .mealType(plan.getMealType())
                .lunchFoodPreference(plan.getLunchFoodPreference())
                .dinnerFoodPreference(plan.getDinnerFoodPreference())
                .cafeYn(Boolean.TRUE.equals(plan.getCafeYn()))
                .searchDates(plan.getSearchDates())
                .searchCategories(plan.getSearchCategories())
                .searchDistrict(plan.getSearchDistrict())
                .searchFreeYn(plan.getSearchFreeYn())
                .headcount(plan.getHeadcount())
                .build());
        return 1;
    }

    @Override
    public Optional<RecommendEventView> findRecommendEvent(String eventContentId) {
        return Optional.ofNullable(recommendEvents.get(eventContentId));
    }

    /** SQL 과 같은 조건: 표시·좌표 있음·그날 진행 중·분야·자치구·무료, 종료일 가까운 순 */
    @Override
    public List<RecommendEventView> findRecommendEventCandidates(LocalDate visitDate, List<String> eventTypes,
                                                                 String district, boolean freeOnly, int limit) {
        return recommendEvents.values().stream()
                .filter(event -> Boolean.TRUE.equals(event.getDisplayYn()))
                .filter(event -> event.getMapx() != null && event.getMapy() != null)
                .filter(event -> !visitDate.isBefore(event.getEventStartDate()) && !visitDate.isAfter(event.getEventEndDate()))
                .filter(event -> eventTypes == null || eventTypes.isEmpty() || eventTypes.contains(event.getEventType()))
                .filter(event -> district == null || district.equals(event.getDistrictName()))
                .filter(event -> !freeOnly || Boolean.TRUE.equals(event.getFreeYn()))
                .sorted(Comparator.comparing(RecommendEventView::getEventEndDate)
                        .thenComparing(RecommendEventView::getEventContentId))
                .limit(limit)
                .toList();
    }

    private long nextPlanId = 900L;

    @Override
    public int insertPlan(TripPlan plan) {
        long planId = nextPlanId++;
        // MyBatis useGeneratedKeys 처럼 넘겨받은 객체에 번호를 채운다
        try {
            var field = TripPlan.class.getDeclaredField("tripPlanId");
            field.setAccessible(true);
            field.set(plan, planId);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        RecommendEventView event = recommendEvents.get(plan.getAnchorContentId());
        plans.put(planId, TripPlan.builder()
                .tripPlanId(planId)
                .userId(plan.getUserId())
                .anchorContentId(plan.getAnchorContentId())
                .anchorEventName(event == null ? null : event.getEventName())
                .anchorStartDate(event == null ? null : event.getEventStartDate())
                .anchorEndDate(event == null ? null : event.getEventEndDate())
                .title(plan.getTitle())
                .tripDate(plan.getTripDate())
                .visitStartTime(plan.getVisitStartTime())
                .visitEndTime(plan.getVisitEndTime())
                .saveYn(false)
                .aiYn(plan.getAiYn())
                .transportMd(plan.getTransportMd())
                .foodPreference(plan.getFoodPreference())
                .mealType(plan.getMealType())
                .lunchFoodPreference(plan.getLunchFoodPreference())
                .dinnerFoodPreference(plan.getDinnerFoodPreference())
                .cafeYn(Boolean.TRUE.equals(plan.getCafeYn()))
                .searchDates(plan.getSearchDates())
                .searchCategories(plan.getSearchCategories())
                .searchDistrict(plan.getSearchDistrict())
                .searchFreeYn(plan.getSearchFreeYn())
                .headcount(plan.getHeadcount())
                .build());
        return 1;
    }

    @Override
    public int updateConditions(TripPlan plan) {
        TripPlan before = plans.get(plan.getTripPlanId());
        if (before == null || Boolean.TRUE.equals(before.getSaveYn())) {
            return 0;
        }
        // 실제 SQL 처럼 기준 행사·제목도 바꾼다. 행사가 그대로면 제목 수정(updateTitle)을 지킨다
        boolean eventChanged = !Objects.equals(plan.getAnchorContentId(), before.getAnchorContentId());
        RecommendEventView anchor = recommendEvents.get(plan.getAnchorContentId());
        // 서비스가 제목을 바꿔 넘기면(행사 변경·기본 제목 날짜 맞춤) 그 제목, 아니면 제목 수정(updateTitle)을 지킨다
        boolean titleChanged = eventChanged || !Objects.equals(plan.getTitle(), before.getTitle());
        String title = titleChanged ? plan.getTitle() : titles.getOrDefault(before.getTripPlanId(), before.getTitle());
        titles.put(before.getTripPlanId(), title);
        plans.put(plan.getTripPlanId(), TripPlan.builder()
                .tripPlanId(before.getTripPlanId())
                .userId(before.getUserId())
                .anchorContentId(plan.getAnchorContentId())
                .anchorEventName(anchor == null ? before.getAnchorEventName() : anchor.getEventName())
                .anchorStartDate(anchor == null ? before.getAnchorStartDate() : anchor.getEventStartDate())
                .anchorEndDate(anchor == null ? before.getAnchorEndDate() : anchor.getEventEndDate())
                .title(title)
                .tripDate(plan.getTripDate())
                .visitStartTime(plan.getVisitStartTime())
                .visitEndTime(plan.getVisitEndTime())
                .saveYn(before.getSaveYn())
                .aiYn(plan.getAiYn())
                .transportMd(plan.getTransportMd())
                .foodPreference(plan.getFoodPreference())
                .mealType(plan.getMealType())
                .lunchFoodPreference(plan.getLunchFoodPreference())
                .dinnerFoodPreference(plan.getDinnerFoodPreference())
                .cafeYn(Boolean.TRUE.equals(plan.getCafeYn()))
                .searchDates(plan.getSearchDates())
                .searchCategories(plan.getSearchCategories())
                .searchDistrict(plan.getSearchDistrict())
                .searchFreeYn(plan.getSearchFreeYn())
                .headcount(before.getHeadcount())
                .build());
        return 1;
    }

    @Override
    public int deleteItemsByPlanId(Long tripPlanId) {
        int before = rows.size();
        rows.values().removeIf(row -> row.getTripPlanId().equals(tripPlanId));
        return before - rows.size();
    }

    @Override
    public int replaceSavedFromDraft(Long planId, Long draftId) {
        TripPlan saved = plans.get(planId);
        TripPlan draft = plans.get(draftId);
        if (saved == null || draft == null || !Boolean.TRUE.equals(saved.getSaveYn())
                || Boolean.TRUE.equals(draft.getSaveYn())) return 0;
        plans.put(planId, TripPlan.builder()
                .tripPlanId(planId).userId(saved.getUserId()).saveYn(true)
                .anchorContentId(draft.getAnchorContentId()).title(draft.getTitle())
                .tripDate(draft.getTripDate()).visitStartTime(draft.getVisitStartTime())
                .visitEndTime(draft.getVisitEndTime()).aiYn(draft.getAiYn())
                .transportMd(draft.getTransportMd()).foodPreference(draft.getFoodPreference())
                .mealType(draft.getMealType()).lunchFoodPreference(draft.getLunchFoodPreference())
                .dinnerFoodPreference(draft.getDinnerFoodPreference()).cafeYn(draft.getCafeYn())
                .searchDates(draft.getSearchDates()).searchCategories(draft.getSearchCategories())
                .searchDistrict(draft.getSearchDistrict()).searchFreeYn(draft.getSearchFreeYn())
                .headcount(draft.getHeadcount()).build());
        return 1;
    }

    @Override
    public int deleteDraft(Long draftId) {
        TripPlan draft = plans.get(draftId);
        return draft != null && !Boolean.TRUE.equals(draft.getSaveYn()) && plans.remove(draftId) != null ? 1 : 0;
    }

    private void checkConstraints(Long tripPlanId) {
        List<TripItem> planRows = rows.values().stream().filter(row -> row.getTripPlanId().equals(tripPlanId)).toList();
        if (planRows.stream().anyMatch(row -> row.getSeqOrder() <= 0)) {
            throw new IllegalStateException("CK_TRIP_ITEM_SEQ 위반");
        }
        if (planRows.stream().map(TripItem::getSeqOrder).distinct().count() != planRows.size()) {
            throw new IllegalStateException("UK_TRIP_ITEM_SEQ 위반");
        }
        List<String> events = planRows.stream().map(TripItem::getEventContentId).filter(Objects::nonNull).toList();
        List<String> places = planRows.stream().map(TripItem::getPlaceContentId).filter(Objects::nonNull).toList();
        if (events.stream().distinct().count() != events.size() || places.stream().distinct().count() != places.size()) {
            throw new IllegalStateException("UK_TRIP_ITEM_EVENT/PLACE 위반");
        }
    }

    private PlanItemView toView(TripItem row) {
        String contentId = "EVENT".equals(row.getItemType()) ? row.getEventContentId() : row.getPlaceContentId();
        Display display = displays.getOrDefault(contentId, new Display(contentId, null, null, null, null, null, null, null));
        return PlanItemView.builder()
                .tripItemId(row.getTripItemId())
                .seqOrder(row.getSeqOrder())
                .itemType(row.getItemType())
                .eventContentId(row.getEventContentId())
                .placeContentId(row.getPlaceContentId())
                .startTime(row.getStartTime())
                .durationMin(row.getDurationMin())
                .aiReason(row.getAiReason())
                .timeFixYn(row.getTimeFixYn())
                .name(display.name())
                .addr(display.addr())
                .mapx(display.mapx() == null ? null : new BigDecimal(display.mapx()))
                .mapy(display.mapy() == null ? null : new BigDecimal(display.mapy()))
                .openTime(display.openTime())
                .closeTime(display.closeTime())
                .breakOpenTime(display.breakOpenTime())
                .breakCloseTime(display.breakCloseTime())
                .cuisineType(cuisines.get(contentId))
                .build();
    }

    private static TripItem.TripItemBuilder copy(TripItem row) {
        return TripItem.builder()
                .tripItemId(row.getTripItemId())
                .tripPlanId(row.getTripPlanId())
                .seqOrder(row.getSeqOrder())
                .itemType(row.getItemType())
                .eventContentId(row.getEventContentId())
                .placeContentId(row.getPlaceContentId())
                .startTime(row.getStartTime())
                .durationMin(row.getDurationMin())
                .aiReason(row.getAiReason())
                .timeFixYn(row.getTimeFixYn());
    }
}
