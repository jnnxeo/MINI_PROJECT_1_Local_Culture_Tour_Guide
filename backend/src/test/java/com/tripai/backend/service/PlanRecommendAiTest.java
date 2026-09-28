package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tripai.backend.domain.dto.AiPlanInput;
import com.tripai.backend.domain.dto.AiPlanResponse;
import com.tripai.backend.domain.dto.DraftConditionsRequest;
import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.PlanItemResponse;
import com.tripai.backend.domain.dto.PlanRecommendRequest;
import com.tripai.backend.domain.dto.PlanScheduleItem;
import com.tripai.backend.domain.dto.RestaurantCandidate;
import com.tripai.backend.domain.entity.RecommendEventView;
import com.tripai.backend.domain.entity.RestaurantView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** AI 추천 연결 — AI 결과가 서버 규칙을 모두 지킬 때만 쓰고, 아니면 규칙 기반 추천으로 대체한다 */
class PlanRecommendAiTest {

    private static final LocalDate VISIT_DATE = LocalDate.of(2026, 10, 3);

    /** 넘겨받은 입력을 기록하고, 정해 둔 응답(또는 예외)을 돌려주는 가짜 AI */
    static class FakeAi implements AiPlanClient {
        boolean enabled = true;
        Function<AiPlanInput, AiPlanResponse> answer = input -> { throw new IllegalStateException("응답 없음"); };
        final List<AiPlanInput> inputs = new ArrayList<>();

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public AiPlanResponse generate(AiPlanInput input) {
            inputs.add(input);
            return answer.apply(input);
        }
    }

    private FakePlanDraftMapper mapper;
    private PlanRecommendServiceTest.FakePlaceMapper places;
    private PlanDraftService draftService;
    private FakeAi ai;
    private PlanRecommendService service;

    @BeforeEach
    void setUp() {
        mapper = new FakePlanDraftMapper();
        places = new PlanRecommendServiceTest.FakePlaceMapper();
        ai = new FakeAi();
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 9, 28, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul"));
        draftService = new PlanDraftService(mapper, clock);
        service = new PlanRecommendService(mapper, places, draftService, clock, ai);
        mapper.recommendEvents.put("EV-1", RecommendEventView.builder()
                .eventContentId("EV-1").eventName("고궁의 밤").eventPlace("경복궁").dateText("매일 18:00~21:00")
                .displayYn(true).eventStartDate(LocalDate.of(2026, 10, 1)).eventEndDate(LocalDate.of(2026, 10, 10))
                .eventStartTime(LocalTime.of(18, 0)).eventEndTime(LocalTime.of(21, 0))
                .mapx(new BigDecimal("126.9770000")).mapy(new BigDecimal("37.5796000")).build());
        // 규칙 기반이면 점심 PL-1, 저녁 PL-2 를 고른다
        addRestaurant(restaurant("PL-1", "KOREAN", 300, "11:00", "21:00", null, null));
        addRestaurant(restaurant("PL-2", "KOREAN", 400, "10:00", "22:00", null, null));
        addRestaurant(restaurant("PL-3", "WESTERN", 500, "11:00", "22:00", "15:00", "17:00"));
        addRestaurant(restaurant("UNKNOWN", "KOREAN", 100, null, null, null, null));
    }

    @Test
    void AI_결과가_규칙을_지키면_AI_일정과_제목_이유로_저장한다() {
        ai.answer = input -> response("고궁의 밤 전통 맛집 하루",
                item(1, "12:00", "13:00", "RESTAURANT", "PL-3", "행사장에서 약 500m 떨어진 양식당, 11:00~22:00 영업"),
                item(2, "17:00", "18:00", "RESTAURANT", "PL-2", "저녁 17:00에 영업 중인 가까운 한식당"),
                item(3, "18:00", "21:00", "EVENT", "EV-1", "선택한 행사, 매일 18:00~21:00"));

        DraftResponse draft = service.recommend(1L, request(null));

        assertThat(draft.title()).isEqualTo("고궁의 밤 전통 맛집 하루");
        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("PL-3", "PL-2", "EV-1");
        assertThat(draft.items()).extracting(PlanItemResponse::startTime).containsExactly("12:00", "17:00", "18:00");
        assertThat(draft.items().get(2).timeFixed()).isTrue();
        assertThat(draft.recommendationReasons()).extracting(r -> r.reason())
                .containsExactly("행사장에서 약 500m 떨어진 양식당, 11:00~22:00 영업", "저녁 17:00에 영업 중인 가까운 한식당",
                        "선택한 행사, 매일 18:00~21:00");
        assertThat(mapper.plans.get(draft.draftId()).getAiYn()).isTrue();
    }

