package com.tripai.backend.domain.dto;

import java.util.List;

/** API-PLACE-001 응답 {items} — 결과가 없으면 빈 배열 (오류와 구분, UX-003) */
public record RestaurantSearchResponse(
		List<RestaurantResponse> items
) {
}
