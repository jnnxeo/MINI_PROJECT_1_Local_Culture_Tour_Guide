package com.tripai.backend.domain.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Builder
public class RestaurantCandidate {

    // place.content_id
    private String contentId;

    // place.content_type_cd
    // 맛집만 조회한다면 FOOD 타입 코드로 고정된 후보가 들어옴
    private Integer contentTypeCd;

    // 장소 기본 정보
    private String placeName;
    private String addr;
    private String districtName;
    private String activityLabelName;
    private Integer defaultDurationMin;

    // 영업 가능 여부 판단용
    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalTime breakOpenTime;
    private LocalTime breakCloseTime;
    private String businessHoursText;

    // 지도·화면 표시용
    private BigDecimal mapx; // 경도
    private BigDecimal mapy; // 위도
    private String imageUrl;
}
