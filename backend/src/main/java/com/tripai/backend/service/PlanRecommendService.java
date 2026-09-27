package com.tripai.backend.service;

import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.PlanRecommendRequest;
import com.tripai.backend.domain.entity.RecommendEventView;
import com.tripai.backend.domain.entity.RestaurantView;
import com.tripai.backend.domain.entity.TripItem;
import com.tripai.backend.domain.entity.TripPlan;
import com.tripai.backend.repository.PlaceMapper;
import com.tripai.backend.repository.PlanDraftMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-PLAN-001 문화행사 1개 기준 맛집 조합 일정 초안 생성 (AI-002·004·005, FOOD-002).
 * AI 연동 전이라 규칙 기반으로 만든다 (ai_yn = FALSE).
 *
 * 규칙 — 명세에 값이 없어 정한 부분은 docs/07 [제안]
 * 1. 행사는 여행 날짜에 진행 중이어야 한다 (아니면 400 행사일 불일치).
 * 2. 행사 시작 시간이 있으면 그 시간에 고정, 소요시간은 종료 시간까지(없으면 120분).
 * 3. 식사는 점심 12:30 / 저녁 17:00 에 60분. 행사와 겹치면 행사 직전·직후로 옮긴다.
 * 4. 맛집은 선택한 음식 종류 중 식사 60분 동안 영업하고 브레이크타임과 겹치지 않는 곳을
 *    행사장에서 가까운 순으로 고른다 (반경 1.5km → 3km → 5km).
 * 5. 맛집을 하나도 못 찾으면 초안을 만들지 않고 422 (추천 후보 부족).
 */
@Service
public class PlanRecommendService {

    static final LocalTime DEFAULT_VISIT_START = LocalTime.of(10, 0);
    static final LocalTime DEFAULT_VISIT_END = LocalTime.of(21, 0);
    static final int DEFAULT_EVENT_DURATION = 120;
    static final int MEAL_DURATION = 60;
    static final int[] SEARCH_RADII = {1500, 3000, 5000};
    static final int CANDIDATE_LIMIT = 20;

    private static final double METERS_PER_DEGREE_LAT = 111_320d;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<String, String> CUISINE_LABELS = Map.of(
            "KOREAN", "한식", "CHINESE", "중식", "JAPANESE", "일식", "WESTERN", "양식");

    enum MealType {
        LUNCH(LocalTime.of(12, 30), "점심"),
        DINNER(LocalTime.of(17, 0), "저녁");

        final LocalTime time;
        final String label;

        MealType(LocalTime time, String label) {
            this.time = time;
            this.label = label;
        }
    }

    private final PlanDraftMapper planDraftMapper;
    private final PlaceMapper placeMapper;
    private final PlanDraftService planDraftService;

    public PlanRecommendService(PlanDraftMapper planDraftMapper, PlaceMapper placeMapper,
                                PlanDraftService planDraftService) {
        this.planDraftMapper = planDraftMapper;
        this.placeMapper = placeMapper;
        this.planDraftService = planDraftService;
    }

    @Transactional
    public DraftResponse recommend(Long userId, PlanRecommendRequest request) {
        RecommendEventView event = planDraftMapper.findRecommendEvent(request.eventId())
                .filter(found -> Boolean.TRUE.equals(found.getDisplayYn()))
                .orElseThrow(() -> new PlanRuleException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));

        LocalDate visitDate = request.visitDate();
        if (visitDate.isBefore(event.getEventStartDate()) || visitDate.isAfter(event.getEventEndDate())) {
            throw new PlanRuleException(HttpStatus.BAD_REQUEST,
                    event.getEventName() + "은(는) " + visitDate + "에 진행하지 않는 행사입니다. ("
                            + event.getEventStartDate() + " ~ " + event.getEventEndDate() + ")");
        }

        LocalTime requestStart = parse(request.startTime());
        LocalTime requestEnd = parse(request.endTime());
        LocalTime eventStart = event.getEventStartTime() != null
                ? event.getEventStartTime()
                : (requestStart != null ? requestStart : DEFAULT_VISIT_START);
        LocalTime eventEnd = eventStart.plusMinutes(eventDuration(event));

        // 방문 시간을 안 보내면 기본 10:00~21:00 에 행사 시간이 들어가도록 넓힌다
        LocalTime windowStart = requestStart != null ? requestStart : earlier(DEFAULT_VISIT_START, eventStart);
        LocalTime windowEnd = requestEnd != null ? requestEnd : later(DEFAULT_VISIT_END, eventEnd);
        if (!windowEnd.isAfter(windowStart)) {
            throw new PlanRuleException(HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간보다 늦어야 합니다.");
        }
        if (eventStart.isBefore(windowStart) || eventEnd.isAfter(windowEnd) || eventEnd.isBefore(eventStart)) {
            throw new PlanRuleException(HttpStatus.BAD_REQUEST,
                    "방문 시간(" + format(windowStart) + "~" + format(windowEnd) + ") 안에 행사 시간("
                            + format(eventStart) + "~" + format(eventEnd) + ")이 들어가지 않습니다.");
        }
        if (event.getMapx() == null || event.getMapy() == null) {
            throw new PlanRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "행사 위치 정보가 없어 주변 맛집을 찾을 수 없습니다.");
        }

