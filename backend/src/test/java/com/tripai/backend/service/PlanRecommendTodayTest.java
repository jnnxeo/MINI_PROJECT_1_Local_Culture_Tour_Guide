package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.PlanItemResponse;
import com.tripai.backend.domain.dto.PlanRecommendRequest;
import com.tripai.backend.domain.entity.RecommendEventView;
import com.tripai.backend.domain.entity.RestaurantView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** 오늘 날짜로 만드는 일정은 지금 이후로만 (09-28 흐름 검증: 밤 9시에 오늘 10:00 일정이 만들어졌다) */
class PlanRecommendTodayTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    private PlanRecommendService serviceAt(int hour, int minute, FakePlanDraftMapper mapper,
                                           PlanRecommendServiceTest.FakePlaceMapper places) {
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 9, 28, hour, minute, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
        return new PlanRecommendService(mapper, places, new PlanDraftService(mapper, clock), clock);
    }

    private static void setUp(FakePlanDraftMapper mapper, PlanRecommendServiceTest.FakePlaceMapper places) {
        mapper.recommendEvents.put("FIXED", event("FIXED", LocalTime.of(10, 0), LocalTime.of(12, 0)));
        mapper.recommendEvents.put("OPEN", event("OPEN", null, null));
        places.restaurants.add(RestaurantView.builder().contentId("KR-1").placeName("KR-1").cuisineType("KOREAN")
                .distance(300.0).openTime(LocalTime.of(10, 0)).closeTime(LocalTime.of(22, 0)).build());
        places.restaurants.add(RestaurantView.builder().contentId("KR-2").placeName("KR-2").cuisineType("KOREAN")
                .distance(350.0).openTime(LocalTime.of(10, 0)).closeTime(LocalTime.of(22, 0)).build());
    }

    @Test
    void 오늘_이미_시작한_행사는_만들지_않는다() {
        FakePlanDraftMapper mapper = new FakePlanDraftMapper();
        PlanRecommendServiceTest.FakePlaceMapper places = new PlanRecommendServiceTest.FakePlaceMapper();
        setUp(mapper, places);
        PlanRecommendService service = serviceAt(13, 10, mapper, places);

        assertThatThrownBy(() -> service.recommend(1L, request("FIXED")))
                .isInstanceOfSatisfying(PlanRuleException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).contains("이미 시작한 행사");
                });
    }

    @Test
    void 오늘_시간_정보_없는_행사는_지금_이후부터_잡는다() {
        FakePlanDraftMapper mapper = new FakePlanDraftMapper();
        PlanRecommendServiceTest.FakePlaceMapper places = new PlanRecommendServiceTest.FakePlaceMapper();
        setUp(mapper, places);
        PlanRecommendService service = serviceAt(13, 10, mapper, places);

        DraftResponse draft = service.recommend(1L, request("OPEN"));

        // 13:10 → 13:30 부터. 점심(12:30)은 지나서 없고 저녁만
        assertThat(draft.items()).extracting(PlanItemResponse::startTime).allMatch(time -> time.compareTo("13:30") >= 0);
        assertThat(draft.conditions().startTime()).isEqualTo("13:30");
    }

    @Test
    void 오늘_저녁_8시가_지나면_오늘은_만들지_않는다() {
        FakePlanDraftMapper mapper = new FakePlanDraftMapper();
        PlanRecommendServiceTest.FakePlaceMapper places = new PlanRecommendServiceTest.FakePlaceMapper();
        setUp(mapper, places);
        PlanRecommendService service = serviceAt(21, 40, mapper, places);

        assertThatThrownBy(() -> service.recommend(1L, request("OPEN")))
                .isInstanceOfSatisfying(PlanRuleException.class, e -> assertThat(e.getMessage()).contains("내일 이후"));
    }

    @Test
    void 메인_AI_추천_저녁_8시가_지나면_오늘은_빼고_고른다() {
        FakePlanDraftMapper mapper = new FakePlanDraftMapper();
        PlanRecommendServiceTest.FakePlaceMapper places = new PlanRecommendServiceTest.FakePlaceMapper();
        setUp(mapper, places);
        PlanRecommendService service = serviceAt(21, 40, mapper, places);

        // 오늘만 고르면 "오늘은 시간이 부족" 안내 (행사·맛집 없음 422 가 아니라)
        assertThatThrownBy(() -> service.recommend(1L, mainRequest(List.of(TODAY))))
                .isInstanceOfSatisfying(PlanRuleException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).contains("내일 이후");
                });

        // 오늘과 내일을 고르면 내일로 만든다
        DraftResponse draft = service.recommend(1L, mainRequest(List.of(TODAY, TODAY.plusDays(1))));
        assertThat(draft.visitDate()).isEqualTo(TODAY.plusDays(1));
    }

    private static PlanRecommendRequest request(String eventId) {
        return new PlanRecommendRequest(eventId, TODAY, null, null, null, null, null, "WALK_TRANSIT", null);
    }

    private static PlanRecommendRequest mainRequest(List<LocalDate> dates) {
        return new PlanRecommendRequest(null, null, null, null, null, null, null, "WALK_TRANSIT", null,
                null, null, null, dates, null, null, null);
    }

    private static RecommendEventView event(String id, LocalTime start, LocalTime end) {
        return RecommendEventView.builder().eventContentId(id).eventName(id).displayYn(true)
                .eventStartDate(TODAY.minusDays(3)).eventEndDate(TODAY.plusDays(3))
                .eventStartTime(start).eventEndTime(end)
                .mapx(new BigDecimal("126.9770000")).mapy(new BigDecimal("37.5796000")).build();
    }
}
