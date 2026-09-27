package com.tripai.backend.domain.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;

//해당 DTO는 프론트엔드에서 백엔드로 전달된 여행 일정과 관련된 조건 정보를 가집니다.

@Getter 
public class PlanGenerateRequest {
    private LocalDate tripDate;             //여행 일자
    private Integer participants;           //일행 수  
    private List<String> transportation;    //이동수단
    private List<String> interests;         //관심사   
    private List<String> eventTypes;        //문화행사 유형 
    private List<String> districts;         //구        
    private Boolean freeOnly;               //무료, 유료 여부
    private String requiredEventId;         //만약 특정 행사를 기준으로 일정 생성한 경우 해당 행사의 id
}
