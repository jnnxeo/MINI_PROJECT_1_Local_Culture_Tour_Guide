package com.tripai.backend.external.tour;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class OpeningHoursParserTest {

    @Test
    void HHmm_범위는_영업시각으로_변환한다() {
        OpeningHours result = OpeningHoursParser.parse("매일 11:00~22:30").orElseThrow();

        assertThat(result.openTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(result.closeTime()).isEqualTo(LocalTime.of(22, 30));
        assertThat(result.breakOpenTime()).isNull();
        assertThat(result.breakCloseTime()).isNull();
    }

    @Test
    void 준비시간_표현은_브레이크타임으로_변환한다() {
        OpeningHours result = OpeningHoursParser.parse("11:30~21:00 (준비시간 15:00~18:00)").orElseThrow();

        assertThat(result.breakOpenTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(result.breakCloseTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void 형식이_불명확한_영업시간은_자동추천용_시간으로_변환하지_않는다() {
        assertThat(OpeningHoursParser.parse("상세 운영시간은 전화 문의")).isEmpty();
        assertThat(OpeningHoursParser.parse(null)).isEmpty();
    }

    @Test
    void 자정을_24시로_적은_영업시간은_00시로_정규화한다() {
        OpeningHours result = OpeningHoursParser.parse("- 12:00~24:00<br>- 점심 준비시간 15:00~17:30").orElseThrow();

        assertThat(result.openTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(result.closeTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(result.breakOpenTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(result.breakCloseTime()).isEqualTo(LocalTime.of(17, 30));
    }

    @Test
    void 영업시간이_준비시간_뒤에_와도_준비시간을_영업시간으로_읽지_않는다() {
        OpeningHours result = OpeningHoursParser.parse("월요일~토요일 11:00~24:00 (준비시간 17:00~18:00)").orElseThrow();

        assertThat(result.openTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(result.closeTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(result.breakOpenTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(result.breakCloseTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void 영업시간_원문이_깨져있으면_뒤의_준비시간을_영업시간으로_읽지_않고_전체를_제외한다() {
        assertThat(OpeningHoursParser.parse("- 11:30~22:3 <br>- 준비시간 15:00~17:00")).isEmpty();
    }

    @Test
    void 준비_시간처럼_띄어쓴_표현도_브레이크타임으로_인식한다() {
        OpeningHours result = OpeningHoursParser.parse("10:00~21:00 (준비 시간 15:00~16:00)").orElseThrow();

        assertThat(result.openTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(result.closeTime()).isEqualTo(LocalTime.of(21, 0));
        assertThat(result.breakOpenTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(result.breakCloseTime()).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    void 휴게_시간_휴식_시간도_띄어쓰면_브레이크타임으로_인식한다() {
        assertThat(OpeningHoursParser.parse("11:00~21:00 (휴게 시간 15:00~17:00)").orElseThrow().breakOpenTime())
                .isEqualTo(LocalTime.of(15, 0));
        assertThat(OpeningHoursParser.parse("11:00~21:00 (휴식 시간 15:00~17:00)").orElseThrow().breakOpenTime())
                .isEqualTo(LocalTime.of(15, 0));
    }
}
