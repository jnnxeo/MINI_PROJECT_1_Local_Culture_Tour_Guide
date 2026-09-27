package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tripai.backend.domain.dto.DraftItemAddRequest;
import com.tripai.backend.domain.dto.DraftItemAddResponse;
import com.tripai.backend.domain.dto.DraftItemRequest;
import com.tripai.backend.domain.dto.DraftItemsResponse;
import com.tripai.backend.domain.dto.DraftResponse;
import com.tripai.backend.domain.dto.PlanItemResponse;
import com.tripai.backend.domain.dto.PlanSaveResponse;
import com.tripai.backend.domain.entity.ItemTargetView;
import com.tripai.backend.domain.entity.TripItem;
import com.tripai.backend.domain.entity.TripPlan;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PlanDraftServiceTest {

    private static final long OWNER_ID = 2L;
    private static final long DRAFT_ID = 12L;
    private static final String CORE_EVENT = "DEV-EV-001";

    private FakePlanDraftMapper mapper;
    private PlanDraftService service;

    @BeforeEach
    void setUp() {
        mapper = new FakePlanDraftMapper();
        // 오늘 = 2026-09-26 (한국 시간) 으로 고정
        Clock today = Clock.fixed(ZonedDateTime.of(2026, 9, 26, 1, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        service = new PlanDraftService(mapper, today);

        mapper.plans.put(DRAFT_ID, draft(false));
        mapper.addRow(row(101L, 1, "PLACE", "DEV-PL-001", "12:30", 60, "행사장 반경 · 식사 시간대", false));
        mapper.addRow(row(102L, 2, "EVENT", CORE_EVENT, "18:00", 180, "검색 조건 일치", true));

        mapper.displays.put("DEV-PL-001", new FakePlanDraftMapper.Display("[샘플] 경복궁 앞 한식당", "서울 종로구 사직로 125",
                "126.9731000", "37.5758000", LocalTime.of(11, 0), LocalTime.of(21, 0), LocalTime.of(15, 0), LocalTime.of(17, 0)));
        mapper.displays.put(CORE_EVENT, new FakePlanDraftMapper.Display("[샘플] 고궁의 밤, 달빛 산책", "서울 종로구 사직로 161",
                "126.9770000", "37.5796000", null, null, null, null));
        mapper.displays.put("DEV-PL-003", new FakePlanDraftMapper.Display("[샘플] 서촌 파스타", null, null, null, null, null, null, null));

        mapper.places.put("DEV-PL-001", place("DEV-PL-001", "[샘플] 경복궁 앞 한식당", true));
        mapper.places.put("DEV-PL-002", place("DEV-PL-002", "[샘플] 삼청동 칼국수", true));
        mapper.places.put("DEV-PL-003", place("DEV-PL-003", "[샘플] 서촌 파스타", true));
        mapper.places.put("DEV-PL-006", place("DEV-PL-006", "[샘플] 비표출 식당", false));
        mapper.events.put(CORE_EVENT, event(CORE_EVENT, "2026-09-01", "2026-10-31", "18:00"));
    }

    @Nested
    class 초안_조회 {

        @Test
        void 본인_초안을_명세_필드로_조회한다() {
            DraftResponse response = service.getDraft(OWNER_ID, DRAFT_ID);

            assertThat(response.draftId()).isEqualTo(DRAFT_ID);
            assertThat(response.title()).isEqualTo("고궁의 밤을 기다리는 하루");
            assertThat(response.visitDate()).isEqualTo(LocalDate.of(2026, 10, 3));
            assertThat(response.tripType()).isEqualTo("DAY_TRIP");
            assertThat(response.selectedEvent().eventId()).isEqualTo(CORE_EVENT);
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
            assertThat(event.placeId()).isEqualTo(CORE_EVENT);
            assertThat(event.timeFixed()).isTrue();
            assertThat(event.openTime()).isNull();
        }

        @Test
        void 추천이유와_지도좌표는_값이_있는_항목만_담는다() {
            mapper.addRow(row(103L, 3, "PLACE", "DEV-PL-003", "21:30", 30, null, false));

            DraftResponse response = service.getDraft(OWNER_ID, DRAFT_ID);

            assertThat(response.recommendationReasons()).extracting(reason -> reason.itemId()).containsExactly(101L, 102L);
            assertThat(response.mapPoints()).extracting(point -> point.sequence()).containsExactly(1, 2);
        }

        @Test
        void 없는_초안이면_404() {
            assertErrorCode(() -> service.getDraft(OWNER_ID, 999L), ErrorCode.PLAN_NOT_FOUND);
        }

        @Test
        void 다른_사람_초안이면_403() {
            assertErrorCode(() -> service.getDraft(3L, DRAFT_ID), ErrorCode.FORBIDDEN);
        }

        @Test
        void 이미_저장된_일정은_초안으로_조회되지_않는다() {
            mapper.plans.put(DRAFT_ID, draft(true));
            assertErrorCode(() -> service.getDraft(OWNER_ID, DRAFT_ID), ErrorCode.PLAN_NOT_FOUND);
        }
    }

    @Nested
    class 제목_변경 {

        @Test
        void 앞뒤_공백을_빼고_저장한다() {
            var response = service.updateTitle(OWNER_ID, DRAFT_ID, "  서울에서 보내는 문화 산책  ");

            assertThat(response.title()).isEqualTo("서울에서 보내는 문화 산책");
            assertThat(mapper.titles).containsEntry(DRAFT_ID, "서울에서 보내는 문화 산책");
        }

        @Test
        void 다른_사람_초안은_바꿀_수_없다() {
            assertErrorCode(() -> service.updateTitle(3L, DRAFT_ID, "새 제목"), ErrorCode.FORBIDDEN);
            assertThat(mapper.titles).isEmpty();
        }
    }

    @Nested
    class 항목_일괄_수정 {

        @Test
        void 시간을_바꾸면_항목번호는_그대로_두고_저장한다() {
            DraftItemsResponse response = service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(101L, "PLACE", "DEV-PL-001", "13:00", 90, 1),
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 2)));

            assertThat(response.items()).extracting(PlanItemResponse::itemId).containsExactly(101L, 102L);
            assertThat(response.items().get(0).startTime()).isEqualTo("13:00");
            assertThat(response.items().get(0).durationMin()).isEqualTo(90);
            assertThat(response.mapPoints()).hasSize(2);
        }

        @Test
        void 두_항목의_순서를_맞바꿔도_순번_중복_없이_저장한다() {
            mapper.addRow(row(103L, 3, "PLACE", "DEV-PL-002", "14:00", 60, null, false));
            mapper.rows.put(102L, row(102L, 4, "EVENT", CORE_EVENT, "18:00", 180, "검색 조건 일치", true));

            DraftItemsResponse response = service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(103L, "PLACE", "DEV-PL-002", "12:00", 60, 1),
                    request(101L, "PLACE", "DEV-PL-001", "15:00", 60, 2),
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 3)));

            assertThat(response.items()).extracting(PlanItemResponse::itemId).containsExactly(103L, 101L, 102L);
            assertThat(response.items()).extracting(PlanItemResponse::sequence).containsExactly(1, 2, 3);
        }

        @Test
        void 두_항목의_장소를_맞바꿔도_장소_중복_없이_저장한다() {
            mapper.addRow(row(103L, 3, "PLACE", "DEV-PL-002", "14:00", 60, null, false));
            mapper.rows.put(102L, row(102L, 4, "EVENT", CORE_EVENT, "18:00", 180, "검색 조건 일치", true));

            DraftItemsResponse response = service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(101L, "PLACE", "DEV-PL-002", "12:30", 60, 1),
                    request(103L, "PLACE", "DEV-PL-001", "14:00", 60, 2),
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 3)));

            assertThat(response.items()).extracting(PlanItemResponse::placeId).containsExactly("DEV-PL-002", "DEV-PL-001", CORE_EVENT);
        }

        @Test
        void 장소를_바꾸면_이전_장소의_추천_이유는_지운다() {
            service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(101L, "PLACE", "DEV-PL-003", "12:30", 60, 1),
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 2)));

            assertThat(mapper.rows.get(101L).getPlaceContentId()).isEqualTo("DEV-PL-003");
            assertThat(mapper.rows.get(101L).getAiReason()).isNull();
            assertThat(mapper.rows.get(102L).getAiReason()).isEqualTo("검색 조건 일치");
        }

        @Test
        void 시작_시간이_정해진_행사는_시간을_바꿀_수_없다() {
            assertRule(() -> service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(101L, "PLACE", "DEV-PL-001", "12:30", 60, 1),
                    request(102L, "EVENT", CORE_EVENT, "18:30", 150, 2))), HttpStatus.BAD_REQUEST);
        }

        @Test
        void 시간이_겹치면_400() {
            PlanRuleException exception = assertRule(() -> service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(101L, "PLACE", "DEV-PL-001", "17:30", 60, 1),
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 2))), HttpStatus.BAD_REQUEST);

            assertThat(exception.getMessage()).contains("겹칩니다");
        }

        @Test
        void 방문_순서가_시간_순서와_다르면_400() {
            assertRule(() -> service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(101L, "PLACE", "DEV-PL-001", "12:30", 60, 2),
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 1))), HttpStatus.BAD_REQUEST);
        }

        @Test
        void 없는_항목이면_404_빠진_항목이_있으면_400() {
            assertRule(() -> service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(999L, "PLACE", "DEV-PL-001", "12:30", 60, 1),
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 2))), HttpStatus.NOT_FOUND);
            assertRule(() -> service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 1))), HttpStatus.BAD_REQUEST);
        }

        @Test
        void 없거나_비표출인_장소로_바꾸면_404() {
            assertRule(() -> service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(101L, "PLACE", "NO-SUCH", "12:30", 60, 1),
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 2))), HttpStatus.NOT_FOUND);
            assertRule(() -> service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(101L, "PLACE", "DEV-PL-006", "12:30", 60, 1),
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 2))), HttpStatus.NOT_FOUND);
        }

        @Test
        void 실패하면_아무것도_바뀌지_않는다() {
            assertRule(() -> service.updateItems(OWNER_ID, DRAFT_ID, List.of(
                    request(101L, "PLACE", "DEV-PL-001", "17:30", 60, 1),
                    request(102L, "EVENT", CORE_EVENT, "18:00", 180, 2))), HttpStatus.BAD_REQUEST);

            assertThat(mapper.rows.get(101L).getStartTime()).isEqualTo(LocalTime.of(12, 30));
            assertThat(mapper.rows.get(101L).getSeqOrder()).isEqualTo(1);
        }

        @Test
        void 다른_사람_초안은_수정할_수_없다() {
            assertErrorCode(() -> service.updateItems(3L, DRAFT_ID, List.of(
                    request(101L, "PLACE", "DEV-PL-001", "12:30", 60, 1))), ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    class 항목_추가 {

        @Test
        void 맛집을_추가하면_시간순으로_순번을_다시_매긴다() {
            DraftItemAddResponse response = service.addItem(OWNER_ID, DRAFT_ID, add("PLACE", "DEV-PL-002", "14:00", 60));

            assertThat(response.itemId()).isNotNull();
            assertThat(response.items()).extracting(PlanItemResponse::placeId).containsExactly("DEV-PL-001", "DEV-PL-002", CORE_EVENT);
            assertThat(response.items()).extracting(PlanItemResponse::sequence).containsExactly(1, 2, 3);
            assertThat(mapper.rows.get(response.itemId()).getAiReason()).isNull();
            assertThat(mapper.rows.get(response.itemId()).getTimeFixYn()).isFalse();
        }

        @Test
        void 맨_앞에_추가해도_기존_항목번호는_그대로다() {
            DraftItemAddResponse response = service.addItem(OWNER_ID, DRAFT_ID, add("PLACE", "DEV-PL-003", "11:00", 60));

            assertThat(response.items()).extracting(PlanItemResponse::itemId)
                    .containsExactly(response.itemId(), 101L, 102L);
        }

        @Test
        void 이미_있는_장소면_409() {
            assertRule(() -> service.addItem(OWNER_ID, DRAFT_ID, add("PLACE", "DEV-PL-001", "15:00", 60)), HttpStatus.CONFLICT);
        }

        @Test
        void 시간이_겹치거나_가능_시간을_넘으면_422() {
            assertRule(() -> service.addItem(OWNER_ID, DRAFT_ID, add("PLACE", "DEV-PL-002", "13:00", 60)), HttpStatus.UNPROCESSABLE_ENTITY);
            assertRule(() -> service.addItem(OWNER_ID, DRAFT_ID, add("PLACE", "DEV-PL-002", "21:00", 30)), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @Test
        void 없거나_비표출인_장소면_404() {
            assertRule(() -> service.addItem(OWNER_ID, DRAFT_ID, add("PLACE", "NO-SUCH", "15:00", 60)), HttpStatus.NOT_FOUND);
            assertRule(() -> service.addItem(OWNER_ID, DRAFT_ID, add("PLACE", "DEV-PL-006", "15:00", 60)), HttpStatus.NOT_FOUND);
        }

        @Test
        void 여행_날짜에_하지_않는_행사면_422() {
            mapper.events.put("DEV-EV-004", event("DEV-EV-004", "2026-08-01", "2026-08-15", "15:00"));
            assertRule(() -> service.addItem(OWNER_ID, DRAFT_ID, add("EVENT", "DEV-EV-004", "15:00", 60)), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @Test
        void 시작_시간이_정해진_행사를_다른_시간에_넣으면_422() {
            mapper.events.put("DEV-EV-002", event("DEV-EV-002", "2026-09-10", "2026-11-30", "15:00"));
            assertRule(() -> service.addItem(OWNER_ID, DRAFT_ID, add("EVENT", "DEV-EV-002", "14:00", 60)), HttpStatus.UNPROCESSABLE_ENTITY);

            DraftItemAddResponse response = service.addItem(OWNER_ID, DRAFT_ID, add("EVENT", "DEV-EV-002", "15:00", 60));
            assertThat(mapper.rows.get(response.itemId()).getTimeFixYn()).isTrue();
        }

        @Test
        void 문화행사가_이미_2개면_409() {
            mapper.events.put("DEV-EV-002", event("DEV-EV-002", "2026-09-10", "2026-11-30", "15:00"));
            mapper.events.put("DEV-EV-003", event("DEV-EV-003", "2026-09-10", "2026-11-30", "13:30"));
            service.addItem(OWNER_ID, DRAFT_ID, add("EVENT", "DEV-EV-002", "15:00", 60));

            assertRule(() -> service.addItem(OWNER_ID, DRAFT_ID, add("EVENT", "DEV-EV-003", "13:30", 60)), HttpStatus.CONFLICT);
        }

        @Test
        void 다른_사람_초안에는_추가할_수_없다() {
            assertErrorCode(() -> service.addItem(3L, DRAFT_ID, add("PLACE", "DEV-PL-002", "14:00", 60)), ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    class 항목_삭제 {

        @Test
        void 맛집을_지우면_남은_항목_순번을_다시_매긴다() {
            mapper.addRow(row(103L, 3, "PLACE", "DEV-PL-002", "20:00", 30, null, false));
            mapper.rows.put(102L, row(102L, 2, "EVENT", CORE_EVENT, "18:00", 120, "검색 조건 일치", true));

            service.deleteItem(OWNER_ID, DRAFT_ID, 101L);

            assertThat(mapper.rows).doesNotContainKey(101L);
            assertThat(mapper.rows.get(102L).getSeqOrder()).isEqualTo(1);
            assertThat(mapper.rows.get(103L).getSeqOrder()).isEqualTo(2);
        }

        @Test
        void 핵심_문화행사는_지울_수_없어_409() {
            assertRule(() -> service.deleteItem(OWNER_ID, DRAFT_ID, 102L), HttpStatus.CONFLICT);
            assertThat(mapper.rows).containsKey(102L);
        }

        @Test
        void 추가로_넣은_행사는_지울_수_있다() {
            mapper.events.put("DEV-EV-002", event("DEV-EV-002", "2026-09-10", "2026-11-30", "15:00"));
            Long added = service.addItem(OWNER_ID, DRAFT_ID, add("EVENT", "DEV-EV-002", "15:00", 60)).itemId();

            service.deleteItem(OWNER_ID, DRAFT_ID, added);

            assertThat(mapper.rows).doesNotContainKey(added);
        }

        @Test
        void 없는_항목이면_404() {
            assertRule(() -> service.deleteItem(OWNER_ID, DRAFT_ID, 999L), HttpStatus.NOT_FOUND);
        }

        @Test
        void 다른_사람_초안의_항목은_지울_수_없다() {
            assertErrorCode(() -> service.deleteItem(3L, DRAFT_ID, 101L), ErrorCode.FORBIDDEN);
            assertThat(mapper.rows).containsKey(101L);
        }
    }

    @Nested
    class 저장_확정 {

        @Test
        void 초안을_저장하면_같은_번호와_디데이를_돌려준다() {
            PlanSaveResponse response = service.saveDraft(OWNER_ID, DRAFT_ID);

            assertThat(response.planId()).isEqualTo(DRAFT_ID);
            assertThat(response.title()).isEqualTo("고궁의 밤을 기다리는 하루");
            assertThat(response.visitDate()).isEqualTo(LocalDate.of(2026, 10, 3));
            assertThat(response.dDay()).isEqualTo(7);
            assertThat(mapper.plans.get(DRAFT_ID).getSaveYn()).isTrue();
        }

        @Test
        void 디데이는_당일_0_지난_일정은_음수() {
            mapper.plans.put(DRAFT_ID, draftOn(LocalDate.of(2026, 9, 26)));
            assertThat(service.saveDraft(OWNER_ID, DRAFT_ID).dDay()).isZero();

            mapper.plans.put(DRAFT_ID, draftOn(LocalDate.of(2026, 9, 24)));
            assertThat(service.saveDraft(OWNER_ID, DRAFT_ID).dDay()).isEqualTo(-2);
        }

        @Test
        void 이미_저장된_초안이면_409() {
            service.saveDraft(OWNER_ID, DRAFT_ID);
            assertRule(() -> service.saveDraft(OWNER_ID, DRAFT_ID), HttpStatus.CONFLICT);
        }

        @Test
        void 저장된_뒤에는_초안으로_편집할_수_없다() {
            service.saveDraft(OWNER_ID, DRAFT_ID);
            assertErrorCode(() -> service.updateTitle(OWNER_ID, DRAFT_ID, "새 제목"), ErrorCode.PLAN_NOT_FOUND);
        }

        @Test
        void 항목_규칙에_어긋난_초안은_저장하지_않는다() {
            mapper.rows.put(101L, row(101L, 1, "PLACE", "DEV-PL-001", "17:30", 60, null, false));

            assertRule(() -> service.saveDraft(OWNER_ID, DRAFT_ID), HttpStatus.BAD_REQUEST);
            assertThat(mapper.plans.get(DRAFT_ID).getSaveYn()).isFalse();
        }

        @Test
        void 없는_초안_404_다른_사람_초안_403() {
            assertErrorCode(() -> service.saveDraft(OWNER_ID, 999L), ErrorCode.PLAN_NOT_FOUND);
            assertErrorCode(() -> service.saveDraft(3L, DRAFT_ID), ErrorCode.FORBIDDEN);
            assertThat(mapper.plans.get(DRAFT_ID).getSaveYn()).isFalse();
        }

        private TripPlan draftOn(LocalDate date) {
            TripPlan base = draft(false);
            return TripPlan.builder()
                    .tripPlanId(base.getTripPlanId())
                    .userId(base.getUserId())
                    .anchorContentId(base.getAnchorContentId())
                    .title(base.getTitle())
                    .tripDate(date)
                    .visitStartTime(base.getVisitStartTime())
                    .visitEndTime(base.getVisitEndTime())
                    .saveYn(false)
                    .headcount(base.getHeadcount())
                    .build();
        }
    }

    private static DraftItemAddRequest add(String type, String placeId, String start, int duration) {
        return new DraftItemAddRequest(placeId, type, start, duration, null);
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static PlanRuleException assertRule(Runnable call, HttpStatus expected) {
        try {
            call.run();
        } catch (PlanRuleException exception) {
            assertThat(exception.getStatus()).isEqualTo(expected);
            return exception;
        }
        throw new AssertionError("PlanRuleException(" + expected + ")이 발생해야 합니다.");
    }

    private static DraftItemRequest request(Long itemId, String type, String placeId, String start, int duration, int sequence) {
        return new DraftItemRequest(itemId, type, placeId, start, duration, sequence);
    }

    private static TripPlan draft(boolean saved) {
        return TripPlan.builder()
                .tripPlanId(DRAFT_ID)
                .userId(OWNER_ID)
                .anchorContentId(CORE_EVENT)
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

    private static TripItem row(Long id, int seq, String type, String contentId, String start, int duration,
                                String reason, boolean fixed) {
        return TripItem.builder()
                .tripItemId(id)
                .tripPlanId(DRAFT_ID)
                .seqOrder(seq)
                .itemType(type)
                .eventContentId("EVENT".equals(type) ? contentId : null)
                .placeContentId("PLACE".equals(type) ? contentId : null)
                .startTime(LocalTime.parse(start))
                .durationMin(duration)
                .aiReason(reason)
                .timeFixYn(fixed)
                .build();
    }

    private static ItemTargetView place(String id, String name, boolean display) {
        return ItemTargetView.builder().contentId(id).name(name).displayYn(display).build();
    }

    private static ItemTargetView event(String id, String from, String to, String start) {
        return ItemTargetView.builder()
                .contentId(id)
                .name(id)
                .displayYn(true)
                .eventStartDate(LocalDate.parse(from))
                .eventEndDate(LocalDate.parse(to))
                .eventStartTime(LocalTime.parse(start))
                .build();
    }
}
