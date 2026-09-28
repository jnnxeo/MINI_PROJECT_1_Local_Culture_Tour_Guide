package com.tripai.backend.domain.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * AI 에게 넘기는 사용자 조건 — API-PLAN-001·003 요청에서 만든다.
 * mealType 은 LUNCH·DINNER, null 이면 점심·저녁 모두. 음식 종류는 ALL·KOREAN·CHINESE·JAPANESE·WESTERN.
 * lunchFoodPreference·dinnerFoodPreference 는 끼니별 음식 종류(그 끼니를 고르지 않았으면 null),
 * foodPreference 는 두 끼가 같을 때만 그 값. includeCafe 면 카페(cuisineType CAFE) 1곳을 빈 시간에 넣는다.
 */
@Getter
@Builder
public class PlanGenerateRequest {
    private LocalDate tripDate;             //여행 일자
    private String visitStartTime;          //방문 가능 시작 (HH:mm)
    private String visitEndTime;            //방문 가능 종료 (HH:mm)
    private Integer participants;           //일행 수
    private String transportMode;           //이동수단
    private String foodPreference;          //음식 종류 (두 끼가 같을 때)
    private String lunchFoodPreference;     //점심 음식 종류
    private String dinnerFoodPreference;    //저녁 음식 종류
    private Boolean includeCafe;            //카페 포함 여부
    private String mealType;                //식사 시간대
    private String requiredEventId;         //일정의 기준 행사 id
}
