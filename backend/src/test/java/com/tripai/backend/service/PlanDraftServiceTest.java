package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.PlanItemResponse;
import com.tripai.backend.domain.entity.PlanItemView;
import com.tripai.backend.domain.entity.TripPlan;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.repository.PlanDraftMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanDraftServiceTest {

    private static final long OWNER_ID = 2L;
    private static final long DRAFT_ID = 12L;

    private final Map<Long, TripPlan> plans = new HashMap<>();
    private final Map<Long, List<PlanItemView>> items = new HashMap<>();
    private final Map<Long, String> savedTitles = new HashMap<>();
    private PlanDraftService service;

    @BeforeEach
    void setUp() {
        PlanDraftMapper mapper = new PlanDraftMapper() {
            @Override
            public Optional<TripPlan> findPlanById(Long tripPlanId) {
                return Optional.ofNullable(plans.get(tripPlanId));
            }

            @Override
            public List<PlanItemView> findItemsByPlanId(Long tripPlanId) {
                return items.getOrDefault(tripPlanId, List.of());
            }

            @Override
            public int updateTitle(Long tripPlanId, String title) {
                savedTitles.put(tripPlanId, title);
                return 1;
            }
        };
        service = new PlanDraftService(mapper);

        plans.put(DRAFT_ID, draft(false));
        items.put(DRAFT_ID, List.of(lunchItem(), eventItem()));
    }

    @Test
    void 본인_초안을_명세_필드로_조회한다() {
        DraftResponse response = service.getDraft(OWNER_ID, DRAFT_ID);

        assertThat(response.draftId()).isEqualTo(DRAFT_ID);
        assertThat(response.title()).isEqualTo("고궁의 밤을 기다리는 하루");
        assertThat(response.visitDate()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(response.tripType()).isEqualTo("DAY_TRIP");
        assertThat(response.selectedEvent().eventId()).isEqualTo("DEV-EV-001");
        assertThat(response.selectedEvent().title()).isEqualTo("[샘플] 고궁의 밤, 달빛 산책");
        assertThat(response.conditions().startTime()).isEqualTo("11:00");
        assertThat(response.conditions().endTime()).isEqualTo("21:00");
        assertThat(response.conditions().transportMode()).isEqualTo("WALK_TRANSIT");
        assertThat(response.items()).extracting(PlanItemResponse::sequence).containsExactly(1, 2);
    }

    @Test
    void 항목은_타입별_아이디와_HHmm_시각_위경도로_내려준다() {
        DraftResponse response = service.getDraft(OWNER_ID, DRAFT_ID);
        PlanItemResponse lunch = response.items().get(0);
        PlanItemResponse event = response.items().get(1);

        assertThat(lunch.type()).isEqualTo("PLACE");
        assertThat(lunch.placeId()).isEqualTo("DEV-PL-001");
        assertThat(lunch.startTime()).isEqualTo("12:30");
        assertThat(lunch.lat()).isEqualTo(37.5758);
        assertThat(lunch.lng()).isEqualTo(126.9731);
        assertThat(lunch.breakTime()).isEqualTo("15:00~17:00");
        assertThat(lunch.timeFixed()).isFalse();

        assertThat(event.type()).isEqualTo("EVENT");
        assertThat(event.placeId()).isEqualTo("DEV-EV-001");
        assertThat(event.timeFixed()).isTrue();
        assertThat(event.openTime()).isNull();
    }

    @Test
    void 추천이유와_지도좌표는_값이_있는_항목만_담는다() {
        DraftResponse response = service.getDraft(OWNER_ID, DRAFT_ID);

        assertThat(response.recommendationReasons()).hasSize(1);
        assertThat(response.recommendationReasons().get(0).itemId()).isEqualTo(101L);
        assertThat(response.mapPoints()).extracting(point -> point.sequence()).containsExactly(1, 2);
    }

    @Test
    void 없는_초안이면_404() {
        assertThatThrownBy(() -> service.getDraft(OWNER_ID, 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAN_NOT_FOUND);
    }

    @Test
    void 다른_사람_초안이면_403() {
        assertThatThrownBy(() -> service.getDraft(3L, DRAFT_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 이미_저장된_일정은_초안으로_조회되지_않는다() {
        plans.put(DRAFT_ID, draft(true));

        assertThatThrownBy(() -> service.getDraft(OWNER_ID, DRAFT_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAN_NOT_FOUND);
    }

    @Test
    void 제목은_앞뒤_공백을_빼고_저장한다() {
        var response = service.updateTitle(OWNER_ID, DRAFT_ID, "  서울에서 보내는 문화 산책  ");

        assertThat(response.draftId()).isEqualTo(DRAFT_ID);
        assertThat(response.title()).isEqualTo("서울에서 보내는 문화 산책");
        assertThat(savedTitles).containsEntry(DRAFT_ID, "서울에서 보내는 문화 산책");
    }

    @Test
    void 다른_사람_초안_제목은_바꿀_수_없다() {
        assertThatThrownBy(() -> service.updateTitle(3L, DRAFT_ID, "새 제목"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(savedTitles).isEmpty();
    }

    private static TripPlan draft(boolean saved) {
        return TripPlan.builder()
                .tripPlanId(DRAFT_ID)
                .userId(OWNER_ID)
                .anchorContentId("DEV-EV-001")
                .anchorEventName("[샘플] 고궁의 밤, 달빛 산책")
                .title("고궁의 밤을 기다리는 하루")
                .tripDate(LocalDate.of(2026, 10, 3))
                .visitStartTime(LocalTime.of(11, 0))
                .visitEndTime(LocalTime.of(21, 0))
                .saveYn(saved)
                .aiYn(false)
                .transportMd("WALK_TRANSIT")
                .headcount(2)
                .build();
    }

    private static PlanItemView lunchItem() {
        return PlanItemView.builder()
                .tripItemId(101L)
                .seqOrder(1)
                .itemType("PLACE")
                .placeContentId("DEV-PL-001")
                .startTime(LocalTime.of(12, 30))
                .durationMin(60)
                .aiReason("행사장 반경 · 식사 시간대")
                .timeFixYn(false)
                .name("[샘플] 경복궁 앞 한식당")
                .mapx(new BigDecimal("126.9731000"))
                .mapy(new BigDecimal("37.5758000"))
                .openTime(LocalTime.of(11, 0))
                .closeTime(LocalTime.of(21, 0))
                .breakOpenTime(LocalTime.of(15, 0))
                .breakCloseTime(LocalTime.of(17, 0))
                .build();
    }

    private static PlanItemView eventItem() {
        return PlanItemView.builder()
                .tripItemId(102L)
                .seqOrder(2)
                .itemType("EVENT")
                .eventContentId("DEV-EV-001")
                .startTime(LocalTime.of(18, 0))
                .durationMin(180)
                .timeFixYn(true)
                .name("[샘플] 고궁의 밤, 달빛 산책")
                .mapx(new BigDecimal("126.9770000"))
                .mapy(new BigDecimal("37.5796000"))
                .build();
    }
}
