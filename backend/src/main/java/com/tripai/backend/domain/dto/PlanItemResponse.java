package com.tripai.backend.domain.dto;

/**
 * 일정 항목 응답. 08 API 명세는 items:[...] 만 정해 두어서
 * API-PLAN-006 요청 필드 + API-PLACE-001 응답 필드 이름으로 맞췄다. (docs/07 3장)
 * 시각은 "HH:mm" 문자열, 좌표는 lat = mapy(위도), lng = mapx(경도).
 */
public record PlanItemResponse(
		Long itemId,
		String type,
		String placeId,
		Integer sequence,
		String startTime,
		Integer durationMin,
		String name,
		String addr,
		Double lat,
		Double lng,
		String imageUrl,
		String openTime,
		String breakTime,
		String closeTime,
		boolean timeFixed
) {
}
