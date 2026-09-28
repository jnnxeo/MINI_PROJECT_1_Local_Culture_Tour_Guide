package com.tripai.backend.domain.dto;

import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

/** 일정에 넣을 수 있는 맛집 후보 — 행사장 주변에서 서버가 미리 고른다 */
@Getter
@Builder
public class RestaurantCandidate {
    // place.content_id
    private String contentId;
    private String placeName;
    private String addr;
    // KOREAN·CHINESE·JAPANESE·WESTERN
    private String cuisineType;
    // 영업 가능 여부 판단용 (openTime = closeTime 이면 24시간 영업)
    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalTime breakOpenTime;
    private LocalTime breakCloseTime;
    // 행사장에서의 직선거리(m)
    private Long distanceMeters;
}
