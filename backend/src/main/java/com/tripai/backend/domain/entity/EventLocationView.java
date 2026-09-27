package com.tripai.backend.domain.entity;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 맛집 후보 검색 기준이 되는 행사의 위치 (mapx = 경도, mapy = 위도) */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLocationView {

	private String eventContentId;
	private BigDecimal mapx;
	private BigDecimal mapy;
	private Boolean displayYn;
}
