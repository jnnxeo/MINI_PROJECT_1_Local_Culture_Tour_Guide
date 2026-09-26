package com.tripai.backend.external.tour;

import java.time.LocalTime;

/** TourAPI 영업시간 원문에서 안전하게 읽어 낸 하루 영업·브레이크 구간. */
public record OpeningHours(
        LocalTime openTime,
        LocalTime closeTime,
        LocalTime breakOpenTime,
        LocalTime breakCloseTime
) {
}
