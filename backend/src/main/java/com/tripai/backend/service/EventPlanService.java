package com.tripai.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripai.backend.domain.dto.event.EventDetailResponse;
import com.tripai.backend.domain.dto.plan.CreateEventDraftRequest;
import com.tripai.backend.domain.dto.plan.EventPlaceCandidateRow;
import com.tripai.backend.domain.dto.plan.EventPlanDetailResponse;
import com.tripai.backend.domain.dto.plan.EventPlanItemResponse;
import com.tripai.backend.domain.dto.plan.EventPlanItemRow;
import com.tripai.backend.domain.dto.plan.EventPlanRow;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.repository.EventMapper;
import com.tripai.backend.repository.EventPlanMapper;

import lombok.RequiredArgsConstructor;

/** EVENT-004 전용 생성 서비스. 저장 일정 읽기·편집·확정은 Plan 담당 서비스에 맡긴다. */
@Service
@RequiredArgsConstructor
public class EventPlanService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private final EventPlanMapper plans;
    private final EventMapper events;

    @Transactional
    public EventPlanDetailResponse createDraft(long userId, CreateEventDraftRequest request) {
        if (!Set.of("WALK", "WALK_TRANSIT").contains(request.transportMode())
                || request.headcount() > 4
                || request.interests().stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        EventDetailResponse event = requireEvent(request.anchorEventId(), request.tripDate());
        if (event.getLat() == null || event.getLng() == null) {
            throw new CustomException(ErrorCode.EVENT_COORDINATES_MISSING);
        }

        LocalTime eventStart = event.getStartTime() == null ? LocalTime.of(15, 0) : event.getStartTime();
        LocalTime eventEnd = event.getEndTime() == null ? eventStart.plusHours(1) : event.getEndTime();
        if (!eventEnd.isAfter(eventStart)) throw new CustomException(ErrorCode.INVALID_INPUT);
        LocalTime visitStart = eventStart.isBefore(LocalTime.of(10, 0))
                ? eventStart : LocalTime.of(10, 0);
        LocalTime visitEnd = eventEnd.isAfter(LocalTime.of(21, 0))
                ? eventEnd : LocalTime.of(21, 0);

        LocalTime mealStart = LocalTime.of(12, 30);
        if (overlaps(mealStart, mealStart.plusHours(1), eventStart, eventEnd)) {
            mealStart = eventEnd.plusMinutes(30);
            if (mealStart.plusHours(1).isAfter(LocalTime.of(21, 0))) {
                mealStart = eventStart.minusMinutes(90);
            }
        }
        if (mealStart.isBefore(LocalTime.of(10, 0))
                || mealStart.plusHours(1).isAfter(LocalTime.of(21, 0))
                || overlaps(mealStart, mealStart.plusHours(1), eventStart, eventEnd)) {
            mealStart = null;
        }

        // 지역 음식점 데이터가 비었거나 식사 시간이 맞지 않아도 행사만으로 당일 초안을 만든다.
        EventPlaceCandidateRow place = mealStart == null ? null : plans.findNearbyRestaurant(
                event.getLat(), event.getLng(), mealStart, mealStart.plusHours(1));

        // 한 사용자의 미저장 초안은 하나만 유지한다. 일정·항목·관심사를 한 트랜잭션에서 교체한다.
        plans.deleteExistingDraft(userId);
        EventPlanRow plan = new EventPlanRow();
        plan.setUserId(userId);
        plan.setAnchorEventId(event.getEventId());
        String title = request.tripDate().format(DateTimeFormatter.ofPattern("MM.dd"))
                + " " + event.getTitle();
        plan.setTitle(title.length() > 100 ? title.substring(0, 100) : title);
        plan.setTripDate(request.tripDate());
        plan.setVisitStartTime(visitStart);
        plan.setVisitEndTime(visitEnd);
        plan.setTransportMode(request.transportMode());
        plan.setHeadcount(request.headcount());
        plans.insertPlan(plan);

        for (String interest : new HashSet<>(request.interests())) {
            plans.insertInterest(plan.getPlanId(), interest);
        }

        if (place == null) {
            plans.insertItem(plan.getPlanId(), 1, "EVENT", event.getEventId(), eventStart,
                    (int) ChronoUnit.MINUTES.between(eventStart, eventEnd), null,
                    event.getStartTime() != null);
        } else if (mealStart.isBefore(eventStart)) {
            plans.insertItem(plan.getPlanId(), 1, "PLACE", place.getContentId(), mealStart, 60,
                    "행사장 근처에서 식사할 수 있는 장소", false);
            plans.insertItem(plan.getPlanId(), 2, "EVENT", event.getEventId(), eventStart,
                    (int) ChronoUnit.MINUTES.between(eventStart, eventEnd), null,
                    event.getStartTime() != null);
        } else {
            plans.insertItem(plan.getPlanId(), 1, "EVENT", event.getEventId(), eventStart,
                    (int) ChronoUnit.MINUTES.between(eventStart, eventEnd), null,
                    event.getStartTime() != null);
            plans.insertItem(plan.getPlanId(), 2, "PLACE", place.getContentId(), mealStart, 60,
                    "행사장 근처에서 식사할 수 있는 장소", false);
        }

        // AI 미연결: useAi=true여도 규칙 기반 결과(ai_yn=false)를 반환한다.
        EventPlanRow inserted = plans.findPlan(plan.getPlanId());
        return toDetail(inserted);
    }

    /** EVENT-003: 저장 일정의 소유자와 상태를 확인한 뒤 행사 한 건을 원자적으로 추가한다. */
    @Transactional
    public EventPlanDetailResponse addEventToSavedPlan(long userId, long planId,
                                                        String eventId, LocalTime requestedStart) {
        EventPlanRow plan = plans.findSavedPlanForUpdate(planId, userId);
        if (plan == null) throw new CustomException(ErrorCode.PLAN_NOT_FOUND);
        EventDetailResponse event = events.selectEventForRecommendation(eventId);
        if (event == null) throw new CustomException(ErrorCode.EVENT_NOT_FOUND);
        if (plan.getTripDate() == null || event.getStartDate() == null || event.getEndDate() == null
                || plan.getTripDate().isBefore(event.getStartDate())
                || plan.getTripDate().isAfter(event.getEndDate())) {
            throw new CustomException(ErrorCode.EVENT_DATE_UNAVAILABLE);
        }

        List<EventPlanItemRow> items = plans.findItems(planId);
        if (items.stream().anyMatch(item -> "EVENT".equals(item.getType())
                && eventId.equals(item.getContentId()))) {
            throw new CustomException(ErrorCode.EVENT_ALREADY_IN_PLAN);
        }
        if (items.stream().filter(item -> "EVENT".equals(item.getType())).count() >= 2) {
            throw new CustomException(ErrorCode.EVENT_PLAN_LIMIT);
        }

        LocalTime start = event.getStartTime() == null ? requestedStart : event.getStartTime();
        if (start == null || event.getStartTime() != null
                && requestedStart != null && !event.getStartTime().equals(requestedStart)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        int duration = event.getStartTime() == null || event.getEndTime() == null ? 60
                : (int) ChronoUnit.MINUTES.between(start, event.getEndTime());
        if (duration <= 0 || duration > 1440 || start.toSecondOfDay() / 60 + duration >= 1440) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        LocalTime end = start.plusMinutes(duration);
        if (plan.getVisitStartTime() != null && start.isBefore(plan.getVisitStartTime())
                || plan.getVisitEndTime() != null && end.isAfter(plan.getVisitEndTime())) {
            throw new CustomException(ErrorCode.EVENT_PLAN_TIME_CONFLICT);
        }
        for (EventPlanItemRow item : items) {
            if (overlaps(start, end, item.getStartTime(),
                    item.getStartTime().plusMinutes(item.getDurationMin()))) {
                throw new CustomException(ErrorCode.EVENT_PLAN_TIME_CONFLICT);
            }
        }

        int seq = items.stream().mapToInt(EventPlanItemRow::getSeq).max().orElse(0) + 1;
        plans.insertItem(planId, seq, "EVENT", eventId, start, duration, null,
                event.getStartTime() != null);
        resequenceItems(planId);
        return toDetail(plan);
    }

    @Transactional(readOnly = true)
    public EventPlanDetailResponse getSavedPlan(long userId, long planId) {
        EventPlanRow plan = plans.findPlan(planId);
        if (plan == null || !Boolean.TRUE.equals(plan.getSaved())
                || !Long.valueOf(userId).equals(plan.getUserId())) {
            throw new CustomException(ErrorCode.PLAN_NOT_FOUND);
        }
        return toDetail(plan);
    }

    @Transactional
    public EventPlanDetailResponse updateSavedEventTime(long userId, long planId,
                                                         long itemId, LocalTime start) {
        EventPlanRow plan = plans.findSavedPlanForUpdate(planId, userId);
        if (plan == null) throw new CustomException(ErrorCode.PLAN_NOT_FOUND);
        List<EventPlanItemRow> items = plans.findItems(planId);
        EventPlanItemRow target = items.stream()
                .filter(item -> itemId == item.getItemId() && "EVENT".equals(item.getType()))
                .findFirst().orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
        if (Boolean.TRUE.equals(target.getTimeFixed())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        int endMinute = start.toSecondOfDay() / 60 + target.getDurationMin();
        if (endMinute >= 1440 || plan.getVisitStartTime() != null
                && start.isBefore(plan.getVisitStartTime())
                || plan.getVisitEndTime() != null
                && start.plusMinutes(target.getDurationMin()).isAfter(plan.getVisitEndTime())) {
            throw new CustomException(ErrorCode.EVENT_PLAN_TIME_CONFLICT);
        }
        LocalTime end = start.plusMinutes(target.getDurationMin());
        for (EventPlanItemRow item : items) {
            if (!item.getItemId().equals(itemId) && overlaps(start, end, item.getStartTime(),
                    item.getStartTime().plusMinutes(item.getDurationMin()))) {
                throw new CustomException(ErrorCode.EVENT_PLAN_TIME_CONFLICT);
            }
        }
        plans.updateItemStartTime(planId, itemId, start);
        resequenceItems(planId);
        return toDetail(plan);
    }

    private void resequenceItems(long planId) {
        List<EventPlanItemRow> sorted = plans.findItems(planId).stream()
                .sorted(java.util.Comparator.comparing(EventPlanItemRow::getStartTime)
                        .thenComparing(EventPlanItemRow::getItemId)).toList();
        int maxSeq = sorted.stream().mapToInt(EventPlanItemRow::getSeq).max().orElse(0);
        plans.offsetItemSeq(planId, maxSeq + 1);
        for (int index = 0; index < sorted.size(); index++) {
            plans.updateItemSeq(planId, sorted.get(index).getItemId(), index + 1);
        }
    }

    private EventDetailResponse requireEvent(String eventId, LocalDate tripDate) {
        EventDetailResponse event = events.selectEventForRecommendation(eventId);
        if (event == null) throw new CustomException(ErrorCode.EVENT_NOT_FOUND);
        if (tripDate.isBefore(LocalDate.now(SEOUL))
                || tripDate.isBefore(event.getStartDate())
                || tripDate.isAfter(event.getEndDate())) {
            throw new CustomException(ErrorCode.EVENT_DATE_UNAVAILABLE);
        }
        return event;
    }

    private EventPlanDetailResponse toDetail(EventPlanRow row) {
        List<EventPlanItemRow> itemRows = plans.findItems(row.getPlanId());
        List<EventPlanItemResponse> items = itemRows.stream()
                .map(item -> new EventPlanItemResponse(
                        item.getItemId(), item.getSeq(), item.getType(), item.getContentId(),
                        item.getName(), item.getCategory(), item.getAddress(), item.getLat(),
                        item.getLng(), item.getImageUrl(), time(item.getStartTime()),
                        item.getDurationMin(), time(item.getStartTime().plusMinutes(item.getDurationMin())),
                        Boolean.TRUE.equals(item.getTimeFixed()), item.getAiReason(),
                        time(item.getOpenTime()), time(item.getCloseTime())))
                .toList();
        List<String> warnings = new ArrayList<>(itemRows.stream()
                .filter(item -> "PLACE".equals(item.getType())
                        && (item.getOpenTime() == null || item.getCloseTime() == null))
                .map(item -> item.getName() + "의 영업시간은 방문 전 확인해 주세요.")
                .toList());
        if (itemRows.stream().noneMatch(item -> "PLACE".equals(item.getType()))) {
            warnings.add("행사 주변에 추천할 음식점이 없어 행사만 일정에 담았습니다.");
        }
        return new EventPlanDetailResponse(
                row.getPlanId(), row.getTitle(), row.getTripDate(),
                time(row.getVisitStartTime()), time(row.getVisitEndTime()),
                row.getHeadcount() == null ? 1 : row.getHeadcount(),
                row.getTransportMode(), plans.findInterests(row.getPlanId()), row.getAnchorEventId(),
                Boolean.TRUE.equals(row.getSaved()), Boolean.TRUE.equals(row.getAiGenerated()),
                row.getTripDate() == null ? null
                        : ChronoUnit.DAYS.between(LocalDate.now(SEOUL), row.getTripDate()),
                items, warnings);
    }

    private static String time(LocalTime value) {
        return value == null ? null : value.format(CLOCK);
    }

    private static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
