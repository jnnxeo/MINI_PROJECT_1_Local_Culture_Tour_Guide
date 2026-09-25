package com.tripai.backend.domain.dto;

/** 항목별 추천 이유 (AI-007) — trip_item.ai_reason */
public record RecommendationReasonResponse(
		Long itemId,
		String reason
) {
}
