package com.tripai.backend.domain.dto;

import java.util.List;

/** API-PLAN-007 응답 {itemId, items, mapPoints} */
public record DraftItemAddResponse(
		Long itemId,
		List<PlanItemResponse> items,
		List<MapPointResponse> mapPoints
) {
}
