package com.tripai.backend.domain.dto;

/**
 * API-PLACE-001 항목 {placeId, name, addr, lat, lng, openTime, breakTime, closeTime, firstMenu, imageUrl, distance}
 * - lat = mapy(위도), lng = mapx(경도), 시각은 "HH:mm", breakTime 은 "HH:mm~HH:mm"
 * - distance: 기준 위치에서 직선거리(m, 반올림). 실제 이동 경로 거리가 아님
 * - firstMenu: DDL v3.2.1 place 에 컬럼이 없어 지금은 null (ERD 반영 예정 항목)
 */
public record RestaurantResponse(
		String placeId,
		String name,
		String addr,
		Double lat,
		Double lng,
		String openTime,
		String breakTime,
		String closeTime,
		String firstMenu,
		String imageUrl,
		Long distance
) {
}