    @Test
    void AI_에게는_선택_행사와_영업시간을_아는_주변_맛집만_넘긴다() {
        ai.answer = input -> { throw new IllegalStateException("검사용"); };

        service.recommend(1L, request("KOREAN"));

        AiPlanInput input = ai.inputs.get(0);
        assertThat(input.getUserConditions().getRequiredEventId()).isEqualTo("EV-1");
        assertThat(input.getUserConditions().getVisitStartTime()).isEqualTo("10:00");
        assertThat(input.getUserConditions().getFoodPreference()).isEqualTo("KOREAN");
        assertThat(input.getEventCandidates()).hasSize(1);
        assertThat(input.getEventCandidates().get(0).getDateText()).isEqualTo("매일 18:00~21:00");
        assertThat(input.getRestaurantCandidates()).extracting(RestaurantCandidate::getContentId)
                .containsExactly("PL-1", "PL-2");
    }

    @Test
    void AI_가_고정된_행사_시간을_바꾸면_규칙_기반_추천을_쓴다() {
        // 실제 검증에서 20번 중 1번 나온 경우 — 18:00 행사를 13:00~17:00 으로 옮김
        ai.answer = input -> response("제목",
                item(1, "11:30", "12:30", "RESTAURANT", "PL-1", "점심"),
                item(2, "13:00", "17:00", "EVENT", "EV-1", "행사"));

        assertRuleBased(service.recommend(1L, request(null)));
    }

    @Test
    void AI_가_행사를_두_번_넣거나_후보_밖_식당을_넣으면_규칙_기반_추천을_쓴다() {
        ai.answer = input -> response("제목",
                item(1, "12:00", "13:00", "RESTAURANT", "UNKNOWN", "영업시간 모름"),
                item(2, "18:00", "21:00", "EVENT", "EV-1", "행사"));
        assertRuleBased(service.recommend(1L, request(null)));

        ai.answer = input -> response("제목",
                item(1, "10:00", "11:00", "EVENT", "EV-1", "행사"),
                item(2, "12:00", "13:00", "RESTAURANT", "PL-1", "점심"),
                item(3, "18:00", "21:00", "EVENT", "EV-1", "행사"));
        assertRuleBased(service.recommend(1L, request(null)));
    }

    @Test
    void AI_가_브레이크타임이나_식사_시간대_밖에_식당을_두면_규칙_기반_추천을_쓴다() {
        ai.answer = input -> response("제목",
                item(1, "14:30", "15:30", "RESTAURANT", "PL-3", "브레이크 15:00~17:00 과 겹침"),
                item(2, "18:00", "21:00", "EVENT", "EV-1", "행사"));
        assertRuleBased(service.recommend(1L, request(null)));

        ai.answer = input -> response("제목",
                item(1, "15:30", "16:30", "RESTAURANT", "PL-1", "점심도 저녁도 아닌 시각"),
                item(2, "18:00", "21:00", "EVENT", "EV-1", "행사"));
        assertRuleBased(service.recommend(1L, request(null)));
    }

    @Test
    void AI_호출이_실패하거나_키가_없으면_규칙_기반_추천을_쓴다() {
        ai.answer = input -> { throw new IllegalStateException("OpenAI API 호출 실패 (HTTP 401)"); };
        assertRuleBased(service.recommend(1L, request(null)));

        ai.enabled = false;
        ai.inputs.clear();
        assertRuleBased(service.recommend(1L, request(null)));
        assertThat(ai.inputs).isEmpty();
    }

    @Test
    void 이유는_100자로_자르고_너무_긴_제목은_기본_제목을_쓴다() {
        String longReason = "가".repeat(150);
        ai.answer = input -> response("제".repeat(101),
                item(1, "12:00", "13:00", "RESTAURANT", "PL-1", longReason),
                item(2, "17:00", "18:00", "RESTAURANT", "PL-2", "저녁"),
                item(3, "18:00", "21:00", "EVENT", "EV-1", "행사"));

        DraftResponse draft = service.recommend(1L, request(null));

        assertThat(draft.title()).isEqualTo("10월 3일 고궁의 밤");
        assertThat(draft.recommendationReasons().get(0).reason()).hasSize(100);
        assertThat(mapper.plans.get(draft.draftId()).getAiYn()).isTrue();
    }

    @Test
    void 규칙_기반으로는_채운_식사를_AI_가_빼먹으면_규칙_기반_추천을_쓴다() {
        ai.answer = input -> response("제목",
                item(1, "12:00", "13:00", "RESTAURANT", "PL-1", "점심만"),
                item(2, "18:00", "21:00", "EVENT", "EV-1", "행사"));

        assertRuleBased(service.recommend(1L, request(null)));
    }

