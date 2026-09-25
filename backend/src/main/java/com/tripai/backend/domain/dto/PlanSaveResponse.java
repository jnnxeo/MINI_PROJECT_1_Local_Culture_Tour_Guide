package com.tripai.backend.domain.dto;

import java.time.LocalDate;

/**
 * API-PLAN-009 응답 {planId, title, visitDate, dDay}
 * dDay = 여행 날짜 - 오늘 (한국 시간). 0 이면 당일, 음수면 지난 일정 (MY-003)
 */
public record PlanSaveResponse(
		Long planId,
		String title,
		LocalDate visitDate,
		long dDay
) {
}
