package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tripai.backend.domain.dto.DraftConditionsRequest;
import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.PlanItemResponse;
import com.tripai.backend.domain.dto.PlanRecommendRequest;
import com.tripai.backend.domain.entity.RecommendEventView;
import com.tripai.backend.domain.entity.RestaurantView;
import com.tripai.backend.domain.entity.TripPlan;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 끼니별 음식 종류(점심·저녁)와 카페 포함 — 메인 AI 추천 모달·조건 수정 팝업 조건 (docs/07 [제안]) */
class PlanRecommendFoodCafeTest {

    private static final LocalDate VISIT_DATE = LocalDate.of(2026, 10, 3);

    private FakePlanDraftMapper mapper;
    private PlanRecommendServiceTest.FakePlaceMapper places;
    private PlanDraftService draftService;
    private PlanRecommendService service;

    @BeforeEach
    void setUp() {
        mapper = new FakePlanDraftMapper();
        places = new PlanRecommendServiceTest.FakePlaceMapper();
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 9, 28, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
        draftService = new PlanDraftService(mapper, clock);
        service = new PlanRecommendService(mapper, places, draftService, clock);
        mapper.recommendEvents.put("EV-1", RecommendEventView.builder()
                .eventContentId("EV-1").eventName("고궁의 밤").displayYn(true)
                .eventStartDate(LocalDate.of(2026, 10, 1)).eventEndDate(LocalDate.of(2026, 10, 10))
                .eventStartTime(LocalTime.of(18, 0)).eventEndTime(LocalTime.of(21, 0))
                .mapx(new BigDecimal("126.9770000")).mapy(new BigDecimal("37.5796000")).build());
        add(place("KR-1", "KOREAN", 300, "10:00", "22:00", null, null));
        add(place("KR-2", "KOREAN", 350, "10:00", "22:00", null, null));
        add(place("WS-1", "WESTERN", 400, "10:00", "22:00", null, null));
        add(place("WS-2", "WESTERN", 450, "10:00", "22:00", null, null));
    }

    @Test
    void 점심과_저녁에_다른_음식_종류를_고르면_끼니마다_그_종류로_찾는다() {
        DraftResponse draft = service.recommend(1L, request("KOREAN", "WESTERN", null, null, null));

        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("KR-1", "WS-1", "EV-1");
        assertThat(draft.items()).extracting(PlanItemResponse::startTime).containsExactly("12:30", "17:00", "18:00");
        assertThat(draft.conditions().lunchFoodPreference()).isEqualTo("KOREAN");
        assertThat(draft.conditions().dinnerFoodPreference()).isEqualTo("WESTERN");
        // 두 끼가 다르면 예전 단일 값(food_preference)은 비워 둔다
        assertThat(draft.conditions().foodPreference()).isNull();
        assertThat(draft.conditions().includeCafe()).isFalse();
        TripPlan saved = mapper.plans.get(draft.draftId());
        assertThat(saved.getLunchFoodPreference()).isEqualTo("KOREAN");
        assertThat(saved.getDinnerFoodPreference()).isEqualTo("WESTERN");
    }

    @Test
    void foodPreference_만_보내면_두_끼_모두_그_종류로_찾는다() {
        // 행사 상세(#45)처럼 기존 방식으로 부르는 경우
        DraftResponse draft = service.recommend(1L, new PlanRecommendRequest(
                "EV-1", VISIT_DATE, null, null, null, "WESTERN", null, "WALK_TRANSIT", null));

        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("WS-1", "WS-2", "EV-1");
        assertThat(draft.conditions().foodPreference()).isEqualTo("WESTERN");
        assertThat(draft.conditions().lunchFoodPreference()).isEqualTo("WESTERN");
        assertThat(draft.conditions().dinnerFoodPreference()).isEqualTo("WESTERN");
    }

    @Test
    void 끼니별_값이_있으면_foodPreference_보다_먼저_쓴다() {
        DraftResponse draft = service.recommend(1L, new PlanRecommendRequest(
                "EV-1", VISIT_DATE, null, null, null, "WESTERN", null, "WALK_TRANSIT", null, "KOREAN", null, null));

        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("KR-1", "WS-1", "EV-1");
    }

