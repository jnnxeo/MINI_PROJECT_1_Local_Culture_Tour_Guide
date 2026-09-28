package com.tripai.backend.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정 초안을 만들 기준 행사 (API-PLAN-001).
 * 행사 기간·시간으로 일정 시간을 잡고, 좌표로 주변 맛집을 찾는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendEventView {

	private String eventContentId;
	private String eventName;
	// AI 추천 후보 설명용 (행사 분류·자치구·장소·일시 원문·무료 여부)
	private String eventType;
	private String districtName;
	private String eventPlace;
	private String dateText;
	private Boolean freeYn;
	private Boolean displayYn;
	private LocalDate eventStartDate;
	private LocalDate eventEndDate;
	private LocalTime eventStartTime;
	private LocalTime eventEndTime;
	private BigDecimal mapx;
	private BigDecimal mapy;
}
