package com.tripai.backend.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter 
@NoArgsConstructor 
public class PlanScheduleItem {

    private Integer sequence;           //일정 상 순서
    private String startTime;           
    private String endTime;

    private String placeType; // EVENT, RESTAURANT

    // EVENT면 event_content_id, RESTAURANT면 place.content_id
    private String placeContentId;

    private String placeName;
    private String reason;
    private String transportation;      //이전 위치에서 해당 위치까지 이동 방식
    private String note;                //추천 이유
}