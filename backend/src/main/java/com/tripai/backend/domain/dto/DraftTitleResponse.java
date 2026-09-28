package com.tripai.backend.domain.dto;

/** API-PLAN-005 응답 {draftId, title} */
public record DraftTitleResponse(
		Long draftId,
		String title
) {
}
