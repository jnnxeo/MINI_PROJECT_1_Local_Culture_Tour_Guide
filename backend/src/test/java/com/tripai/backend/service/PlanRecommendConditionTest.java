package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tripai.backend.domain.dto.DraftConditionsRequest;
import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.PlanItemResponse;
import com.tripai.backend.domain.dto.PlanRecommendRequest;
import com.tripai.backend.domain.entity.RecommendEventView;
import com.tripai.backend.domain.entity.RestaurantView;
import com.tripai.backend.domain.entity.TripPlan;
import com.tripai.backend.global.exception.CustomException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** 메인 AI 추천 — 행사 ID 없이 방문 가능한 날짜·행사 분야·지역·무료 여부로 행사를 고른다 (docs/07 [제안]) */
class PlanRecommendConditionTest {

    private static final LocalDate OCT_3 = LocalDate.of(2026, 10, 3);
    private static final LocalDate OCT_8 = LocalDate.of(2026, 10, 8);

    private FakePlanDraftMapper mapper;
    private PlanRecommendServiceTest.FakePlaceMapper places;
    private PlanDraftService draftService;
    private PlanRecommendService service;

    @BeforeEach
    void setUp() {
        mapper = new FakePlanDraftMapper();
        places = new PlanRecommendServiceTest.FakePlaceMapper();
        // 오늘은 2026-09-28
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 9, 28, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
        draftService = new PlanDraftService(mapper, clock);
        service = new PlanRecommendService(mapper, places, draftService, clock);
        // 종료일: EV-A 10/5 < EV-C 10/20. EV-B 는 공연(10/1~10/10), EV-X 는 좌표 없음
        event("EV-A", "고궁의 밤", "전시/미술", "종로구", true, "10-01", "10-05", "18:00", "21:00", true);
        event("EV-B", "한강 콘서트", "콘서트", "중구", false, "10-01", "10-10", null, null, true);
        event("EV-C", "현대미술전", "전시/미술", "종로구", false, "10-01", "10-20", "14:00", "16:00", true);
        event("EV-X", "좌표 없는 전시", "전시/미술", "종로구", true, "09-01", "10-02", "12:00", "13:00", false);
        restaurant("KR-1", 300);
        restaurant("KR-2", 350);
        restaurant("KR-3", 400);
    }

    @Test
    void 행사_ID_없이_날짜와_분야로_종료가_가까운_행사를_고른다() {
        DraftResponse draft = service.recommend(1L, conditions(List.of(OCT_3), List.of("전시"), null, null));

        assertThat(draft.selectedEvent().eventId()).isEqualTo("EV-A");
        assertThat(draft.visitDate()).isEqualTo(OCT_3);
        assertThat(draft.title()).isEqualTo("10월 3일 고궁의 밤");
        assertThat(draft.items()).extracting(PlanItemResponse::placeId).contains("EV-A");
        assertThat(draft.conditions().availableDates()).containsExactly(OCT_3);
        assertThat(draft.conditions().categories()).containsExactly("전시");
        TripPlan saved = mapper.plans.get(draft.draftId());
        assertThat(saved.getSearchDates()).isEqualTo("2026-10-03");
        assertThat(saved.getSearchCategories()).isEqualTo("전시");
    }

    @Test
    void 체크한_날짜_중_지난_날은_빼고_빠른_날부터_본다() {
        DraftResponse draft = service.recommend(1L,
                conditions(List.of(OCT_8, LocalDate.of(2026, 9, 20), OCT_3), List.of("공연"), null, null));

        assertThat(draft.selectedEvent().eventId()).isEqualTo("EV-B");
        assertThat(draft.visitDate()).isEqualTo(OCT_3);
    }

