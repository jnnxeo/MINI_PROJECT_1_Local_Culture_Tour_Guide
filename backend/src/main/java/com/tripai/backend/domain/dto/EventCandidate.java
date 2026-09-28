package com.tripai.backend.domain.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

/** 일정의 기준 행사 후보 */
@Getter
@Builder
public class EventCandidate {
    // event.event_content_id
    private String eventContentId;
    // 행사 기본 정보
    private String eventName;
    private String eventType;
    private String districtName;
    private String eventPlace;
    private Boolean freeYn;
    // 일정 가능 여부 판단용
    private LocalDate eventStartDate;
    private LocalDate eventEndDate;
    private LocalTime eventStartTime;
    private LocalTime eventEndTime;
    private String dateText; // 시간이 불명확할 때 참고용 원문
}
