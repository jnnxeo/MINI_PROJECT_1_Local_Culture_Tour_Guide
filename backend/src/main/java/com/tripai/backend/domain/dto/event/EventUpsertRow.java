package com.tripai.backend.domain.dto.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

/** event 테이블에 넣기 직전의 값. 필드는 DB 컬럼 기준(camelCase). */
@Getter
@Setter
public class EventUpsertRow {
    private String eventContentId;
    private String eventType;
    private String districtName;
    private String eventName;
    private LocalDate eventStartDate;
    private LocalDate eventEndDate;
    private LocalTime eventStartTime;
    private String dateText;
    private String eventPlace;
    private BigDecimal mapx;   // 경도
    private BigDecimal mapy;   // 위도
    private String telNo;
    private String useFee;
    private Boolean freeYn;
    private String overview;
    private String imageUrl;
    private String homepage;
}