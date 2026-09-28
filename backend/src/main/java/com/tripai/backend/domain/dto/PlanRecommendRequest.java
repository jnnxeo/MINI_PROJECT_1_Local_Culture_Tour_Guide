package com.tripai.backend.domain.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * API-PLAN-001 요청 — {eventId, visitDate, startTime, endTime, companion, foodPreference, transportMode}
 * mealType·headcount 는 명세에 없는 선택값이다 (docs/07 [제안]).
 * - mealType: 음식점 추천 식사 시간대 LUNCH·DINNER. 안 보내거나 BOTH 면 점심·저녁 모두 찾아본다.
 * - headcount: DDL trip_plan.headcount 가 NOT NULL 이라 받는다. 안 보내면 1명.
 * companion 은 DDL 에 저장할 컬럼이 없어 받기만 한다.
 * 메인 AI 추천 모달·조건 수정 팝업용 선택값 (docs/07 [제안], 04 마이그레이션에 저장):
 * - lunchFoodPreference·dinnerFoodPreference: 점심·저녁 음식 종류. 안 보내면 foodPreference(없으면 ALL)를 쓴다.
 * - includeCafe: true 면 식사·행사와 겹치지 않는 빈 시간에 행사장 근처 카페(카페/전통찻집) 1곳을 넣는다.
 * 메인 AI 추천(eventId 없음): availableDates(필수)·categories·district·freeYn 으로 서버가 행사와 방문일을 고른다.
 */
public record PlanRecommendRequest(
		// 행사 상세처럼 행사를 직접 고른 경우. 없으면 availableDates 와 행사 조건으로 서버가 행사를 고른다
		@Size(max = 100, message = "행사 ID가 너무 깁니다.")
		String eventId,

		// eventId 가 있을 때의 방문 날짜 (서비스에서 필수 확인)
		LocalDate visitDate,

		@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "시작 시간은 HH:mm 형식입니다.")
		String startTime,

		@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "종료 시간은 HH:mm 형식입니다.")
		String endTime,

		String companion,

		@Pattern(regexp = "ALL|KOREAN|CHINESE|JAPANESE|WESTERN",
				message = "음식 종류는 ALL, KOREAN, CHINESE, JAPANESE, WESTERN 중 하나입니다.")
		String foodPreference,

		@Pattern(regexp = "BOTH|LUNCH|DINNER", message = "식사 시간은 BOTH, LUNCH, DINNER 중 하나입니다.")
		String mealType,

		@Size(max = 30, message = "이동 방법은 30자 이하로 입력해 주세요.")
		String transportMode,

		@Positive(message = "인원은 1명 이상입니다.")
		Integer headcount,

		@Pattern(regexp = "ALL|KOREAN|CHINESE|JAPANESE|WESTERN",
				message = "점심 음식 종류는 ALL, KOREAN, CHINESE, JAPANESE, WESTERN 중 하나입니다.")
		String lunchFoodPreference,

		@Pattern(regexp = "ALL|KOREAN|CHINESE|JAPANESE|WESTERN",
				message = "저녁 음식 종류는 ALL, KOREAN, CHINESE, JAPANESE, WESTERN 중 하나입니다.")
		String dinnerFoodPreference,

		Boolean includeCafe,

		// 메인 AI 추천 모달 — 방문 가능한 날짜(여러 개)와 행사 조건 (행사 검색 API와 같은 값, docs/07 [제안])
		@Size(max = 31, message = "방문 가능한 날짜는 31개까지 고를 수 있습니다.")
		List<LocalDate> availableDates,

		@Size(max = 6, message = "행사 분야는 6개까지 고를 수 있습니다.")
		List<String> categories,

		@Size(max = 20, message = "지역 이름이 너무 깁니다.")
		String district,

		Boolean freeYn
) {

	/** 끼니별 음식 종류·카페 없이 부르는 기존 호출 (행사 상세 #45 등) */
	public PlanRecommendRequest(String eventId, LocalDate visitDate, String startTime, String endTime, String companion,
								String foodPreference, String mealType, String transportMode, Integer headcount) {
		this(eventId, visitDate, startTime, endTime, companion, foodPreference, mealType, transportMode, headcount,
				null, null, null);
	}

	/** 행사 조건 없이 부르는 호출 (끼니별 음식 종류·카페까지) */
	public PlanRecommendRequest(String eventId, LocalDate visitDate, String startTime, String endTime, String companion,
								String foodPreference, String mealType, String transportMode, Integer headcount,
								String lunchFoodPreference, String dinnerFoodPreference, Boolean includeCafe) {
		this(eventId, visitDate, startTime, endTime, companion, foodPreference, mealType, transportMode, headcount,
				lunchFoodPreference, dinnerFoodPreference, includeCafe, null, null, null, null);
	}
}
