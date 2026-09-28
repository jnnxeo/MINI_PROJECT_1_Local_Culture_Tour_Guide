package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tripai.backend.domain.dto.DraftConditionsRequest;
import com.tripai.backend.domain.dto.DraftConditionsResponse;
import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.PlanItemResponse;
import com.tripai.backend.domain.dto.PlanRecommendRequest;
import com.tripai.backend.domain.entity.EventLocationView;
import com.tripai.backend.domain.entity.RecommendEventView;
import com.tripai.backend.domain.entity.RestaurantView;
import com.tripai.backend.domain.entity.TripPlan;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;
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
                    .filter(r -> mealTime == null || (r.getOpenTime() != null && r.getCloseTime() != null
                            && !mealTime.isBefore(r.getOpenTime()) && mealTime.isBefore(r.getCloseTime())))
                    .sorted(Comparator.comparing(RestaurantView::getDistance))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }
    }

    private FakePlanDraftMapper mapper;
    private FakePlaceMapper places;
    private PlanRecommendService service;
    private PlanDraftService draftService;

    @BeforeEach
    void setUp() {
        mapper = new FakePlanDraftMapper();
        places = new FakePlaceMapper();
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 9, 27, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
        draftService = new PlanDraftService(mapper, clock);
        service = new PlanRecommendService(mapper, places, draftService, clock);
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
    void 가까운_20곳이_모두_안_맞아도_다음_후보까지_찾는다() {
        for (int index = 1; index <= 21; index++) {
            // 12:30 에는 열려 있지만 13:00 에 닫아 60분 식사가 안 되는 곳
            places.restaurants.add(restaurant("SHORT-" + index, "KOREAN", index * 50, "11:00", "13:00", null, null));
        }
        places.restaurants.add(restaurant("OK", "KOREAN", 1200, "11:00", "21:00", null, null));

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

    // ---- API-PLAN-004 다시 추천 ----

    @Test
    void 다시_추천하면_지금과_다른_맛집으로_바꾸고_제목은_유지한다() {
        addRestaurant(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", "15:00", "17:00"));
        addRestaurant(restaurant("PL-2", "KOREAN", 560, "10:30", "20:00", null, null));
        addRestaurant(restaurant("PL-3", "KOREAN", 700, "11:00", "22:00", null, null));
        addRestaurant(restaurant("PL-4", "WESTERN", 800, "11:00", "22:00", null, null));
        Long draftId = service.recommend(1L, request(null, null, null, null)).draftId();
        mapper.titles.put(draftId, "내가 바꾼 제목");

        DraftResponse again = service.regenerate(1L, draftId);

        assertThat(again.items()).extracting(PlanItemResponse::placeId).containsExactly("PL-3", "PL-4", "EV-1");
        assertThat(again.items()).extracting(PlanItemResponse::sequence).containsExactly(1, 2, 3);
        assertThat(again.title()).isEqualTo("내가 바꾼 제목");
        assertThat(again.recommendationReasons()).hasSize(3);
        assertThat(mapper.rows.values()).allMatch(row -> row.getTripPlanId().equals(draftId));
        assertThat(mapper.rows).hasSize(3);
    }

    @Test
    void 다시_추천을_누를_때마다_한_단계씩_먼_맛집으로_바뀌고_끝나면_처음으로_돌아온다() {
        for (int index = 1; index <= 6; index++) {
            addNearby("PL-" + index, 200 + index * 100);
        }
        Long draftId = service.recommend(1L, request(null, null, null, null)).draftId();
        assertThat(placeIds(draftId)).containsExactly("PL-1", "PL-2");

        service.regenerate(1L, draftId);
        assertThat(placeIds(draftId)).containsExactly("PL-3", "PL-4");

        service.regenerate(1L, draftId);
        assertThat(placeIds(draftId)).containsExactly("PL-5", "PL-6");

        service.regenerate(1L, draftId);
        assertThat(placeIds(draftId)).containsExactly("PL-1", "PL-2");
    }

    @Test
    void 대체할_맛집이_일부만_있으면_그만큼만_바꾼다() {
        addRestaurant(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", "15:00", "17:00"));
        addRestaurant(restaurant("PL-2", "KOREAN", 560, "10:30", "20:00", null, null));
        addRestaurant(restaurant("PL-3", "KOREAN", 700, "11:00", "22:00", null, null));
        Long draftId = service.recommend(1L, request(null, null, null, null)).draftId();

        DraftResponse again = service.regenerate(1L, draftId);

        assertThat(again.items()).extracting(PlanItemResponse::placeId).containsExactly("PL-3", "PL-1", "EV-1");
    }

    @Test
    void 다른_후보가_없으면_422_기존_초안을_유지한다() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", "15:00", "17:00"));
        places.restaurants.add(restaurant("PL-2", "KOREAN", 560, "10:30", "20:00", null, null));
        Long draftId = service.recommend(1L, request(null, null, null, null)).draftId();
        List<Long> before = List.copyOf(mapper.rows.keySet());

        assertStatus(() -> service.regenerate(1L, draftId), HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(mapper.rows.keySet()).containsExactlyElementsOf(before);
    }

    @Test
    void 다른_사람_초안은_403_저장된_초안은_404() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", null, null));
        Long draftId = service.recommend(1L, request(null, "LUNCH", null, null)).draftId();

        assertErrorCode(() -> service.regenerate(2L, draftId), ErrorCode.FORBIDDEN);
        assertErrorCode(() -> service.updateConditions(2L, draftId, conditions(null, null, null, "WALK")), ErrorCode.FORBIDDEN);
        mapper.markSaved(draftId);
        assertErrorCode(() -> service.regenerate(1L, draftId), ErrorCode.PLAN_NOT_FOUND);
    }

    // ---- API-PLAN-003 조건 수정 ----

    @Test
    void 조건을_바꾸면_조건과_일정을_한번에_바꾼다() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", null, null));
        places.restaurants.add(restaurant("CN", "CHINESE", 900, "11:00", "21:00", null, null));
        Long draftId = service.recommend(1L, request(null, null, null, null)).draftId();

        DraftConditionsResponse changed = service.updateConditions(1L, draftId, conditions(null, "CHINESE", "LUNCH", null));

        assertThat(changed.draftId()).isEqualTo(draftId);
        assertThat(changed.conditions().foodPreference()).isEqualTo("CHINESE");
        assertThat(changed.conditions().mealType()).isEqualTo("LUNCH");
        assertThat(changed.conditions().transportMode()).isEqualTo("WALK_TRANSIT");
        assertThat(changed.conditions().startTime()).isEqualTo("10:00");
        // 004 없이 003 만으로 새 조건의 일정이 들어가 있다 (화면은 003 → 002)
        DraftResponse draft = draftService.getDraft(1L, draftId);
        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("CN", "EV-1");
        assertThat(draft.recommendationReasons()).extracting(r -> r.reason()).first().asString().startsWith("점심 12:30 · 행사장에서 약 900m · 중식");
    }

    @Test
    void 조건_수정은_지금_맛집과_상관없이_새_조건에서_가장_가까운_곳부터_고른다() {
        addNearby("KO-1", "KOREAN", 300);
        addNearby("KO-2", "KOREAN", 400);
        addNearby("CN-1", "CHINESE", 350);
        addNearby("CN-2", "CHINESE", 900);
        Long draftId = service.recommend(1L, request("KOREAN", "LUNCH", null, null)).draftId();
        service.regenerate(1L, draftId);
        assertThat(placeIds(draftId)).containsExactly("KO-2");

        service.updateConditions(1L, draftId, conditions(null, "CHINESE", null, null));

        assertThat(placeIds(draftId)).containsExactly("CN-1");
    }

    @Test
    void 식사_시간을_바꾸면_새_식사_시간에_가장_가까운_곳으로_바로_바꾼다() {
        addNearby("PL-1", 300);
        addNearby("PL-2", 400);
        Long draftId = service.recommend(1L, request(null, "LUNCH", null, null)).draftId();
        assertThat(placeIds(draftId)).containsExactly("PL-1");

        service.updateConditions(1L, draftId, conditions(null, null, "DINNER", null));

        DraftResponse draft = draftService.getDraft(1L, draftId);
        assertThat(placeIds(draftId)).containsExactly("PL-1");
        assertThat(draft.items().get(0).startTime()).isEqualTo("17:00");
    }

    @Test
    void 조건_수정_뒤_다시_추천이_실패해도_조건과_일정이_어긋나지_않는다() {
        // GPT 검토 E5: 맛집 1곳뿐인데 날짜만 바꾼 경우 — 예전에는 003 이 날짜만 바꾸고 004 가 422 로 끝나 둘이 어긋났다
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", null, null));
        Long draftId = service.recommend(1L, request(null, "LUNCH", null, null)).draftId();

        service.updateConditions(1L, draftId, conditions(LocalDate.of(2026, 10, 4), null, null, null));
        DraftResponse afterConditions = draftService.getDraft(1L, draftId);
        assertStatus(() -> service.regenerate(1L, draftId), HttpStatus.UNPROCESSABLE_ENTITY);

        DraftResponse after = draftService.getDraft(1L, draftId);
        assertThat(after.visitDate()).isEqualTo(LocalDate.of(2026, 10, 4));
        assertThat(after.recommendationReasons()).extracting(r -> r.reason()).contains("선택한 행사 · 10월 4일 진행 · 18:00 시작");
        assertThat(after.items()).extracting(PlanItemResponse::itemId)
                .containsExactlyElementsOf(afterConditions.items().stream().map(PlanItemResponse::itemId).toList());
    }

    @Test
    void 자정을_넘는_영업과_브레이크타임도_식사_60분_기준으로_판단한다() {
        // GPT 검토 E1: 17:00~02:00 가게의 00:30 식사는 전날 밤 영업에 들어간다
        assertThat(PlanRecommendService.openDuringMeal(restaurant("NIGHT", "KOREAN", 1, "17:00", "02:00", null, null),
                LocalTime.of(0, 30))).isTrue();
        assertThat(PlanRecommendService.openDuringMeal(restaurant("NIGHT", "KOREAN", 1, "17:00", "02:00", null, null),
                LocalTime.of(1, 30))).isFalse();
        // GPT 검토 E2: 23:00~00:30 브레이크타임은 22:30~23:30 식사와 겹친다
        assertThat(PlanRecommendService.openDuringMeal(restaurant("BREAK", "KOREAN", 1, "12:00", "03:00", "23:00", "00:30"),
                LocalTime.of(22, 30))).isFalse();
        assertThat(PlanRecommendService.openDuringMeal(restaurant("BREAK", "KOREAN", 1, "12:00", "03:00", "23:00", "00:30"),
                LocalTime.of(21, 0))).isTrue();
        // 브레이크가 끝나는 시각에 시작하는 식사는 겹치지 않는다
        assertThat(PlanRecommendService.openDuringMeal(restaurant("DAY", "KOREAN", 1, "11:00", "21:00", "15:00", "17:00"),
                LocalTime.of(17, 0))).isTrue();
    }

    @Test
    void 조건_수정에서_BOTH_는_점심_저녁_모두로_바꾼다() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", null, null));
        places.restaurants.add(restaurant("PL-2", "KOREAN", 560, "10:30", "20:00", null, null));
        Long draftId = service.recommend(1L, request(null, "LUNCH", null, null)).draftId();

        DraftConditionsResponse changed = service.updateConditions(1L, draftId, conditions(null, null, "BOTH", null));

        assertThat(changed.conditions().mealType()).isNull();
        assertThat(mapper.plans.get(draftId).getMealType()).isNull();
    }

    @Test
    void 조건_수정에서_지난_날짜로는_바꿀_수_없다() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", null, null));
        Long draftId = service.recommend(1L, request(null, "LUNCH", null, null)).draftId();

        assertStatus(() -> service.updateConditions(1L, draftId,
                conditions(LocalDate.of(2026, 9, 26), null, null, null)), HttpStatus.BAD_REQUEST);
        assertThat(mapper.plans.get(draftId).getTripDate()).isEqualTo(VISIT_DATE);
    }

    @Test
    void 보내지_않은_조건은_그대로_둔다() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", null, null));
        Long draftId = service.recommend(1L, request("KOREAN", "DINNER", null, null)).draftId();

        DraftConditionsResponse changed = service.updateConditions(1L, draftId, conditions(null, null, null, "WALK"));

        assertThat(changed.conditions().visitDate()).isEqualTo(VISIT_DATE);
        assertThat(changed.conditions().foodPreference()).isEqualTo("KOREAN");
        assertThat(changed.conditions().mealType()).isEqualTo("DINNER");
        assertThat(changed.conditions().transportMode()).isEqualTo("WALK");
        assertThat(changed.conditions().endTime()).isEqualTo("21:00");
    }

    @Test
    void 새_조건으로_추천이_안_되면_조건을_바꾸지_않는다() {
        places.restaurants.add(restaurant("PL-1", "KOREAN", 550, "11:00", "21:00", null, null));
        Long draftId = service.recommend(1L, request(null, "LUNCH", null, null)).draftId();
        TripPlan before = mapper.plans.get(draftId);
        List<Long> rowIdsBefore = List.copyOf(mapper.rows.keySet());

        assertStatus(() -> service.updateConditions(1L, draftId, conditions(null, "JAPANESE", null, null)),
                HttpStatus.UNPROCESSABLE_ENTITY);
        assertStatus(() -> service.updateConditions(1L, draftId,
                new DraftConditionsRequest(LocalDate.of(2026, 11, 1), null, null, null, null, null, null)),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.updateConditions(1L, draftId,
                new DraftConditionsRequest(null, "10:00", "17:00", null, null, null, null)),
                HttpStatus.BAD_REQUEST);

        TripPlan after = mapper.plans.get(draftId);
        assertThat(mapper.rows.keySet()).containsExactlyElementsOf(rowIdsBefore);
        assertThat(after.getFoodPreference()).isEqualTo(before.getFoodPreference());
        assertThat(after.getTripDate()).isEqualTo(before.getTripDate());
        assertThat(after.getVisitEndTime()).isEqualTo(before.getVisitEndTime());
    }

    private static DraftConditionsRequest conditions(LocalDate date, String food, String meal, String transport) {
        return new DraftConditionsRequest(date, null, null, null, food, meal, transport);
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(expected));
    }

    /** 행사장 북쪽으로 distance(m) 떨어진 하루 종일 영업하는 식당 — 좌표와 거리가 맞도록 위도만 옮긴다 */
    private void addNearby(String id, double distance) {
        addNearby(id, "KOREAN", distance);
    }

    private void addNearby(String id, String cuisine, double distance) {
        places.restaurants.add(restaurant(id, cuisine, distance, "09:00", "23:00", null, null));
        String lat = String.valueOf(37.5796 + Math.toDegrees(distance / 6371000));
        mapper.displays.put(id, new FakePlanDraftMapper.Display(id, null, "126.9770000", lat,
                LocalTime.of(9, 0), LocalTime.of(23, 0), null, null));
        mapper.cuisines.put(id, cuisine);
    }

    private List<String> placeIds(Long draftId) {
        return mapper.findItemsByPlanId(draftId).stream()
                .map(item -> item.getPlaceContentId())
                .filter(id -> id != null)
                .toList();
    }

    /** 후보로 넣고, 초안 조회(findItemsByPlanId)에도 음식 종류·영업시간이 나오도록 등록한다 (실제 DB 조회와 같게) */
    private void addRestaurant(RestaurantView restaurant) {
        places.restaurants.add(restaurant);
        mapper.displays.put(restaurant.getContentId(), new FakePlanDraftMapper.Display(restaurant.getContentId(), null,
                null, null, restaurant.getOpenTime(), restaurant.getCloseTime(),
                restaurant.getBreakOpenTime(), restaurant.getBreakCloseTime()));
        mapper.cuisines.put(restaurant.getContentId(), restaurant.getCuisineType());
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
