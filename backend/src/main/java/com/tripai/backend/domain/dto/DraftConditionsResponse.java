package com.tripai.backend.domain.dto;

/** API-PLAN-003 응답 — {draftId, conditions} */
public record DraftConditionsResponse(
		Long draftId,
		DraftResponse.Conditions conditions
) {
}
