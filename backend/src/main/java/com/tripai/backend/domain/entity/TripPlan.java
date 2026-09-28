package com.tripai.backend.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * trip_plan 테이블 — DDL v3.2.1 기준.
 * save_yn = FALSE 이면 저장 전 초안(draft), TRUE 이면 저장한 일정.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPlan {

	private Long tripPlanId;
	private Long userId;
	private String anchorContentId;
	private String title;
	private LocalDate tripDate;
	private LocalTime visitStartTime;
	private LocalTime visitEndTime;
	private Boolean saveYn;
	private Boolean aiYn;
	private String transportMd;
	// database/schema/03_trip_plan_food_condition_v3.2.3.sql 에서 추가한 추천 조건
	private String foodPreference;
	private String mealType;
	// database/schema/04_trip_plan_meal_food_cafe_v3.2.4.sql 에서 추가한 끼니별 음식 종류·카페 포함 (null 이면 foodPreference)
	private String lunchFoodPreference;
	private String dinnerFoodPreference;
	private Boolean cafeYn;
	// database/schema/05_trip_plan_event_condition_v3.2.5.sql — 행사를 조건으로 고른 초안의 검색 조건 (행사를 직접 고른 초안은 null)
	private String searchDates;       // YYYY-MM-DD 쉼표 구분
	private String searchCategories;  // 화면 분야명 쉼표 구분
	private String searchDistrict;
	private Boolean searchFreeYn;
	private Integer headcount;
	private LocalDateTime createdAt;
	private LocalDateTime updAt;

	// 조회 시 event 테이블에서 함께 가져오는 기준 행사명
	private String anchorEventName;
	private LocalDate anchorStartDate;
	private LocalDate anchorEndDate;
}
