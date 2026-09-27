package com.tripai.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * API-PLAN-007 요청 — {placeId, type, startTime, durationMin, sequence}
 * 방문 순서는 추가 후 시작 시간 순으로 다시 매긴다 (TRIP-004).
 */
public record DraftItemAddRequest(
		@NotBlank(message = "장소를 선택해 주세요.")
		String placeId,

		@NotNull(message = "항목 종류가 필요합니다.")
		@Pattern(regexp = "EVENT|PLACE", message = "항목 종류는 EVENT 또는 PLACE 입니다.")
		String type,

		@NotNull(message = "시작 시간을 입력해 주세요.")
		@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "시작 시간은 HH:mm 형식입니다.")
		String startTime,

		@NotNull(message = "머무는 시간을 입력해 주세요.")
		@Positive(message = "머무는 시간은 0분보다 길어야 합니다.")
		Integer durationMin,

		@Positive(message = "방문 순서는 1부터 시작합니다.")
		Integer sequence
) {
}