        String foodPreference = request.foodPreference() == null ? "ALL" : request.foodPreference();
        List<MealType> meals = request.mealType() == null
                ? List.of(MealType.LUNCH, MealType.DINNER)
                : List.of(MealType.valueOf(request.mealType()));

        List<Slot> occupied = new ArrayList<>(List.of(new Slot(eventStart, eventEnd)));
        List<TripItem> items = new ArrayList<>();
        items.add(TripItem.builder()
                .itemType("EVENT")
                .eventContentId(event.getEventContentId())
                .startTime(eventStart)
                .durationMin((int) Duration.between(eventStart, eventEnd).toMinutes())
                .aiReason(eventReason(event, visitDate))
                .timeFixYn(event.getEventStartTime() != null)
                .build());

        Set<String> chosenPlaces = new HashSet<>();
        for (MealType meal : meals) {
            pickRestaurant(meal, event, foodPreference, windowStart, windowEnd, occupied, chosenPlaces)
                    .ifPresent(items::add);
        }
        if (chosenPlaces.isEmpty()) {
            throw new PlanRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "조건에 맞는 맛집 후보가 부족합니다. 음식 종류나 식사 시간을 바꿔 보세요.");
        }

        items.sort(Comparator.comparing(TripItem::getStartTime));
        List<PlanItemRules.Candidate> candidates = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            TripItem item = items.get(index);
            candidates.add(new PlanItemRules.Candidate(item.getItemType(),
                    "EVENT".equals(item.getItemType()) ? item.getEventContentId() : item.getPlaceContentId(),
                    "", item.getStartTime(), item.getDurationMin(), index + 1));
        }
        PlanItemRules.check(candidates, event.getEventContentId(), windowStart, windowEnd, true)
                .ifPresent(result -> {
                    throw new IllegalStateException("추천 결과가 일정 규칙에 맞지 않습니다: " + result.message());
                });

        TripPlan plan = TripPlan.builder()
                .userId(userId)
                .anchorContentId(event.getEventContentId())
                .title(defaultTitle(visitDate, event.getEventName()))
                .tripDate(visitDate)
                .visitStartTime(windowStart)
                .visitEndTime(windowEnd)
                .aiYn(false)
                .transportMd(request.transportMode())
                .foodPreference(foodPreference)
                .mealType(request.mealType())
                .headcount(request.headcount() == null ? 1 : request.headcount())
                .build();
        planDraftMapper.insertPlan(plan);

        for (int index = 0; index < items.size(); index++) {
            TripItem item = items.get(index);
            planDraftMapper.insertItem(TripItem.builder()
                    .tripPlanId(plan.getTripPlanId())
                    .seqOrder(index + 1)
                    .itemType(item.getItemType())
                    .eventContentId(item.getEventContentId())
                    .placeContentId(item.getPlaceContentId())
                    .startTime(item.getStartTime())
                    .durationMin(item.getDurationMin())
                    .aiReason(item.getAiReason())
                    .timeFixYn(item.getTimeFixYn())
                    .build());
        }

        return planDraftService.getDraft(userId, plan.getTripPlanId());
    }

    /**
     * 식사 시간 후보(기본 → 행사 직전 → 행사 직후) 중 방문 시간 안에서 다른 일정과 겹치지 않는 시간에
     * 영업 중인 맛집을 가까운 순으로 찾는다.
     */
    private Optional<TripItem> pickRestaurant(MealType meal, RecommendEventView event, String foodPreference,
                                              LocalTime windowStart, LocalTime windowEnd,
                                              List<Slot> occupied, Set<String> chosenPlaces) {
        LocalTime eventStart = occupied.get(0).start();
        LocalTime eventEnd = occupied.get(0).end();
        List<LocalTime> times = new ArrayList<>(List.of(meal.time));
        if (!eventStart.isBefore(LocalTime.of(1, 0))) {
            times.add(eventStart.minusMinutes(MEAL_DURATION));
        }
        times.add(eventEnd);

        for (LocalTime start : times) {
            LocalTime end = start.plusMinutes(MEAL_DURATION);
            Slot slot = new Slot(start, end);
            if (start.isBefore(windowStart) || end.isAfter(windowEnd) || end.isBefore(start)
                    || occupied.stream().anyMatch(slot::overlaps)) {
                continue;
            }
            Optional<RestaurantView> found = findOpenRestaurant(event, foodPreference, start, chosenPlaces);
            if (found.isPresent()) {
                RestaurantView restaurant = found.get();
                occupied.add(slot);
                chosenPlaces.add(restaurant.getContentId());
                return Optional.of(TripItem.builder()
                        .itemType("PLACE")
                        .placeContentId(restaurant.getContentId())
                        .startTime(start)
                        .durationMin(MEAL_DURATION)
                        .aiReason(placeReason(meal, start, restaurant))
                        .timeFixYn(false)
                        .build());
            }
        }
        return Optional.empty();
    }

    private Optional<RestaurantView> findOpenRestaurant(RecommendEventView event, String foodPreference,
                                                        LocalTime start, Set<String> chosenPlaces) {
        double lat = event.getMapy().doubleValue();
        double lng = event.getMapx().doubleValue();
        for (int radius : SEARCH_RADII) {
            double latDelta = radius / METERS_PER_DEGREE_LAT;
            double lngDelta = radius / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
            Optional<RestaurantView> found = placeMapper.findRestaurantsNear(lat, lng, radius,
                            lat - latDelta, lat + latDelta, lng - lngDelta, lng + lngDelta,
                            foodPreference, start, CANDIDATE_LIMIT, 0)
                    .stream()
                    .filter(restaurant -> !chosenPlaces.contains(restaurant.getContentId()))
                    .filter(restaurant -> openDuringMeal(restaurant, start))
                    .findFirst();
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** 식사 60분 내내 영업하고 브레이크타임과 겹치지 않는지 (영업시간 모르면 제외 — FOOD-002) */
    static boolean openDuringMeal(RestaurantView restaurant, LocalTime start) {
        LocalTime open = restaurant.getOpenTime();
        LocalTime close = restaurant.getCloseTime();
        if (open == null || close == null) {
            return false;
        }
        int from = minutes(start);
        int to = from + MEAL_DURATION;
        int openAt = minutes(open);
        int closeAt = minutes(close) <= openAt ? minutes(close) + 24 * 60 : minutes(close);
        if (from < openAt || to > closeAt) {
            return false;
        }
        LocalTime breakOpen = restaurant.getBreakOpenTime();
        LocalTime breakClose = restaurant.getBreakCloseTime();
        return breakOpen == null || breakClose == null
                || to <= minutes(breakOpen) || from >= minutes(breakClose);
    }

    /** AI-004 — 행사는 조건 일치를 근거로 짧게 설명 */
    private static String eventReason(RecommendEventView event, LocalDate visitDate) {
        String time = event.getEventStartTime() != null
                ? format(event.getEventStartTime()) + " 시작"
                : "시작 시간 정보 없음";
        return "선택한 행사 · " + visitDate.getMonthValue() + "월 " + visitDate.getDayOfMonth() + "일 진행 · " + time;
    }

    /** AI-004 — 맛집은 거리·시간대·영업시간을 근거로 짧게 설명 (확인된 값만 쓴다) */
    private static String placeReason(MealType meal, LocalTime start, RestaurantView restaurant) {
        StringBuilder reason = new StringBuilder()
                .append(meal.label).append(' ').append(format(start))
                .append(" · 행사장에서 약 ").append(Math.round(restaurant.getDistance())).append("m");
        String cuisine = CUISINE_LABELS.get(restaurant.getCuisineType());
        if (cuisine != null) {
            reason.append(" · ").append(cuisine);
        }
        reason.append(" · 영업 ").append(format(restaurant.getOpenTime())).append('~').append(format(restaurant.getCloseTime()));
        return reason.length() > 100 ? reason.substring(0, 100) : reason.toString();
    }

    /** AI-005 제목 생성 실패 시 기본 제목 — 날짜와 행사명 조합 */
    static String defaultTitle(LocalDate visitDate, String eventName) {
        String title = visitDate.getMonthValue() + "월 " + visitDate.getDayOfMonth() + "일 " + eventName;
        return title.length() > 100 ? title.substring(0, 100) : title;
    }

    private static int eventDuration(RecommendEventView event) {
        LocalTime start = event.getEventStartTime();
        LocalTime end = event.getEventEndTime();
        if (start != null && end != null && end.isAfter(start)) {
            return (int) Duration.between(start, end).toMinutes();
        }
        return DEFAULT_EVENT_DURATION;
    }

    private static LocalTime parse(String time) {
        return time == null ? null : LocalTime.parse(time, HH_MM);
    }

    private static LocalTime earlier(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalTime later(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static int minutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    private static String format(LocalTime time) {
        return time == null ? null : time.format(HH_MM);
    }

    private record Slot(LocalTime start, LocalTime end) {
        boolean overlaps(Slot other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }
    }
}