    @Test
    void 지역과_무료_조건을_지키고_전체_지역은_조건_없음이다() {
        DraftResponse free = service.recommend(1L, conditions(List.of(OCT_3), null, "종로구", true));
        assertThat(free.selectedEvent().eventId()).isEqualTo("EV-A");

        DraftResponse district = service.recommend(1L, conditions(List.of(OCT_8), null, "중구", null));
        assertThat(district.selectedEvent().eventId()).isEqualTo("EV-B");

        DraftResponse all = service.recommend(1L, conditions(List.of(OCT_8), List.of("전시"), "전체 지역", null));
        assertThat(all.selectedEvent().eventId()).isEqualTo("EV-C");
        assertThat(all.conditions().district()).isNull();
    }

    @Test
    void 방문_시간_안에_들지_않는_행사는_건너뛰고_다음_행사로_만든다() {
        // 방문 17:00 까지 — EV-A(18:00 시작)는 안 되고 EV-C(14:00~16:00)로
        DraftResponse draft = service.recommend(1L, new PlanRecommendRequest(null, null, null, "17:00", null, null, null,
                "WALK_TRANSIT", null, null, null, null, List.of(OCT_3), List.of("전시"), null, null));

        assertThat(draft.selectedEvent().eventId()).isEqualTo("EV-C");
    }

    @Test
    void 조건에_맞는_행사가_없거나_날짜가_잘못되면_만들지_않는다() {
        assertStatus(() -> service.recommend(1L, conditions(List.of(OCT_3), List.of("축제"), null, null)),
                HttpStatus.UNPROCESSABLE_ENTITY);
        assertStatus(() -> service.recommend(1L, conditions(List.of(LocalDate.of(2026, 9, 1)), null, null, null)),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.recommend(1L, conditions(List.of(), null, null, null)), HttpStatus.BAD_REQUEST);
        // 행사를 직접 고른 요청은 방문 날짜가 필요하다
        assertStatus(() -> service.recommend(1L, new PlanRecommendRequest("EV-A", null, null, null, null, null, null,
                "WALK_TRANSIT", null)), HttpStatus.BAD_REQUEST);
        // 없는 분야 이름
        assertThatThrownBy(() -> service.recommend(1L, conditions(List.of(OCT_3), List.of("없는분야"), null, null)))
                .isInstanceOf(CustomException.class);
        assertThat(mapper.plans).isEmpty();
    }

    @Test
    void 다시_추천은_같은_조건에서_다른_행사를_먼저_찾고_제목도_바꾼다() {
        Long draftId = service.recommend(1L, conditions(List.of(OCT_3), List.of("전시"), null, null)).draftId();

        DraftResponse again = service.regenerate(1L, draftId);

        assertThat(again.selectedEvent().eventId()).isEqualTo("EV-C");
        assertThat(again.title()).isEqualTo("10월 3일 현대미술전");
        assertThat(again.conditions().categories()).containsExactly("전시");
        assertThat(again.items()).extracting(PlanItemResponse::placeId).contains("EV-C").doesNotContain("EV-A");
    }

    @Test
    void 다시_추천은_세_행사를_순환한_뒤_처음으로_돌아온다() {
        Long id = service.recommend(1L, conditions(List.of(OCT_3), null, null, null)).draftId();
        assertThat(draftService.getDraft(1L, id).selectedEvent().eventId()).isEqualTo("EV-A");
        assertThat(service.regenerate(1L, id).selectedEvent().eventId()).isEqualTo("EV-B");
        assertThat(service.regenerate(1L, id).selectedEvent().eventId()).isEqualTo("EV-C");
        assertThat(service.regenerate(1L, id).selectedEvent().eventId()).isEqualTo("EV-A");
    }

