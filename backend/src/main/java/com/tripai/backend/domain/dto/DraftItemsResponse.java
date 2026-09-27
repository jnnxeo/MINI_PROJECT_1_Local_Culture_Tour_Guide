package com.tripai.backend.domain.dto;

import java.util.List;

/** API-PLAN-006 응답 {draftId, items, mapPoints} */
public record DraftItemsResponse(
		Long draftId,
		List<PlanItemResponse> items,
		List<MapPointResponse> mapPoints
) {
}
