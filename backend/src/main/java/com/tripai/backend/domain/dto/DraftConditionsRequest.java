package com.tripai.backend.domain.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * API-PLAN-003 요청 — {visitDate, startTime, endTime, companion, foodPreference, transportMode}
 * + 식사 시간대 mealType (docs/07 [제안], BOTH 는 점심·저녁 모두). 보내지 않은 값(null)은 지금 조건을 그대로 둔다.
 * companion 은 DDL 에 저장할 컬럼이 없어 받기만 한다.
  * 끼니별 음식 종류(lunchFoodPreference·dinnerFoodPreference)와 카페 포함(includeCafe)도 받는다 (docs/07 [제안]).
 * foodPreference 만 보내면 점심·저녁 모두 그 음식 종류로 바꾼다.
 */
public record DraftConditionsRequest(
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

		@Pattern(regexp = "ALL|KOREAN|CHINESE|JAPANESE|WESTERN",
				message = "점심 음식 종류는 ALL, KOREAN, CHINESE, JAPANESE, WESTERN 중 하나입니다.")
		String lunchFoodPreference,

		@Pattern(regexp = "ALL|KOREAN|CHINESE|JAPANESE|WESTERN",
				message = "저녁 음식 종류는 ALL, KOREAN, CHINESE, JAPANESE, WESTERN 중 하나입니다.")
		String dinnerFoodPreference,

		Boolean includeCafe,

		// 행사 조건 (docs/07 [제안]) — 보내면 조건에 맞는 행사를 다시 고른다
		@Size(max = 31, message = "방문 가능한 날짜는 31개까지 고를 수 있습니다.")
		List<LocalDate> availableDates,

		@Size(max = 6, message = "행사 분야는 6개까지 고를 수 있습니다.")
		List<String> categories,

		@Size(max = 20, message = "지역 이름이 너무 깁니다.")
		String district,

		Boolean freeYn
) {

	/** 끼니별 음식 종류·카페 없이 부르는 기존 호출 */
	public DraftConditionsRequest(LocalDate visitDate, String startTime, String endTime, String companion,
								  String foodPreference, String mealType, String transportMode) {
		this(visitDate, startTime, endTime, companion, foodPreference, mealType, transportMode, null, null, null);
	}

	/** 행사 조건 없이 부르는 호출 (끼니별 음식 종류·카페까지) */
	public DraftConditionsRequest(LocalDate visitDate, String startTime, String endTime, String companion,
								  String foodPreference, String mealType, String transportMode,
								  String lunchFoodPreference, String dinnerFoodPreference, Boolean includeCafe) {
		this(visitDate, startTime, endTime, companion, foodPreference, mealType, transportMode,
				lunchFoodPreference, dinnerFoodPreference, includeCafe, null, null, null, null);
	}

	/** 행사 조건을 하나라도 보냈는지 */
	public boolean hasEventConditions() {
		return availableDates != null || categories != null || district != null || freeYn != null;
	}
}
