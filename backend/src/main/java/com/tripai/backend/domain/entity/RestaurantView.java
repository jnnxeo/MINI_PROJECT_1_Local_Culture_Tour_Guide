package com.tripai.backend.domain.entity;

import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 맛집 후보 조회 결과 — place(음식점 39) 한 행 + 기준 위치에서의 직선거리(m) */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantView {

	private String contentId;
	private String placeName;
	private String addr;
	private BigDecimal mapx;
	private BigDecimal mapy;
	private String imageUrl;
	private LocalTime openTime;
	private LocalTime closeTime;
	private LocalTime breakOpenTime;
	private LocalTime breakCloseTime;
	private Double distance;
}
