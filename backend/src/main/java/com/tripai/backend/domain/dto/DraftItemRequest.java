package com.tripai.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** API-PLAN-006 항목 한 줄 — {itemId, type, placeId, startTime, durationMin, sequence} */
public record DraftItemRequest(
		@NotNull(message = "항목 번호가 필요합니다.")
		Long itemId,

		@NotNull(message = "항목 종류가 필요합니다.")
		@Pattern(regexp = "EVENT|PLACE", message = "항목 종류는 EVENT 또는 PLACE 입니다.")
		String type,

		@NotBlank(message = "장소를 선택해 주세요.")
		String placeId,

		@NotNull(message = "시작 시간을 입력해 주세요.")
		@Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "시작 시간은 HH:mm 형식입니다.")
		String startTime,

		@NotNull(message = "머무는 시간을 입력해 주세요.")
		@Positive(message = "머무는 시간은 0분보다 길어야 합니다.")
		Integer durationMin,

		@NotNull(message = "방문 순서가 필요합니다.")
		@Positive(message = "방문 순서는 1부터 시작합니다.")
		Integer sequence
) {
}
