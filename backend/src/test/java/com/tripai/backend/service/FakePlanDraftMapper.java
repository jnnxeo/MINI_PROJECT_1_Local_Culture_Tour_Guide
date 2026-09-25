package com.tripai.backend.service;

import com.tripai.backend.domain.entity.ItemTargetView;
import com.tripai.backend.domain.entity.PlanItemView;
import com.tripai.backend.domain.entity.TripItem;
import com.tripai.backend.domain.entity.TripPlan;
import com.tripai.backend.repository.PlanDraftMapper;
import java.math.BigDecimal;
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
    final Map<Long, String> titles = new HashMap<>();

    void addRow(TripItem item) {
        rows.put(item.getTripItemId(), item);
        checkConstraints(item.getTripPlanId());
    }

    @Override
    public Optional<TripPlan> findPlanById(Long tripPlanId) {
        return Optional.ofNullable(plans.get(tripPlanId));
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
