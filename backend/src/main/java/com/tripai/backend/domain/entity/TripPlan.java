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
	private Integer headcount;
	private LocalDateTime createdAt;
	private LocalDateTime updAt;

	// 조회 시 event 테이블에서 함께 가져오는 기준 행사명
	private String anchorEventName;
}
