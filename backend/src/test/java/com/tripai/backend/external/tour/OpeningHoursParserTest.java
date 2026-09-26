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
}
