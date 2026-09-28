package com.tripai.backend.service;

import com.tripai.backend.domain.dto.AiPlanInput;
import com.tripai.backend.domain.dto.AiPlanResponse;
import com.tripai.backend.domain.dto.DraftConditionsRequest;
import com.tripai.backend.domain.dto.DraftConditionsResponse;
import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.EventCandidate;
import com.tripai.backend.domain.dto.PlanGenerateRequest;
import com.tripai.backend.domain.dto.PlanScheduleItem;
import com.tripai.backend.domain.dto.PlanRecommendRequest;
import com.tripai.backend.domain.dto.RestaurantCandidate;
import com.tripai.backend.domain.entity.PlanItemView;
import com.tripai.backend.domain.entity.RecommendEventView;
import com.tripai.backend.domain.entity.RestaurantView;
import com.tripai.backend.domain.entity.TripItem;
import com.tripai.backend.domain.entity.TripPlan;
import com.tripai.backend.repository.PlaceMapper;
import com.tripai.backend.repository.PlanDraftMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 나의 일정 초안 추천 — API-PLAN-001 생성, 003 조건 수정, 004 다시 추천 (AI-002·004·005·009·010, FOOD-002).
 * 아래 규칙 기반 결과를 기본으로 만들고(ai_yn = FALSE), 키가 있으면 AI 결과가 검사를 통과할 때만 그것을 쓴다(ai_yn = TRUE).
 *
 * 규칙 — 명세에 값이 없어 정한 부분은 docs/07 [제안]
 * 1. 여행 날짜는 오늘 이후이고 행사가 그날 진행 중이어야 한다 (아니면 400 행사일 불일치).
 * 2. 행사 시작 시간이 있으면 그 시간에 고정, 소요시간은 종료 시간까지(없으면 120분).
 * 3. 식사는 점심 12:30 / 저녁 17:00 에 60분. 행사와 겹치면 행사 직전·직후로 옮긴다.
 * 4. 맛집은 선택한 음식 종류 중 식사 60분 동안 영업하고 브레이크타임과 겹치지 않는 곳을
 *    행사장에서 가까운 순으로 고른다 (반경 1.5km → 3km → 5km).
 * 5. 맛집을 하나도 못 찾으면 초안을 만들거나 바꾸지 않고 422 (추천 후보 부족).
 *
 * AI 추천 (OPENAI_API_KEY 가 있을 때, docs/07 [제안])
 * - 위 규칙으로 만든 결과를 기본값으로 두고, 선택 행사 + 주변 맛집 후보로 AI(ApiPlanService)에 일정을 맡긴다.
 * - AI 결과는 서버에서 다시 검사한다 (행사 1개·고정 시간, 후보 안의 맛집, 식사 시간대·영업시간·브레이크, 시간 겹침·범위,
 *   규칙 기반보다 식사 수가 적지 않은지, 다시 추천이면 지금 일정과 다른지).
 * - 키가 없거나, 호출이 실패하거나, 검사에 걸리면 규칙 기반 결과를 쓴다 → 사용자에게는 항상 규칙에 맞는 일정이 나간다.
 */
@Service
public class PlanRecommendService {

    static final LocalTime DEFAULT_VISIT_START = LocalTime.of(10, 0);
    static final LocalTime DEFAULT_VISIT_END = LocalTime.of(21, 0);
    static final int DEFAULT_EVENT_DURATION = 120;
    static final int MEAL_DURATION = 60;
    static final int[] SEARCH_RADII = {1500, 3000, 5000};
    static final int CANDIDATE_LIMIT = 20;
    static final int DAY_MINUTES = 24 * 60;
    static final int AI_CANDIDATE_LIMIT = 15;
    static final int MIN_EVENT_DURATION = 30;
    static final int MAX_EVENT_DURATION = 360;
    static final int MIN_MEAL_DURATION = 30;
    static final int MAX_MEAL_DURATION = 120;

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
    private final Clock clock;
    private final AiPlanClient aiPlanClient;

    private static final Logger log = LoggerFactory.getLogger(PlanRecommendService.class);

    /** AI 를 쓰지 않는 경우 (테스트 기본값) */
    static final AiPlanClient AI_DISABLED = new AiPlanClient() {
        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public AiPlanResponse generate(AiPlanInput input) {
            throw new IllegalStateException("AI 를 쓰지 않습니다.");
        }
    };

    @Autowired
    public PlanRecommendService(PlanDraftMapper planDraftMapper, PlaceMapper placeMapper,
                                PlanDraftService planDraftService, AiPlanClient aiPlanClient) {
        this(planDraftMapper, placeMapper, planDraftService, Clock.system(ZoneId.of("Asia/Seoul")), aiPlanClient);
    }

