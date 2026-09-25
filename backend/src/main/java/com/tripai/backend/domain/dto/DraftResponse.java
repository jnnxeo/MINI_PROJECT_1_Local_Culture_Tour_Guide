package com.tripai.backend.domain.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * API-PLAN-002 저장 전 일정 초안 조회 응답.
 * 명세 필드: draftId, title, visitDate, tripType, items, mapPoints
 * 화면(조건 수정 팝업·추천 이유)에 필요해 docs/07 3장에 정리한 selectedEvent, conditions,
 * recommendationReasons 를 함께 내려준다.
 */
public record DraftResponse(
		Long draftId,
		String title,
		LocalDate visitDate,
		String tripType,
		SelectedEvent selectedEvent,
		Conditions conditions,
		List<PlanItemResponse> items,
		List<RecommendationReasonResponse> recommendationReasons,
		List<MapPointResponse> mapPoints
) {

	public record SelectedEvent(
			String eventId,
			String title
	) {
	}

	/** API-PLAN-003 요청과 같은 필드. companion·foodPreference 는 DDL 에 컬럼이 없어 null */
	public record Conditions(
			LocalDate visitDate,
			String startTime,
			String endTime,
			String companion,
			String foodPreference,
			String transportMode
	) {
	}
}