    @Test
    void 카페를_넣으면_식사_시간대가_아닌_빈_시간에_가까운_카페_1곳을_넣는다() {
        add(place("CF-1", "CAFE", 200, "10:00", "22:00", null, null));
        add(place("CF-2", "CAFE", 250, "10:00", "22:00", null, null));

        DraftResponse draft = service.recommend(1L, request(null, null, null, null, true));

        // 점심 12:30 → (13:30·14:30 은 점심 시간대, 17:00 은 저녁 시간대라 제외) → 저녁 바로 앞 16:00
        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("KR-1", "CF-1", "KR-2", "EV-1");
        assertThat(draft.items()).extracting(PlanItemResponse::startTime).containsExactly("12:30", "16:00", "17:00", "18:00");
        assertThat(draft.items().get(1).durationMin()).isEqualTo(60);
        assertThat(draft.recommendationReasons().get(1).reason()).isEqualTo("카페 16:00 · 행사장에서 약 200m · 영업 10:00~22:00");
        assertThat(draft.conditions().includeCafe()).isTrue();
        assertThat(mapper.plans.get(draft.draftId()).getCafeYn()).isTrue();
    }

    @Test
    void 카페를_넣지_않으면_카페는_식사로도_추천하지_않는다() {
        add(place("CF-1", "CAFE", 100, "10:00", "22:00", null, null));

        DraftResponse draft = service.recommend(1L, request(null, null, null, null, false));

        assertThat(draft.items()).extracting(PlanItemResponse::placeId).doesNotContain("CF-1");
    }

    @Test
    void 카페_브레이크타임과_겹치는_시각은_피한다() {
        // 16:00~17:00 은 브레이크(15:30~17:00)와 겹친다 → 일정과 가까운 다른 빈 시간 중 영업하는 시각
        add(place("CF-1", "CAFE", 200, "10:00", "22:00", "15:30", "17:00"));

        DraftResponse draft = service.recommend(1L, request(null, null, null, null, true));

        PlanItemResponse cafe = draft.items().stream().filter(item -> item.placeId().equals("CF-1")).findFirst().orElseThrow();
        assertThat(cafe.startTime()).isEqualTo("10:30");
        assertThat(draft.items()).extracting(PlanItemResponse::startTime).containsExactly("10:30", "12:30", "17:00", "18:00");
    }

    @Test
    void 카페를_넣을_빈_시간이_없으면_카페_없이_만든다() {
        add(place("CF-1", "CAFE", 200, "10:00", "22:00", null, null));

        // 방문 16:30~ → 점심 불가, 저녁 17:00 과 행사 18:00 사이에 60분 빈 시간이 없다
        DraftResponse draft = service.recommend(1L, new PlanRecommendRequest(
                "EV-1", VISIT_DATE, "16:30", null, null, null, null, "WALK_TRANSIT", null, null, null, true));

        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("KR-1", "EV-1");
        assertThat(draft.conditions().includeCafe()).isTrue();
    }

    @Test
    void 조건_수정에서_끼니별_음식_종류와_카페를_바꾼다() {
        add(place("CF-1", "CAFE", 200, "10:00", "22:00", null, null));
        Long draftId = service.recommend(1L, request("KOREAN", "KOREAN", null, null, false)).draftId();

        // 저녁만 양식으로 바꾸고 카페를 넣는다 — 점심 음식 종류는 그대로
        service.updateConditions(1L, draftId, new DraftConditionsRequest(
                null, null, null, null, null, null, null, null, "WESTERN", true));
        DraftResponse draft = draftService.getDraft(1L, draftId);
        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("KR-1", "CF-1", "WS-1", "EV-1");
        assertThat(draft.conditions().lunchFoodPreference()).isEqualTo("KOREAN");
        assertThat(draft.conditions().dinnerFoodPreference()).isEqualTo("WESTERN");
        assertThat(draft.conditions().includeCafe()).isTrue();

        // foodPreference 만 보내면 두 끼 모두 바뀐다
        service.updateConditions(1L, draftId, new DraftConditionsRequest(
                null, null, null, null, "WESTERN", null, null));
        draft = draftService.getDraft(1L, draftId);
        assertThat(draft.conditions().lunchFoodPreference()).isEqualTo("WESTERN");
        assertThat(draft.conditions().dinnerFoodPreference()).isEqualTo("WESTERN");
        assertThat(draft.conditions().includeCafe()).isTrue();
    }

    @Test
    void 다시_추천은_저장한_끼니별_음식_종류를_지키고_다른_카페를_먼저_고른다() {
        add(place("CF-1", "CAFE", 200, "10:00", "22:00", null, null));
        add(place("CF-2", "CAFE", 250, "10:00", "22:00", null, null));
        Long draftId = service.recommend(1L, request("KOREAN", "WESTERN", null, null, true)).draftId();

        DraftResponse again = service.regenerate(1L, draftId);

        assertThat(again.items()).extracting(PlanItemResponse::placeId).containsExactly("KR-2", "CF-2", "WS-2", "EV-1");
    }