    // 오늘 날짜를 테스트에서 고정하려고 둔 생성자 (AI 없이 규칙 기반만)
    PlanRecommendService(PlanDraftMapper planDraftMapper, PlaceMapper placeMapper,
                         PlanDraftService planDraftService, Clock clock) {
        this(planDraftMapper, placeMapper, planDraftService, clock, AI_DISABLED);
    }

    PlanRecommendService(PlanDraftMapper planDraftMapper, PlaceMapper placeMapper,
                         PlanDraftService planDraftService, Clock clock, AiPlanClient aiPlanClient) {
        this.planDraftMapper = planDraftMapper;
        this.placeMapper = placeMapper;
        this.planDraftService = planDraftService;
        this.clock = clock;
        this.aiPlanClient = aiPlanClient;
    }

    /** API-PLAN-001 문화행사 1개 기준 맛집 조합 초안 생성 */
    @Transactional
    public DraftResponse recommend(Long userId, PlanRecommendRequest request) {
        checkNotPast(request.visitDate());
        RecommendEventView event = findEvent(request.eventId());
        String foodPreference = request.foodPreference() == null ? "ALL" : request.foodPreference();
        String mealType = normalizeMealType(request.mealType());
        Generated generated = generate(event, request.visitDate(), parse(request.startTime()), parse(request.endTime()),
                foodPreference, mealType, List.of());

        TripPlan plan = TripPlan.builder()
                .userId(userId)
                .anchorContentId(event.getEventContentId())
                .title(generated.title() != null ? generated.title() : defaultTitle(request.visitDate(), event.getEventName()))
                .tripDate(request.visitDate())
                .visitStartTime(generated.windowStart())
                .visitEndTime(generated.windowEnd())
                .aiYn(generated.aiUsed())
                .transportMd(request.transportMode())
                .foodPreference(foodPreference)
                .mealType(mealType)
                .headcount(request.headcount() == null ? 1 : request.headcount())
                .build();
        planDraftMapper.insertPlan(plan);
        insertItems(plan.getTripPlanId(), generated.items());

        return planDraftService.getDraft(userId, plan.getTripPlanId());
    }

    /**
     * API-PLAN-003 추천 조건 수정 (AI-009 "조건 수정 후 전체 초안을 다시 생성"). 보내지 않은 값은 지금 조건을 그대로 쓴다.
     * 새 조건으로 일정을 만든 뒤 조건과 일정 항목을 한 트랜잭션에서 함께 바꾼다 — 화면은 003 → 002 로 끝난다.
     * 새 조건으로 추천이 안 되면(행사일·방문 시간 400, 후보 부족 422) 아무것도 바꾸지 않아
     * 기존 초안(조건·항목·추천 이유·itemId)이 그대로 유지된다 (AI-009 추천 실패 시 기존 초안 유지).
     * 새 조건에서 가장 알맞은 곳(가까운 곳)부터 고르고, 같은 조건으로 다른 조합을 찾는 건 004 가 맡는다.
     */
    @Transactional
    public DraftConditionsResponse updateConditions(Long userId, Long draftId, DraftConditionsRequest request) {
        TripPlan plan = planDraftService.lockOwnedDraft(userId, draftId);
        RecommendEventView event = findEvent(plan.getAnchorContentId());

        LocalDate visitDate = request.visitDate() != null ? request.visitDate() : plan.getTripDate();
        if (request.visitDate() != null) {
            checkNotPast(visitDate);
        }
        LocalTime start = request.startTime() != null ? parse(request.startTime()) : plan.getVisitStartTime();
        LocalTime end = request.endTime() != null ? parse(request.endTime()) : plan.getVisitEndTime();
        String foodPreference = firstNonNull(request.foodPreference(), plan.getFoodPreference(), "ALL");
        String mealType = request.mealType() != null ? normalizeMealType(request.mealType()) : plan.getMealType();
        String transportMode = request.transportMode() != null ? request.transportMode() : plan.getTransportMd();

        Generated generated = generate(event, visitDate, start, end, foodPreference, mealType, List.of());

        TripPlan updated = copyConditions(plan, visitDate, generated.windowStart(), generated.windowEnd(),
                transportMode, foodPreference, mealType, generated.aiUsed());
        planDraftMapper.updateConditions(updated);
        planDraftMapper.deleteItemsByPlanId(draftId);
        insertItems(draftId, generated.items());
        return new DraftConditionsResponse(draftId, PlanDraftService.toConditions(updated));
    }

