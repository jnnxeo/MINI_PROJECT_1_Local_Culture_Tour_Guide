package com.tripai.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** AI 가 만든 일정 안의 행사·식당 한 줄 */
@Getter
@Builder
@NoArgsConstructor
@Setter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanScheduleItem {
    private Integer sequence;           //일정 상 순서
    private String startTime;           //HH:mm
    private String endTime;             //HH:mm
    private String placeType;           // EVENT, RESTAURANT
    // EVENT면 event_content_id, RESTAURANT면 place.content_id
    private String placeContentId;
    private String placeName;
    private String reason;              //추천 이유
}
