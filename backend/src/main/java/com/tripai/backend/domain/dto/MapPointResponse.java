package com.tripai.backend.domain.dto;

/** 지도 표시용 방문 순서 좌표 (MAP-001, TRIP-008) */
public record MapPointResponse(
		Integer sequence,
		Double lat,
		Double lng
) {
}