    /**
     * API-PLAN-004 지금 조건 그대로 일정 다시 추천 (AI-010).
     * 기준 행사는 사용자가 고른 행사라 그대로 두고, 맛집은 지금과 다른 곳을 먼저 찾는다.
     * 결과가 지금 초안과 같으면(대체 후보 없음) 바꾸지 않고 422. 제목은 사용자가 바꿨을 수 있어 유지한다.
     */
    @Transactional
    public DraftResponse regenerate(Long userId, Long draftId) {
        TripPlan plan = planDraftService.lockOwnedDraft(userId, draftId);
        RecommendEventView event = findEvent(plan.getAnchorContentId());
        List<PlanItemView> current = planDraftMapper.findItemsByPlanId(draftId);
        String foodPreference = plan.getFoodPreference() == null ? "ALL" : plan.getFoodPreference();

        Generated generated = generate(event, plan.getTripDate(), plan.getVisitStartTime(), plan.getVisitEndTime(),
                foodPreference, plan.getMealType(), current);
        if (sameItems(current, generated.items())) {
            throw new PlanRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "지금 조건으로는 다른 추천 후보가 없어 기존 일정을 유지합니다. 조건을 바꿔 다시 시도해 주세요.");
        }

        planDraftMapper.updateConditions(copyConditions(plan, plan.getTripDate(), generated.windowStart(),
                generated.windowEnd(), plan.getTransportMd(), foodPreference, plan.getMealType(), generated.aiUsed()));
        planDraftMapper.deleteItemsByPlanId(draftId);
        insertItems(draftId, generated.items());
        return planDraftService.getDraft(userId, draftId);
    }

    private RecommendEventView findEvent(String eventContentId) {
        return planDraftMapper.findRecommendEvent(eventContentId)
                .filter(found -> Boolean.TRUE.equals(found.getDisplayYn()))
                .orElseThrow(() -> new PlanRuleException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));
    }

    /**
     * 행사 1개 + 식사 맛집으로 항목을 만든다. DB 에는 쓰지 않는다.
     * @param requestStart 방문 시작 시각 (null 이면 10:00, 행사가 더 이르면 행사 시작)
     * @param requestEnd   방문 종료 시각 (null 이면 21:00, 행사가 더 늦으면 행사 종료)
     * @param currentItems 지금 초안 항목 — 다시 추천에서 지금 맛집보다 한 단계 먼 곳을 고른다 (처음 만들 때는 빈 목록)
     */
    Generated generate(RecommendEventView event, LocalDate visitDate, LocalTime requestStart, LocalTime requestEnd,
                       String foodPreference, String mealType, List<PlanItemView> currentItems) {
        if (visitDate.isBefore(event.getEventStartDate()) || visitDate.isAfter(event.getEventEndDate())) {
            throw new PlanRuleException(HttpStatus.BAD_REQUEST,
                    event.getEventName() + "은(는) " + visitDate + "에 진행하지 않는 행사입니다. ("
                            + event.getEventStartDate() + " ~ " + event.getEventEndDate() + ")");
        }

        LocalTime eventStart = event.getEventStartTime() != null
                ? event.getEventStartTime()
                : (requestStart != null ? requestStart : DEFAULT_VISIT_START);
        LocalTime eventEnd = eventStart.plusMinutes(eventDuration(event));

        // 방문 시간이 없으면 기본 10:00~21:00 에 행사 시간이 들어가도록 넓힌다
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

        List<MealType> meals = mealType == null
                ? List.of(MealType.LUNCH, MealType.DINNER)
                : List.of(MealType.valueOf(mealType));

        Current current = current(event, currentItems, foodPreference, mealTimes(meals, eventStart, eventEnd));
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
            pickRestaurant(meal, event, foodPreference, windowStart, windowEnd, occupied, chosenPlaces, current)
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
            candidates.add(new PlanItemRules.Candidate(item.getItemType(), contentIdOf(item),
                    "", item.getStartTime(), item.getDurationMin(), index + 1));
        }
        PlanItemRules.check(candidates, event.getEventContentId(), windowStart, windowEnd, true)
                .ifPresent(result -> {
                    throw new IllegalStateException("추천 결과가 일정 규칙에 맞지 않습니다: " + result.message());
                });
        Generated ruleBased = new Generated(windowStart, windowEnd, items, false, null);

        if (!aiPlanClient.isEnabled()) {
            return ruleBased;
        }
        // 다시 추천에서는 지금 조건에 맞는지와 상관없이 지금 일정의 맛집은 모두 AI 후보에서 뺀다
        Set<String> currentPlaces = new HashSet<>();
        currentItems.stream().map(PlanItemView::getPlaceContentId).filter(Objects::nonNull).forEach(currentPlaces::add);
        int ruleBasedMeals = (int) items.stream().filter(item -> !"EVENT".equals(item.getItemType())).count();
        return tryAi(event, visitDate, windowStart, windowEnd, eventStart, eventEnd, foodPreference, mealType, meals,
                ruleBasedMeals, currentPlaces, currentItems).orElse(ruleBased);
    }

    /**
     * 선택 행사 + 주변 맛집 후보로 AI 에 일정을 맡기고, 서버 규칙을 모두 통과할 때만 쓴다.
     * 실패·규칙 위반이면 빈 값 → 규칙 기반 결과를 쓴다.
     */
    private Optional<Generated> tryAi(RecommendEventView event, LocalDate visitDate, LocalTime windowStart, LocalTime windowEnd,
                                      LocalTime eventStart, LocalTime eventEnd, String foodPreference, String mealType,
                                      List<MealType> meals, int ruleBasedMeals, Set<String> avoidPlaces,
                                      List<PlanItemView> currentItems) {
        Map<String, RestaurantView> restaurants = aiRestaurantCandidates(event, foodPreference, avoidPlaces);
        if (restaurants.isEmpty()) {
            return Optional.empty();
        }
        AiPlanInput input = AiPlanInput.builder()
                .userConditions(PlanGenerateRequest.builder()
                        .tripDate(visitDate)
                        .visitStartTime(format(windowStart))
                        .visitEndTime(format(windowEnd))
                        .foodPreference(foodPreference)
                        .mealType(mealType)
                        .requiredEventId(event.getEventContentId())
                        .build())
                .eventCandidates(List.of(EventCandidate.builder()
                        .eventContentId(event.getEventContentId())
                        .eventName(event.getEventName())
                        .eventType(event.getEventType())
                        .districtName(event.getDistrictName())
                        .eventPlace(event.getEventPlace())
                        .freeYn(event.getFreeYn())
                        .eventStartDate(event.getEventStartDate())
                        .eventEndDate(event.getEventEndDate())
                        .eventStartTime(event.getEventStartTime())
                        .eventEndTime(event.getEventEndTime())
                        .dateText(event.getDateText())
                        .build()))
                .restaurantCandidates(restaurants.values().stream()
                        .map(restaurant -> RestaurantCandidate.builder()
                                .contentId(restaurant.getContentId())
                                .placeName(restaurant.getPlaceName())
                                .addr(restaurant.getAddr())
                                .cuisineType(restaurant.getCuisineType())
                                .openTime(restaurant.getOpenTime())
                                .closeTime(restaurant.getCloseTime())
                                .breakOpenTime(restaurant.getBreakOpenTime())
                                .breakCloseTime(restaurant.getBreakCloseTime())
                                .distanceMeters(Math.round(restaurant.getDistance()))
                                .build())
                        .toList())
                .build();
        try {
            AiPlanResponse response = aiPlanClient.generate(input);
            List<TripItem> items = toAiItems(response, event, eventStart, eventEnd, restaurants, meals, windowStart, windowEnd);
            long aiMeals = items.stream().filter(item -> !"EVENT".equals(item.getItemType())).count();
            if (aiMeals < ruleBasedMeals) {
                // 규칙 기반으로는 채운 식사를 AI 가 빼먹으면 일정이 줄어드므로 쓰지 않는다
                throw new IllegalStateException("식사 " + aiMeals + "번 (규칙 기반 " + ruleBasedMeals + "번)");
            }
            if (!currentItems.isEmpty() && sameItems(currentItems, items)) {
                throw new IllegalStateException("지금 일정과 같은 결과");
            }
            return Optional.of(new Generated(windowStart, windowEnd, items, true, aiTitle(response.getTitle())));
        } catch (RuntimeException exception) {
            log.warn("AI 일정 생성 결과를 쓰지 않고 규칙 기반 추천으로 대체합니다: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    /** AI 에 넘길 맛집 후보 — 영업시간을 아는 곳 중 행사장에서 가까운 순 최대 15곳 (다시 추천에서는 지금 맛집 제외) */
    private Map<String, RestaurantView> aiRestaurantCandidates(RecommendEventView event, String foodPreference,
                                                               Set<String> avoidPlaces) {
        double lat = event.getMapy().doubleValue();
        double lng = event.getMapx().doubleValue();
        Map<String, RestaurantView> candidates = new LinkedHashMap<>();
        for (int radius : SEARCH_RADII) {
            double latDelta = radius / PlaceService.METERS_PER_DEGREE_LAT * PlaceService.BOX_MARGIN;
            double lngDelta = radius / (PlaceService.METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)))
                    * PlaceService.BOX_MARGIN;
            placeMapper.findRestaurantsNear(lat, lng, radius, lat - latDelta, lat + latDelta, lng - lngDelta, lng + lngDelta,
                            foodPreference, null, AI_CANDIDATE_LIMIT * 3, 0)
                    .stream()
                    .filter(restaurant -> restaurant.getOpenTime() != null && restaurant.getCloseTime() != null)
                    .filter(restaurant -> !avoidPlaces.contains(restaurant.getContentId()))
                    .limit(AI_CANDIDATE_LIMIT)
                    .forEach(restaurant -> candidates.putIfAbsent(restaurant.getContentId(), restaurant));
            if (candidates.size() >= AI_CANDIDATE_LIMIT / 2) {
                break;
            }
        }
        return candidates;
    }

    /** AI 결과를 일정 항목으로 바꾸면서 서버 규칙을 모두 확인한다. 하나라도 어기면 예외 */
    private static List<TripItem> toAiItems(AiPlanResponse response, RecommendEventView event, LocalTime eventStart,
                                            LocalTime eventEnd, Map<String, RestaurantView> restaurants, List<MealType> meals,
                                            LocalTime windowStart, LocalTime windowEnd) {
        List<PlanScheduleItem> schedule = response.getSchedule();
        if (schedule == null || schedule.isEmpty()) {
            throw new IllegalStateException("일정 없음");
        }
        List<TripItem> items = new ArrayList<>();
        Set<MealType> usedMeals = new HashSet<>();
        Set<String> usedPlaces = new HashSet<>();
        int eventCount = 0;

        for (PlanScheduleItem scheduled : schedule) {
            LocalTime start = LocalTime.parse(scheduled.getStartTime().trim());
            LocalTime end = LocalTime.parse(scheduled.getEndTime().trim());
            int duration = minutes(end) - minutes(start);
            String reason = aiReason(scheduled.getReason());

            if ("EVENT".equals(scheduled.getPlaceType())) {
                eventCount++;
                if (!event.getEventContentId().equals(scheduled.getPlaceContentId())) {
                    throw new IllegalStateException("선택 행사가 아닌 행사");
                }
                boolean fixed = event.getEventStartTime() != null;
                if (fixed && (!start.equals(eventStart) || end.isAfter(eventEnd))) {
                    throw new IllegalStateException("행사 고정 시간 변경 " + start + "~" + end);
                }
                if (duration < MIN_EVENT_DURATION || duration > MAX_EVENT_DURATION) {
                    throw new IllegalStateException("행사 소요시간 " + duration + "분");
                }
                items.add(TripItem.builder().itemType("EVENT").eventContentId(event.getEventContentId())
                        .startTime(start).durationMin(duration).aiReason(reason).timeFixYn(fixed).build());
            } else {
                RestaurantView restaurant = restaurants.get(scheduled.getPlaceContentId());
                if (restaurant == null || !usedPlaces.add(restaurant.getContentId())) {
                    throw new IllegalStateException("후보 밖이거나 두 번 넣은 식당 " + scheduled.getPlaceContentId());
                }
                MealType meal = mealOf(start);
                if (meal == null || !meals.contains(meal) || !usedMeals.add(meal)) {
                    throw new IllegalStateException("식사 시간대가 아님 " + start);
                }
                if (duration < MIN_MEAL_DURATION || duration > MAX_MEAL_DURATION
                        || !openDuring(restaurant.getOpenTime(), restaurant.getCloseTime(),
                        restaurant.getBreakOpenTime(), restaurant.getBreakCloseTime(), start, duration)) {
                    throw new IllegalStateException("영업시간·브레이크 밖 " + restaurant.getPlaceName() + " " + start);
                }
                items.add(TripItem.builder().itemType("PLACE").placeContentId(restaurant.getContentId())
                        .startTime(start).durationMin(duration).aiReason(reason).timeFixYn(false).build());
            }
        }
        if (eventCount != 1 || usedMeals.isEmpty()) {
            throw new IllegalStateException("행사 " + eventCount + "개, 식사 " + usedMeals.size() + "번");
        }

        items.sort(Comparator.comparing(TripItem::getStartTime));
        List<PlanItemRules.Candidate> candidates = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            TripItem item = items.get(index);
            candidates.add(new PlanItemRules.Candidate(item.getItemType(), contentIdOf(item),
                    "", item.getStartTime(), item.getDurationMin(), index + 1));
        }
        PlanItemRules.check(candidates, event.getEventContentId(), windowStart, windowEnd, true)
                .ifPresent(result -> {
                    throw new IllegalStateException("일정 규칙 위반: " + result.message());
                });
        return items;
    }

    /** AI 가 정한 식사 시작 시각이 점심(11:00~14:30)·저녁(17:00~20:30) 중 어디인지 */
    private static MealType mealOf(LocalTime start) {
        if (!start.isBefore(LocalTime.of(11, 0)) && !start.isAfter(LocalTime.of(14, 30))) {
            return MealType.LUNCH;
        }
        if (!start.isBefore(LocalTime.of(17, 0)) && !start.isAfter(LocalTime.of(20, 30))) {
            return MealType.DINNER;
        }
        return null;
    }

    /** 추천 이유는 DB 에 100자까지 저장된다 */
    private static String aiReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }

    /** AI-008 일정 제목 — 비어 있거나 100자를 넘으면 쓰지 않고 기본 제목을 쓴다 */
    private static String aiTitle(String title) {
        if (title == null || title.isBlank() || title.trim().length() > 100) {
            return null;
        }
        return title.trim();
    }

    private void insertItems(Long tripPlanId, List<TripItem> items) {
        for (int index = 0; index < items.size(); index++) {
            TripItem item = items.get(index);
            planDraftMapper.insertItem(TripItem.builder()
                    .tripPlanId(tripPlanId)
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
    }

    /** 식사 시간 후보 — 기본 시각, 행사 직전, 행사 직후 순 */
    private static List<LocalTime> mealTimes(List<MealType> meals, LocalTime eventStart, LocalTime eventEnd) {
        List<LocalTime> times = new ArrayList<>();
        for (MealType meal : meals) {
            times.add(meal.time);
            if (!eventStart.isBefore(LocalTime.of(1, 0))) {
                times.add(eventStart.minusMinutes(MEAL_DURATION));
            }
            times.add(eventEnd);
        }
        return times;
    }

    /**
     * 지금 초안의 맛집 중 지금 조건에도 그대로 맞는 곳(음식 종류·식사 60분 영업·지금 식사 시간대)만 모은다.
     * 이 맛집들은 피하고, 같은 식사 시각에서는 이보다 먼 곳을 고른다 (다시 추천).
     * 조건을 바꾼 직후(003 → 004)처럼 지금 맛집이 새 조건에 맞지 않으면 새 조건에서 가장 가까운 곳부터 고른다.
     */
    private static Current current(RecommendEventView event, List<PlanItemView> currentItems, String foodPreference,
                                   List<LocalTime> activeTimes) {
        Set<String> placeIds = new HashSet<>();
        Map<LocalTime, Double> distanceByTime = new HashMap<>();
        for (PlanItemView item : currentItems) {
            boolean fitsConditions = item.getPlaceContentId() != null
                    && activeTimes.contains(item.getStartTime())
                    && cuisineAllowed(foodPreference, item.getCuisineType())
                    && openDuringMeal(item.getOpenTime(), item.getCloseTime(),
                    item.getBreakOpenTime(), item.getBreakCloseTime(), item.getStartTime());
            if (!fitsConditions) {
                continue;
            }
            placeIds.add(item.getPlaceContentId());
            if (item.getMapx() != null && item.getMapy() != null) {
                distanceByTime.put(item.getStartTime(), distance(event.getMapy().doubleValue(), event.getMapx().doubleValue(),
                        item.getMapy().doubleValue(), item.getMapx().doubleValue()));
            }
        }
        return new Current(placeIds, distanceByTime);
    }

    /** 직선거리(m) — PlaceMapper.xml 과 같은 하버사인 공식, 지구 반지름 6371000m */
    static double distance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371000 * 2 * Math.asin(Math.sqrt(a));
    }

    /** 항목 구성(종류·대상·시작 시간·소요시간)이 지금 초안과 같은지 */
    private static boolean sameItems(List<PlanItemView> current, List<TripItem> generated) {
        Set<String> before = current.stream()
                .map(item -> key(item.getItemType(),
                        "EVENT".equals(item.getItemType()) ? item.getEventContentId() : item.getPlaceContentId(),
                        item.getStartTime(), item.getDurationMin()))
                .collect(Collectors.toSet());
        Set<String> after = generated.stream()
                .map(item -> key(item.getItemType(), contentIdOf(item), item.getStartTime(), item.getDurationMin()))
                .collect(Collectors.toSet());
        return before.equals(after);
    }

    private static String key(String type, String contentId, LocalTime start, Integer duration) {
        return type + ":" + contentId + "@" + format(start) + "/" + duration;
    }

    private static TripPlan copyConditions(TripPlan plan, LocalDate visitDate, LocalTime start, LocalTime end,
                                           String transportMode, String foodPreference, String mealType, boolean aiYn) {
        return TripPlan.builder()
                .tripPlanId(plan.getTripPlanId())
                .userId(plan.getUserId())
                .anchorContentId(plan.getAnchorContentId())
                .anchorEventName(plan.getAnchorEventName())
                .title(plan.getTitle())
                .tripDate(visitDate)
                .visitStartTime(start)
                .visitEndTime(end)
                .saveYn(plan.getSaveYn())
                .aiYn(aiYn)
                .transportMd(transportMode)
                .foodPreference(foodPreference)
                .mealType(mealType)
                .headcount(plan.getHeadcount())
                .build();
    }

    /**
     * 식사 시간 후보(기본 → 행사 직전 → 행사 직후) 중 방문 시간 안에서 다른 일정과 겹치지 않는 시간에
     * 영업 중인 맛집을 가까운 순으로 찾는다.
     */
    private Optional<TripItem> pickRestaurant(MealType meal, RecommendEventView event, String foodPreference,
                                              LocalTime windowStart, LocalTime windowEnd, List<Slot> occupied,
                                              Set<String> chosenPlaces, Current current) {
        for (LocalTime start : mealTimes(List.of(meal), occupied.get(0).start(), occupied.get(0).end())) {
            LocalTime end = start.plusMinutes(MEAL_DURATION);
            Slot slot = new Slot(start, end);
            if (start.isBefore(windowStart) || end.isAfter(windowEnd) || end.isBefore(start)
                    || occupied.stream().anyMatch(slot::overlaps)) {
                continue;
            }
            Optional<RestaurantView> found = findOpenRestaurant(event, foodPreference, start, chosenPlaces, current);
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

    /**
     * 다시 추천(AI-010)에서 누를 때마다 다른 조합이 나오도록 세 단계로 찾는다.
     * 1) 같은 식사 시간의 지금 맛집보다 먼 곳 중 가장 가까운 곳 (지금 맛집들은 제외)
     * 2) 더 먼 곳이 없으면 처음부터 다시 — 지금 맛집들만 빼고 가장 가까운 곳
     * 3) 그래도 없으면 지금 맛집까지 포함 (결과가 지금과 같으면 다시 추천에서 422)
     * 처음 만들 때(지금 맛집 없음)는 1)이 곧 가장 가까운 곳이다.
     */
    private Optional<RestaurantView> findOpenRestaurant(RecommendEventView event, String foodPreference,
                                                        LocalTime start, Set<String> chosenPlaces, Current current) {
        Double currentDistance = current.distanceByTime().get(start);
        Optional<RestaurantView> found = searchByRadius(event, foodPreference, start, chosenPlaces,
                current.placeIds(), currentDistance);
        if (found.isEmpty() && currentDistance != null) {
            found = searchByRadius(event, foodPreference, start, chosenPlaces, current.placeIds(), null);
        }
        if (found.isEmpty() && !current.placeIds().isEmpty()) {
            found = searchByRadius(event, foodPreference, start, chosenPlaces, Set.of(), null);
        }
        return found;
    }

    private Optional<RestaurantView> searchByRadius(RecommendEventView event, String foodPreference, LocalTime start,
                                                    Set<String> chosenPlaces, Set<String> avoidPlaces,
                                                    Double fartherThan) {
        double lat = event.getMapy().doubleValue();
        double lng = event.getMapx().doubleValue();
        for (int radius : SEARCH_RADII) {
            // PlaceService 와 같은 기준 — SQL 하버사인과 같은 구, 사각형이 원을 빠짐없이 덮도록 여유를 둔다
            double latDelta = radius / PlaceService.METERS_PER_DEGREE_LAT * PlaceService.BOX_MARGIN;
            double lngDelta = radius / (PlaceService.METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)))
                    * PlaceService.BOX_MARGIN;
            // 가까운 20곳이 모두 조건에서 빠져도 반경 안의 다음 후보까지 본다 (다시 추천을 여러 번 누른 경우)
            for (int offset = 0; ; offset += CANDIDATE_LIMIT) {
                List<RestaurantView> page = placeMapper.findRestaurantsNear(lat, lng, radius,
                        lat - latDelta, lat + latDelta, lng - lngDelta, lng + lngDelta,
                        foodPreference, start, CANDIDATE_LIMIT, offset);
                Optional<RestaurantView> found = page.stream()
                        .filter(restaurant -> !chosenPlaces.contains(restaurant.getContentId()))
                        .filter(restaurant -> !avoidPlaces.contains(restaurant.getContentId()))
                        .filter(restaurant -> fartherThan == null || restaurant.getDistance() > fartherThan + 0.01)
                        .filter(restaurant -> openDuringMeal(restaurant, start))
                        .findFirst();
                if (found.isPresent()) {
                    return found;
                }
                if (page.size() < CANDIDATE_LIMIT) {
                    break;
                }
            }
        }
        return Optional.empty();
    }

    /** ALL 은 추천용 음식 분류 4종(한식·중식·일식·양식) — PlaceMapper.xml 과 같은 기준 */
    private static boolean cuisineAllowed(String foodPreference, String cuisineType) {
        if (cuisineType == null) {
            return false;
        }
        return "ALL".equals(foodPreference) ? CUISINE_LABELS.containsKey(cuisineType) : cuisineType.equals(foodPreference);
    }

    /** 식사 60분 내내 영업하고 브레이크타임과 겹치지 않는지 (영업시간 모르면 제외 — FOOD-002) */
    static boolean openDuringMeal(RestaurantView restaurant, LocalTime start) {
        return openDuringMeal(restaurant.getOpenTime(), restaurant.getCloseTime(),
                restaurant.getBreakOpenTime(), restaurant.getBreakCloseTime(), start);
    }

    private static boolean openDuringMeal(LocalTime open, LocalTime close, LocalTime breakOpen, LocalTime breakClose,
                                          LocalTime start) {
        return openDuring(open, close, breakOpen, breakClose, start, MEAL_DURATION);
    }

    /** start 부터 duration 분 동안 영업하고 브레이크타임과 겹치지 않는지 */
    private static boolean openDuring(LocalTime open, LocalTime close, LocalTime breakOpen, LocalTime breakClose,
                                      LocalTime start, int duration) {
        if (open == null || close == null) {
            return false;
        }
        int openAt = minutes(open);
        int closeAt = minutes(close) <= openAt ? minutes(close) + DAY_MINUTES : minutes(close);
        // 식사를 그날 기준과, 전날 밤부터 이어진 영업(예: 17:00~02:00 가게의 00:30 식사) 기준 두 가지로 본다
        for (int shift : new int[] {0, DAY_MINUTES}) {
            int from = minutes(start) + shift;
            int to = from + duration;
            if (from >= openAt && to <= closeAt && !overlapsBreak(breakOpen, breakClose, from, to)) {
                return true;
            }
        }
        return false;
    }

    /** 브레이크타임이 자정을 넘어도(예: 23:00~00:30) 식사 구간과 겹치는지 본다 */
    private static boolean overlapsBreak(LocalTime breakOpen, LocalTime breakClose, int from, int to) {
        if (breakOpen == null || breakClose == null) {
            return false;
        }
        int breakFrom = minutes(breakOpen);
        int breakTo = minutes(breakClose) <= breakFrom ? minutes(breakClose) + DAY_MINUTES : minutes(breakClose);
        for (int shift : new int[] {-DAY_MINUTES, 0, DAY_MINUTES}) {
            if (from < breakTo + shift && breakFrom + shift < to) {
                return true;
            }
        }
        return false;
    }

    /**
     * 지난 날짜로는 일정을 만들지 않는다 — 이미 끝난 행사도 여기서 걸러진다 (행사일 불일치 400).
     * 오늘은 Asia/Seoul 기준.
     */
    private void checkNotPast(LocalDate visitDate) {
        LocalDate today = LocalDate.now(clock);
        if (visitDate.isBefore(today)) {
            throw new PlanRuleException(HttpStatus.BAD_REQUEST,
                    "지난 날짜(" + visitDate + ")로는 일정을 만들 수 없습니다. 오늘(" + today + ") 이후 날짜를 선택해 주세요.");
        }
    }

    /** 화면은 점심·저녁 모두를 BOTH 로 보낸다 — 값 없음과 같은 뜻이라 저장할 때는 null (docs/07 [제안]) */
    static String normalizeMealType(String mealType) {
        return mealType == null || "BOTH".equals(mealType) ? null : mealType;
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

    private static String contentIdOf(TripItem item) {
        return "EVENT".equals(item.getItemType()) ? item.getEventContentId() : item.getPlaceContentId();
    }

    private static String firstNonNull(String first, String second, String fallback) {
        return first != null ? first : (second != null ? second : fallback);
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

    /** aiUsed 면 AI 가 만든 일정(ai_yn = TRUE), title 은 AI 제목(없으면 null) */
    record Generated(LocalTime windowStart, LocalTime windowEnd, List<TripItem> items, boolean aiUsed, String title) {
    }

    /** 지금 초안의 맛집 id 와 식사 시작 시각별 거리 */
    record Current(Set<String> placeIds, Map<LocalTime, Double> distanceByTime) {
    }

    private record Slot(LocalTime start, LocalTime end) {
        boolean overlaps(Slot other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }
    }
}