    @Test
    void 다시_추천은_나중_날짜에서만_열리는_행사까지_순환한다() {
        event("EV-D", "나중 전시", "전시/미술", "종로구", true, "10-08", "10-15", "14:00", "16:00", true);
        Long id = service.recommend(1L, conditions(List.of(OCT_3, OCT_8), List.of("전시"), null, null)).draftId();
        assertThat(service.regenerate(1L, id).selectedEvent().eventId()).isEqualTo("EV-C");
        DraftResponse later = service.regenerate(1L, id);
        assertThat(later.selectedEvent().eventId()).isEqualTo("EV-D");
        assertThat(later.visitDate()).isEqualTo(OCT_8);
        assertThat(service.regenerate(1L, id).selectedEvent().eventId()).isEqualTo("EV-A");
    }

    @Test
    void 다른_행사가_없으면_행사는_그대로_두고_맛집만_바꾼다() {
        Long draftId = service.recommend(1L, conditions(List.of(OCT_3), List.of("공연"), null, null)).draftId();
        List<String> before = placeIds(draftService.getDraft(1L, draftId));

        DraftResponse again = service.regenerate(1L, draftId);

        assertThat(again.selectedEvent().eventId()).isEqualTo("EV-B");
        assertThat(placeIds(again)).isNotEqualTo(before);
    }

    @Test
    void 조건_수정은_지금_행사가_맞으면_유지하고_분야를_바꾸면_행사를_다시_고른다() {
        Long draftId = service.recommend(1L, conditions(List.of(OCT_3), List.of("전시"), null, null)).draftId();
        draftService.updateTitle(1L, draftId, "나의 고궁 나들이");

        // 음식 종류만 바꾸면 행사(EV-A)와 사용자가 바꾼 제목을 그대로 둔다
        service.updateConditions(1L, draftId, new DraftConditionsRequest(
                null, null, null, null, null, null, null, "KOREAN", "KOREAN", null));
        DraftResponse kept = draftService.getDraft(1L, draftId);
        assertThat(kept.selectedEvent().eventId()).isEqualTo("EV-A");
        assertThat(kept.title()).isEqualTo("나의 고궁 나들이");

        // 분야를 공연으로 바꾸면 행사를 다시 고르고, 행사가 바뀌었으니 제목도 새로
        service.updateConditions(1L, draftId, new DraftConditionsRequest(null, null, null, null, null, null, null,
                null, null, null, null, List.of("공연"), null, null));
        DraftResponse changed = draftService.getDraft(1L, draftId);
        assertThat(changed.selectedEvent().eventId()).isEqualTo("EV-B");
        assertThat(changed.title()).isEqualTo("10월 3일 한강 콘서트");
        assertThat(changed.conditions().categories()).containsExactly("공연");

        // 날짜를 바꾸면 그 날짜 기준으로 다시 고른다
        service.updateConditions(1L, draftId, new DraftConditionsRequest(null, null, null, null, null, null, null,
                null, null, null, List.of(OCT_8), List.of("전시"), null, null));
        DraftResponse moved = draftService.getDraft(1L, draftId);
        assertThat(moved.visitDate()).isEqualTo(OCT_8);
        assertThat(moved.selectedEvent().eventId()).isEqualTo("EV-C");
    }

    @Test
    void 조건_수정에서_전체_지역을_고르면_기존_지역_제한을_푼다() {
        // 종로구로 만든 초안 (10/8 종로구 전시 → EV-C)
        Long draftId = service.recommend(1L, conditions(List.of(OCT_8), null, "종로구", null)).draftId();
        assertThat(draftService.getDraft(1L, draftId).conditions().district()).isEqualTo("종로구");

        // 지역을 보내지 않으면(null) 기존 지역을 그대로 쓴다 (음식 종류만 바꾼 경우)
        service.updateConditions(1L, draftId, new DraftConditionsRequest(null, null, null, null, null, null, null,
                "KOREAN", "KOREAN", null, null, null, null, null));
        assertThat(draftService.getDraft(1L, draftId).conditions().district()).isEqualTo("종로구");

        // 화면의 '전체 지역'은 빈 값("")으로 보낸다 → 지역 제한을 풀고 중구 공연(EV-B)도 고를 수 있다
        service.updateConditions(1L, draftId, new DraftConditionsRequest(null, null, null, null, null, null, null,
                null, null, null, null, List.of("공연"), "", null));
        DraftResponse all = draftService.getDraft(1L, draftId);
        assertThat(all.conditions().district()).isNull();
        assertThat(all.selectedEvent().eventId()).isEqualTo("EV-B");
    }