    @Test
    void 끼니별_값이_없던_기존_초안은_foodPreference_를_두_끼_모두에_쓴다() {
        Long draftId = service.recommend(1L, new PlanRecommendRequest(
                "EV-1", VISIT_DATE, null, null, null, "WESTERN", null, "WALK_TRANSIT", null)).draftId();
        // 04 마이그레이션 전 행처럼 끼니별 값과 카페 값을 비운다
        TripPlan saved = mapper.plans.get(draftId);
        mapper.plans.put(draftId, TripPlan.builder()
                .tripPlanId(draftId).userId(saved.getUserId()).anchorContentId(saved.getAnchorContentId())
                .anchorEventName(saved.getAnchorEventName()).title(saved.getTitle()).tripDate(saved.getTripDate())
                .visitStartTime(saved.getVisitStartTime()).visitEndTime(saved.getVisitEndTime())
                .saveYn(false).aiYn(false).transportMd(saved.getTransportMd())
                .foodPreference("WESTERN").headcount(1).build());

        DraftResponse draft = draftService.getDraft(1L, draftId);
        assertThat(draft.conditions().lunchFoodPreference()).isEqualTo("WESTERN");
        assertThat(draft.conditions().dinnerFoodPreference()).isEqualTo("WESTERN");
        assertThat(draft.conditions().includeCafe()).isFalse();

        add(place("WS-3", "WESTERN", 500, "10:00", "22:00", null, null));
        DraftResponse again = service.regenerate(1L, draftId);
        // 두 끼 모두 양식 안에서 지금과 다른 조합 (점심은 지금보다 먼 WS-3, 저녁은 남은 곳이 없어 WS-1 — 004 규칙)
        assertThat(again.items()).extracting(PlanItemResponse::placeId).containsExactly("WS-3", "WS-1", "EV-1");
    }

    @Test
    void 카페_시각_후보는_식사_시간대와_다른_일정을_피하고_가까운_순이다() {
        List<PlanRecommendService.Slot> occupied = List.of(
                new PlanRecommendService.Slot(LocalTime.of(12, 30), LocalTime.of(13, 30)),
                new PlanRecommendService.Slot(LocalTime.of(18, 0), LocalTime.of(21, 0)));

        List<LocalTime> times = PlanRecommendService.cafeTimes(LocalTime.of(10, 0), LocalTime.of(21, 0), occupied);

        // 저녁이 없는 일정 — 행사(18:00) 바로 앞 17:00 은 저녁 시간대라 빠지고, 행사와 30분 떨어진 16:30 이 가장 가깝다
        assertThat(times).first().isEqualTo(LocalTime.of(16, 30));
        assertThat(times).allMatch(time -> time.isBefore(LocalTime.of(11, 0))
                || (time.isAfter(LocalTime.of(14, 30)) && time.isBefore(LocalTime.of(17, 0))));
        assertThat(times).doesNotContain(LocalTime.of(17, 0), LocalTime.of(13, 30));
    }

    private static PlanRecommendRequest request(String lunch, String dinner, String start, String end, Boolean cafe) {
        return new PlanRecommendRequest("EV-1", VISIT_DATE, start, end, null, null, null, "WALK_TRANSIT", null,
                lunch, dinner, cafe);
    }

    /** 후보로 넣고, 초안 조회에도 음식 종류(카페는 CAFE)·영업시간이 나오도록 등록한다 (실제 DB 조회와 같게) */
    private void add(RestaurantView restaurant) {
        places.restaurants.add(restaurant);
        mapper.displays.put(restaurant.getContentId(), new FakePlanDraftMapper.Display(restaurant.getContentId(), null,
                null, null, restaurant.getOpenTime(), restaurant.getCloseTime(),
                restaurant.getBreakOpenTime(), restaurant.getBreakCloseTime()));
        mapper.cuisines.put(restaurant.getContentId(), restaurant.getCuisineType());
    }

    private static RestaurantView place(String id, String cuisine, double distance, String open, String close,
                                        String breakOpen, String breakClose) {
        return RestaurantView.builder().contentId(id).placeName(id).cuisineType(cuisine).distance(distance)
                .openTime(time(open)).closeTime(time(close)).breakOpenTime(time(breakOpen)).breakCloseTime(time(breakClose))
                .build();
    }

    private static LocalTime time(String value) {
        return value == null ? null : LocalTime.parse(value);
    }
}
