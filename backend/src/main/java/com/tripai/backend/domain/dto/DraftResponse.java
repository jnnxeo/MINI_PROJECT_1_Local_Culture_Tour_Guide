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

	/** startDate·endDate 는 행사 기간 — 조건 수정에서 방문 날짜를 행사 기간 안으로만 고르게 한다 (docs/07 [제안]) */
	public record SelectedEvent(
			String eventId,
			String title,
			LocalDate startDate,
			LocalDate endDate
	) {
	}

	/**
	 * API-PLAN-003 요청과 같은 필드 + 식사 시간대(mealType, docs/07 [제안]).
	 * companion 은 DDL 에 컬럼이 없어 null. foodPreference·mealType 은 03 마이그레이션 컬럼에서 읽는다.
	 * lunchFoodPreference·dinnerFoodPreference·includeCafe 는 04 마이그레이션 컬럼 — 끼니별 값이 없던 초안은
	 * foodPreference 를 두 끼 모두에 채워 준다. 점심·저녁 음식 종류가 다르면 foodPreference 는 null.
	 * availableDates·categories·district·freeYn 은 05 마이그레이션 — 행사를 조건으로 고른 초안만 값이 있다(직접 고른 초안은 null).
	 */
	public record Conditions(
			LocalDate visitDate,
			String startTime,
			String endTime,
			String companion,
			String foodPreference,
			String mealType,
			String transportMode,
			String lunchFoodPreference,
			String dinnerFoodPreference,
			boolean includeCafe,
			List<LocalDate> availableDates,
			List<String> categories,
			String district,
			Boolean freeYn
	) {
	}
}
