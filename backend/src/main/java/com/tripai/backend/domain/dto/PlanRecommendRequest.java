package com.tripai.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * API-PLAN-001 요청 — {eventId, visitDate, startTime, endTime, companion, foodPreference, transportMode}
 * mealType·headcount 는 명세에 없는 선택값이다 (docs/07 [제안]).
 * - mealType: 음식점 추천 식사 시간대. 안 보내면 점심·저녁 모두 찾아본다.
 * - headcount: DDL trip_plan.headcount 가 NOT NULL 이라 받는다. 안 보내면 1명.
 * companion 은 DDL 에 저장할 컬럼이 없어 받기만 한다.
 */
public record PlanRecommendRequest(
		@NotBlank(message = "행사를 선택해 주세요.")
		String eventId,

		@NotNull(message = "방문 날짜를 입력해 주세요.")
		LocalDate visitDate,

		@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "시작 시간은 HH:mm 형식입니다.")
		String startTime,

		@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "종료 시간은 HH:mm 형식입니다.")
		String endTime,

		String companion,

		@Pattern(regexp = "ALL|KOREAN|CHINESE|JAPANESE|WESTERN",
				message = "음식 종류는 ALL, KOREAN, CHINESE, JAPANESE, WESTERN 중 하나입니다.")
		String foodPreference,

		@Pattern(regexp = "LUNCH|DINNER", message = "식사 시간은 LUNCH 또는 DINNER 입니다.")
		String mealType,

		@Size(max = 30, message = "이동 방법은 30자 이하로 입력해 주세요.")
		String transportMode,

		@Positive(message = "인원은 1명 이상입니다.")
		Integer headcount
) {
}
