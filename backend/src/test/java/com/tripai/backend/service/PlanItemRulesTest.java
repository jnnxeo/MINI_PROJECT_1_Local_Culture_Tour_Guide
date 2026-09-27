package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tripai.backend.service.PlanItemRules.Candidate;
import com.tripai.backend.service.PlanItemRules.Violation;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlanItemRulesTest {

    private static final String CORE = "EV-1";
    private static final LocalTime OPEN = LocalTime.of(11, 0);
    private static final LocalTime CLOSE = LocalTime.of(21, 0);

    @Test
    void 정상_일정은_통과한다() {
        assertThat(check(List.of(place("PL-1", "12:30", 60, 1), place("PL-2", "14:00", 60, 2), event(CORE, "18:00", 180, 3)), true))
                .isEmpty();
    }

    @Test
    void 머무는_시간이_0분이거나_자정을_넘기면_막는다() {
        assertThat(violation(List.of(place("PL-1", "12:30", 0, 1), event(CORE, "18:00", 180, 2)))).isEqualTo(Violation.INVALID_DURATION);
        assertThat(PlanItemRules.check(List.of(place("PL-1", "23:30", 60, 1), event(CORE, "18:00", 180, 2)), CORE, null, null, false))
                .get().extracting(PlanItemRules.Result::violation).isEqualTo(Violation.INVALID_DURATION);
    }

    @Test
    void 같은_장소가_두_번_있으면_막는다() {
        assertThat(violation(List.of(place("PL-1", "12:00", 60, 1), place("PL-1", "14:00", 60, 2), event(CORE, "18:00", 180, 3))))
                .isEqualTo(Violation.DUPLICATE);
    }

    @Test
    void 핵심_문화행사가_빠지면_막는다() {
        assertThat(violation(List.of(place("PL-1", "12:30", 60, 1), event("EV-2", "18:00", 180, 2))))
                .isEqualTo(Violation.CORE_EVENT_MISSING);
    }

    @Test
    void 문화행사는_최대_2개() {
        assertThat(violation(List.of(event(CORE, "11:00", 60, 1), event("EV-2", "13:00", 60, 2), event("EV-3", "15:00", 60, 3))))
                .isEqualTo(Violation.EVENT_LIMIT);
    }

    @Test
    void 방문_가능_시간을_벗어나면_막는다() {
        assertThat(violation(List.of(place("PL-1", "10:30", 60, 1), event(CORE, "18:00", 180, 2))))
                .isEqualTo(Violation.OUT_OF_RANGE);
        assertThat(violation(List.of(place("PL-1", "12:00", 60, 1), event(CORE, "18:30", 180, 2))))
                .isEqualTo(Violation.OUT_OF_RANGE);
    }

    @Test
    void 시간이_겹치면_막고_어느_항목인지_알려준다() {
        Optional<PlanItemRules.Result> result = check(
                List.of(place("PL-1", "17:30", 60, 1), event(CORE, "18:00", 180, 2)), false);

        assertThat(result).get().extracting(PlanItemRules.Result::violation).isEqualTo(Violation.TIME_OVERLAP);
        assertThat(result.get().message()).contains("PL-1(17:30–18:30)").contains(CORE);
    }

    @Test
    void 끝나는_시각과_다음_시작이_같으면_겹침이_아니다() {
        assertThat(check(List.of(place("PL-1", "17:00", 60, 1), event(CORE, "18:00", 180, 2)), true)).isEmpty();
    }

    @Test
    void 순서가_빠지거나_시간_순서와_다르면_막는다() {
        assertThat(check(List.of(place("PL-1", "12:30", 60, 1), event(CORE, "18:00", 180, 3)), true))
                .get().extracting(PlanItemRules.Result::violation).isEqualTo(Violation.SEQUENCE_ORDER);
        assertThat(check(List.of(place("PL-1", "12:30", 60, 2), event(CORE, "18:00", 180, 1)), true))
                .get().extracting(PlanItemRules.Result::violation).isEqualTo(Violation.SEQUENCE_ORDER);
    }

    @Test
    void 순서_검사를_끄면_순서는_보지_않는다() {
        assertThat(check(List.of(place("PL-1", "12:30", 60, 2), event(CORE, "18:00", 180, 1)), false)).isEmpty();
    }

    @Test
    void 시간순으로_순번을_다시_매긴다() {
        List<Candidate> result = PlanItemRules.resequenceByTime(
                List.of(event(CORE, "18:00", 180, 1), place("PL-1", "12:30", 60, 3), place("PL-2", "14:00", 60, 2)));

        assertThat(result).extracting(Candidate::contentId).containsExactly("PL-1", "PL-2", CORE);
        assertThat(result).extracting(Candidate::sequence).containsExactly(1, 2, 3);
    }

    private static Optional<PlanItemRules.Result> check(List<Candidate> items, boolean checkSequence) {
        return PlanItemRules.check(items, CORE, OPEN, CLOSE, checkSequence);
    }

    private static Violation violation(List<Candidate> items) {
        return check(items, false).map(PlanItemRules.Result::violation).orElse(null);
    }

    private static Candidate place(String id, String start, int duration, int sequence) {
        return new Candidate("PLACE", id, id, LocalTime.parse(start), duration, sequence);
    }

    private static Candidate event(String id, String start, int duration, int sequence) {
        return new Candidate("EVENT", id, id, LocalTime.parse(start), duration, sequence);
    }
}
