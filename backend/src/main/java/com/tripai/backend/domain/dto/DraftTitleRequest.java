package com.tripai.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** API-PLAN-005 요청 — 빈 제목, 100자 초과는 400 (DDL title VARCHAR(100)) */
public record DraftTitleRequest(
		@NotBlank(message = "일정 이름을 입력해 주세요.")
		@Size(max = 100, message = "일정 이름은 100자 이하로 입력해 주세요.")
		String title
) {
}
