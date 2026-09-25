package com.tripai.backend.domain.dto;

import jakarta.validation.constraints.NotNull;

/** API-PLAN-009 요청 {draftId} */
public record PlanSaveRequest(
		@NotNull(message = "저장할 초안을 선택해 주세요.")
		Long draftId
) {
}