    @Test
    void 행사를_직접_고른_초안은_조건이_없고_다시_추천에서_행사를_유지한다() {
        DraftResponse draft = service.recommend(1L, new PlanRecommendRequest("EV-A", OCT_3, null, null, null, null, null,
                "WALK_TRANSIT", null));
        assertThat(draft.conditions().availableDates()).isNull();

        DraftResponse again = service.regenerate(1L, draft.draftId());
        assertThat(again.selectedEvent().eventId()).isEqualTo("EV-A");
    }

    @Test
    void 날짜만_바꿔도_기본_제목이면_제목의_날짜를_맞추고_사용자가_바꾼_제목은_둔다() {
        Long draftId = service.recommend(1L, new PlanRecommendRequest("EV-B", OCT_3, null, null, null, null, null,
                "WALK_TRANSIT", null)).draftId();
        assertThat(draftService.getDraft(1L, draftId).title()).isEqualTo("10월 3일 한강 콘서트");

        service.updateConditions(1L, draftId, new DraftConditionsRequest(OCT_8, null, null, null, null, null, null));
        assertThat(draftService.getDraft(1L, draftId).title()).isEqualTo("10월 8일 한강 콘서트");

        draftService.updateTitle(1L, draftId, "주말 한강 나들이");
        service.updateConditions(1L, draftId, new DraftConditionsRequest(OCT_3, null, null, null, null, null, null));
        assertThat(draftService.getDraft(1L, draftId).title()).isEqualTo("주말 한강 나들이");
    }

    private static PlanRecommendRequest conditions(List<LocalDate> dates, List<String> categories, String district,
                                                   Boolean freeYn) {
        return new PlanRecommendRequest(null, null, null, null, null, null, null, "WALK_TRANSIT", null,
                null, null, null, dates, categories, district, freeYn);
    }

    private static List<String> placeIds(DraftResponse draft) {
        return draft.items().stream().filter(item -> "PLACE".equals(item.type())).map(PlanItemResponse::placeId).toList();
    }

    private void event(String id, String name, String type, String district, boolean free, String startDate,
                       String endDate, String startTime, String endTime, boolean hasCoords) {
        mapper.recommendEvents.put(id, RecommendEventView.builder()
                .eventContentId(id).eventName(name).eventType(type).districtName(district).freeYn(free)
                .displayYn(true)
                .eventStartDate(LocalDate.parse("2026-" + startDate)).eventEndDate(LocalDate.parse("2026-" + endDate))
                .eventStartTime(startTime == null ? null : LocalTime.parse(startTime))
                .eventEndTime(endTime == null ? null : LocalTime.parse(endTime))
                .mapx(hasCoords ? new BigDecimal("126.9770000") : null)
                .mapy(hasCoords ? new BigDecimal("37.5796000") : null)
                .build());
    }

    private void restaurant(String id, double distance) {
        places.restaurants.add(RestaurantView.builder().contentId(id).placeName(id).cuisineType("KOREAN").distance(distance)
                .openTime(LocalTime.of(10, 0)).closeTime(LocalTime.of(22, 0)).build());
        mapper.displays.put(id, new FakePlanDraftMapper.Display(id, null, null, null,
                LocalTime.of(10, 0), LocalTime.of(22, 0), null, null));
        mapper.cuisines.put(id, "KOREAN");
    }

    private static void assertStatus(Runnable call, HttpStatus expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(PlanRuleException.class, e -> assertThat(e.getStatus()).isEqualTo(expected));
    }
}
