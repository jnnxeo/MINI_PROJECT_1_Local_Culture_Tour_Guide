package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.PlanItemResponse;
import com.tripai.backend.domain.dto.PlanRecommendRequest;
import com.tripai.backend.domain.entity.EventLocationView;
import com.tripai.backend.domain.entity.RecommendEventView;
import com.tripai.backend.domain.entity.RestaurantView;
import com.tripai.backend.repository.PlaceMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PlanRecommendServiceTest {

    private static final LocalDate VISIT_DATE = LocalDate.of(2026, 10, 3);
    private static final Set<String> RECOMMENDABLE = Set.of("KOREAN", "WESTERN", "JAPANESE", "CHINESE");

    /** SQL 조건(반경·음식 종류·식사 시작 시각 영업 여부)을 흉내 내는 가짜 Mapper */
    static class FakePlaceMapper implements PlaceMapper {
        final List<RestaurantView> restaurants = new ArrayList<>();
        final List<Integer> radii = new ArrayList<>();
        final List<String> cuisines = new ArrayList<>();

        @Override
        public Optional<EventLocationView> findEventLocation(String eventContentId) {
            return Optional.empty();
        }

        @Override
        public List<RestaurantView> findRestaurantsNear(double lat, double lng, int radius, double minLat, double maxLat,
                                                        double minLng, double maxLng, String cuisineType,
                                                        LocalTime mealTime, int limit, int offset) {
            radii.add(radius);
            cuisines.add(cuisineType);
            return restaurants.stream()
                    .filter(r -> r.getDistance() <= radius)
                    .filter(r -> "ALL".equals(cuisineType) ? RECOMMENDABLE.contains(r.getCuisineType())
                            : r.getCuisineType().equals(cuisineType))
                    .filter(r -> r.getOpenTime() != null && r.getCloseTime() != null
                            && !mealTime.isBefore(r.getOpenTime()) && mealTime.isBefore(r.getCloseTime()))
                    .sorted(Comparator.comparing(RestaurantView::getDistance))
                    .limit(limit)
                    .toList();
        }
    }

    private FakePlanDraftMapper mapper;
    private FakePlaceMapper places;
    private PlanRecommendService service;

    @BeforeEach
    void setUp() {
        mapper = new FakePlanDraftMapper();
        places = new FakePlaceMapper();
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 9, 27, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
        service = new PlanRecommendService(mapper, places, new PlanDraftService(mapper, clock), clock);
        mapper.recommendEvents.put("EV-1", event("EV-1", LocalTime.of(18, 0), LocalTime.of(21, 0), true));
    }

    @Test
    void 점심_저녁_맛집과_행사로_초안을_만든다() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", "15:00", "17:00"));
        places.restaurants.add(restaurant("PL-2", "KOREAN", 560, "10:30", "20:00", null, null));

        DraftResponse draft = service.recommend(1L, request(null, null, null, null));

        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("PL-1", "PL-2", "EV-1");
        assertThat(draft.items()).extracting(PlanItemResponse::startTime).containsExactly("12:30", "17:00", "18:00");
        assertThat(draft.items()).extracting(PlanItemResponse::sequence).containsExactly(1, 2, 3);
        assertThat(draft.items().get(2).durationMin()).isEqualTo(180);
        assertThat(draft.items().get(2).timeFixed()).isTrue();
        assertThat(draft.title()).isEqualTo("10월 3일 고궁의 밤");
        assertThat(draft.tripType()).isEqualTo("DAY_TRIP");
        assertThat(draft.selectedEvent().eventId()).isEqualTo("EV-1");
        assertThat(draft.recommendationReasons()).extracting(r -> r.reason()).containsExactly(
                "점심 12:30 · 행사장에서 약 550m · 한식 · 영업 11:00~21:00",
                "저녁 17:00 · 행사장에서 약 560m · 한식 · 영업 10:30~20:00",
                "선택한 행사 · 10월 3일 진행 · 18:00 시작");
        assertThat(draft.conditions().startTime()).isEqualTo("10:00");
        assertThat(draft.conditions().endTime()).isEqualTo("21:00");
        assertThat(draft.conditions().foodPreference()).isEqualTo("ALL");
        assertThat(draft.conditions().mealType()).isNull();
        assertThat(mapper.plans.get(draft.draftId()).getSaveYn()).isFalse();
        assertThat(mapper.plans.get(draft.draftId()).getHeadcount()).isEqualTo(1);
    }

    @Test
    void 식사_시간을_고르면_그_시간만_찾는다() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", null, null));
        places.restaurants.add(restaurant("PL-2", "KOREAN", 560, "10:30", "20:00", null, null));

        DraftResponse draft = service.recommend(1L, request("KOREAN", "DINNER", null, null));

        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("PL-1", "EV-1");
        assertThat(draft.items().get(0).startTime()).isEqualTo("17:00");
        assertThat(draft.conditions().foodPreference()).isEqualTo("KOREAN");
        assertThat(draft.conditions().mealType()).isEqualTo("DINNER");
    }

    @Test
    void 식사_60분_안에_문을_닫거나_브레이크가_겹치면_건너뛴다() {
        places.restaurants.add(restaurant("CLOSE", "KOREAN", 300, "11:00", "13:00", null, null));
        places.restaurants.add(restaurant("BREAK", "KOREAN", 400, "11:00", "21:00", "13:00", "14:00"));
        places.restaurants.add(restaurant("OK", "KOREAN", 600, "11:00", "21:00", null, null));

        DraftResponse draft = service.recommend(1L, request(null, "LUNCH", null, null));

        assertThat(draft.items().get(0).placeId()).isEqualTo("OK");
    }

    @Test
    void 영업시간을_모르는_맛집은_추천하지_않는다() {
        places.restaurants.add(restaurant("UNKNOWN", "KOREAN", 300, null, null, null, null));

        assertStatus(() -> service.recommend(1L, request(null, null, null, null)), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void 음식_종류를_고르면_그_종류만_찾는다() {
        places.restaurants.add(restaurant("KO", "KOREAN", 300, "11:00", "21:00", null, null));
        places.restaurants.add(restaurant("CN", "CHINESE", 800, "11:00", "21:00", null, null));

        DraftResponse draft = service.recommend(1L, request("CHINESE", "LUNCH", null, null));

        assertThat(draft.items().get(0).placeId()).isEqualTo("CN");
        assertThat(places.cuisines).containsOnly("CHINESE");
    }

    @Test
    void 가까운_곳이_없으면_반경을_넓혀_찾는다() {
        places.restaurants.add(restaurant("FAR", "KOREAN", 2500, "11:00", "21:00", null, null));

        DraftResponse draft = service.recommend(1L, request(null, "LUNCH", null, null));

        assertThat(draft.items().get(0).placeId()).isEqualTo("FAR");
        assertThat(places.radii).containsExactly(1500, 3000);
        assertThat(draft.recommendationReasons().get(0).reason()).contains("약 2500m");
    }

    @Test
    void 식사_시간이_행사와_겹치면_행사_직전으로_옮긴다() {
        mapper.recommendEvents.put("EV-2", event("EV-2", LocalTime.of(12, 0), LocalTime.of(14, 0), true));
        places.restaurants.add(restaurant("PL-1", "KOREAN", 300, "10:00", "21:00", null, null));

        DraftResponse draft = service.recommend(1L, new PlanRecommendRequest(
                "EV-2", VISIT_DATE, null, null, null, null, "LUNCH", null, null));

        assertThat(draft.items()).extracting(PlanItemResponse::startTime).containsExactly("11:00", "12:00");
    }

    @Test
    void 같은_맛집을_두_번_넣지_않는다() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 300, "10:00", "22:00", null, null));

        DraftResponse draft = service.recommend(1L, request(null, null, null, null));

        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("PL-1", "EV-1");
    }

    @Test
    void 맛집_후보가_없으면_422_초안을_만들지_않는다() {
        assertStatus(() -> service.recommend(1L, request(null, null, null, null)), HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(mapper.plans).isEmpty();
        assertThat(mapper.rows).isEmpty();
    }

    @Test
    void 여행_날짜에_진행하지_않는_행사면_400() {
        assertStatus(() -> service.recommend(1L, new PlanRecommendRequest(
                "EV-1", LocalDate.of(2026, 11, 1), null, null, null, null, null, null, null)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void 지난_날짜나_이미_끝난_행사면_400() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 300, "10:00", "22:00", null, null));
        RecommendEventView ended = RecommendEventView.builder()
                .eventContentId("EV-OLD").eventName("작년 축제").displayYn(true)
                .eventStartDate(LocalDate.of(2025, 10, 1)).eventEndDate(LocalDate.of(2025, 10, 10))
                .eventStartTime(LocalTime.of(18, 0)).eventEndTime(LocalTime.of(21, 0))
                .mapx(new BigDecimal("126.9770000")).mapy(new BigDecimal("37.5796000"))
                .build();
        mapper.recommendEvents.put("EV-OLD", ended);

        // 오늘(테스트 기준 2026-09-27) 이전 날짜 — 행사 기간 안이어도 막는다
        mapper.recommendEvents.put("EV-LONG", RecommendEventView.builder()
                .eventContentId("EV-LONG").eventName("상설 전시").displayYn(true)
                .eventStartDate(LocalDate.of(2026, 9, 1)).eventEndDate(LocalDate.of(2026, 12, 31))
                .eventStartTime(LocalTime.of(18, 0)).eventEndTime(LocalTime.of(21, 0))
                .mapx(new BigDecimal("126.9770000")).mapy(new BigDecimal("37.5796000"))
                .build());

        assertStatus(() -> service.recommend(1L, new PlanRecommendRequest(
                "EV-OLD", LocalDate.of(2025, 10, 3), null, null, null, null, null, null, null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.recommend(1L, new PlanRecommendRequest(
                "EV-LONG", LocalDate.of(2026, 9, 26), null, null, null, null, null, null, null)), HttpStatus.BAD_REQUEST);
        assertThat(service.recommend(1L, new PlanRecommendRequest(
                "EV-LONG", LocalDate.of(2026, 9, 27), null, null, null, null, "LUNCH", null, null)).draftId()).isNotNull();
        assertThat(mapper.plans).hasSize(1);
    }

    @Test
    void 식사_시간_BOTH_는_점심_저녁_모두로_보고_저장은_null() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", null, null));
        places.restaurants.add(restaurant("PL-2", "KOREAN", 560, "10:30", "20:00", null, null));

        DraftResponse draft = service.recommend(1L, request(null, "BOTH", null, null));

        assertThat(draft.items()).extracting(PlanItemResponse::startTime).containsExactly("12:30", "17:00", "18:00");
        assertThat(draft.conditions().mealType()).isNull();
    }

    @Test
    void 없거나_비표출_행사면_404() {
        mapper.recommendEvents.put("HIDDEN", event("HIDDEN", LocalTime.of(18, 0), LocalTime.of(21, 0), false));

        assertStatus(() -> service.recommend(1L, new PlanRecommendRequest(
                "NONE", VISIT_DATE, null, null, null, null, null, null, null)), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.recommend(1L, new PlanRecommendRequest(
                "HIDDEN", VISIT_DATE, null, null, null, null, null, null, null)), HttpStatus.NOT_FOUND);
    }

    @Test
    void 방문_시간_안에_행사가_안_들어가면_400() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 300, "10:00", "22:00", null, null));

        assertStatus(() -> service.recommend(1L, request(null, null, "10:00", "17:00")), HttpStatus.BAD_REQUEST);
    }

    @Test
    void 자정을_넘겨_영업하는_곳도_식사_시간을_판단한다() {
        RestaurantView night = restaurant("NIGHT", "KOREAN", 300, "17:00", "01:00", null, null);

        assertThat(PlanRecommendService.openDuringMeal(night, LocalTime.of(17, 0))).isTrue();
        assertThat(PlanRecommendService.openDuringMeal(night, LocalTime.of(12, 30))).isFalse();
    }

    private static PlanRecommendRequest request(String food, String meal, String start, String end) {
        return new PlanRecommendRequest("EV-1", VISIT_DATE, start, end, null, food, meal, "WALK_TRANSIT", null);
    }

    private static RecommendEventView event(String id, LocalTime start, LocalTime end, boolean display) {
        return RecommendEventView.builder()
                .eventContentId(id).eventName("고궁의 밤").displayYn(display)
                .eventStartDate(LocalDate.of(2026, 10, 1)).eventEndDate(LocalDate.of(2026, 10, 10))
                .eventStartTime(start).eventEndTime(end)
                .mapx(new BigDecimal("126.9770000")).mapy(new BigDecimal("37.5796000"))
                .build();
    }

    private static RestaurantView restaurant(String id, String cuisine, double distance, String open, String close,
                                             String breakOpen, String breakClose) {
        return RestaurantView.builder()
                .contentId(id).placeName(id).cuisineType(cuisine).distance(distance)
                .openTime(time(open)).closeTime(time(close))
                .breakOpenTime(time(breakOpen)).breakCloseTime(time(breakClose))
                .build();
    }

    private static LocalTime time(String value) {
        return value == null ? null : LocalTime.parse(value);
    }

    private static void assertStatus(Runnable call, HttpStatus expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(PlanRuleException.class)
                .satisfies(e -> assertThat(((PlanRuleException) e).getStatus()).isEqualTo(expected));
    }
}