    @Test
    void 다시_추천에서는_지금_맛집을_모두_빼고_넘긴다() {
        // AI 가 11:30 처럼 규칙 기반과 다른 시각에 둔 맛집도 '지금 맛집'으로 본다
        ai.answer = input -> response("첫 추천",
                item(1, "11:30", "12:30", "RESTAURANT", "PL-1", "점심"),
                item(2, "17:00", "18:00", "RESTAURANT", "PL-2", "저녁"),
                item(3, "18:00", "21:00", "EVENT", "EV-1", "행사"));
        Long draftId = service.recommend(1L, request(null)).draftId();
        addRestaurant(restaurant("PL-4", "KOREAN", 600, "10:00", "22:00", null, null));
        ai.inputs.clear();
        ai.answer = input -> response("다시 추천",
                item(1, "12:00", "13:00", "RESTAURANT", "PL-3", "다른 양식당"),
                item(2, "17:00", "18:00", "RESTAURANT", "PL-4", "다른 한식당"),
                item(3, "18:00", "21:00", "EVENT", "EV-1", "행사"));

        DraftResponse again = service.regenerate(1L, draftId);

        assertThat(ai.inputs.get(0).getRestaurantCandidates()).extracting(RestaurantCandidate::getContentId)
                .doesNotContain("PL-1", "PL-2");
        assertThat(again.items()).extracting(PlanItemResponse::placeId).containsExactly("PL-3", "PL-4", "EV-1");
        // 다시 추천은 제목을 바꾸지 않는다
        assertThat(again.title()).isEqualTo("첫 추천");
        assertThat(mapper.plans.get(draftId).getAiYn()).isTrue();
    }

    @Test
    void 조건_수정도_AI_로_만들고_실패하면_규칙_기반으로_만든다() {
        ai.enabled = false;
        Long draftId = service.recommend(1L, request(null)).draftId();
        ai.enabled = true;
        ai.answer = input -> response("제목",
                item(1, "17:00", "18:00", "RESTAURANT", "PL-2", "저녁 한식"),
                item(2, "18:00", "21:00", "EVENT", "EV-1", "행사"));

        service.updateConditions(1L, draftId, new DraftConditionsRequest(null, null, null, null, null, "DINNER", null));
        DraftResponse draft = draftService.getDraft(1L, draftId);
        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("PL-2", "EV-1");
        assertThat(mapper.plans.get(draftId).getAiYn()).isTrue();

        ai.answer = input -> { throw new IllegalStateException("시간 초과"); };
        service.updateConditions(1L, draftId, new DraftConditionsRequest(null, null, null, null, null, "LUNCH", null));
        assertThat(mapper.plans.get(draftId).getAiYn()).isFalse();
        assertThat(draftService.getDraft(1L, draftId).items()).extracting(PlanItemResponse::startTime)
                .containsExactly("12:30", "18:00");
    }

    /** 후보로 넣고, 초안 조회(findItemsByPlanId)에도 음식 종류·영업시간이 나오도록 등록한다 (실제 DB 조회와 같게) */
    private void addRestaurant(RestaurantView restaurant) {
        places.restaurants.add(restaurant);
        mapper.displays.put(restaurant.getContentId(), new FakePlanDraftMapper.Display(restaurant.getContentId(), null,
                null, null, restaurant.getOpenTime(), restaurant.getCloseTime(),
                restaurant.getBreakOpenTime(), restaurant.getBreakCloseTime()));
        mapper.cuisines.put(restaurant.getContentId(), restaurant.getCuisineType());
    }

    private void assertRuleBased(DraftResponse draft) {
        assertThat(draft.items()).extracting(PlanItemResponse::placeId).containsExactly("PL-1", "PL-2", "EV-1");
        assertThat(draft.items()).extracting(PlanItemResponse::startTime).containsExactly("12:30", "17:00", "18:00");
        assertThat(draft.title()).isEqualTo("10월 3일 고궁의 밤");
        assertThat(mapper.plans.get(draft.draftId()).getAiYn()).isFalse();
    }

    private static PlanRecommendRequest request(String food) {
        return new PlanRecommendRequest("EV-1", VISIT_DATE, null, null, null, food, null, "WALK_TRANSIT", null);
    }

    private static AiPlanResponse response(String title, PlanScheduleItem... items) {
        return AiPlanResponse.builder().title(title).schedule(List.of(items)).issues(List.of()).build();
    }

    private static PlanScheduleItem item(int seq, String start, String end, String type, String id, String reason) {
        return PlanScheduleItem.builder().sequence(seq).startTime(start).endTime(end).placeType(type)
                .placeContentId(id).placeName(id).reason(reason).build();
    }

    private static RestaurantView restaurant(String id, String cuisine, double distance, String open, String close,
                                             String breakOpen, String breakClose) {
        return RestaurantView.builder().contentId(id).placeName(id).cuisineType(cuisine).distance(distance)
                .openTime(time(open)).closeTime(time(close)).breakOpenTime(time(breakOpen)).breakCloseTime(time(breakClose))
                .build();
    }

    private static LocalTime time(String value) {
        return value == null ? null : LocalTime.parse(value);
    }
}
