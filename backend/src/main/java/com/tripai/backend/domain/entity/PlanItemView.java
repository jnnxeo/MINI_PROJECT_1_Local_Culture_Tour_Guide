package com.tripai.backend.domain.entity;

import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정 항목 조회용 모델 — trip_item 에 event / place 표시 정보를 붙인 결과.
 * item_type = EVENT 이면 event, PLACE 이면 place 쪽 값이 채워진다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanItemView {

	private Long tripItemId;
	private Integer seqOrder;
	private String itemType;
	private String eventContentId;
	private String placeContentId;
	private LocalTime startTime;
	private Integer durationMin;
	private String aiReason;
	private Boolean timeFixYn;

	private String name;
	private String addr;
	private BigDecimal mapx;
	private BigDecimal mapy;
	private String imageUrl;
	private LocalTime openTime;
	private LocalTime closeTime;
	private LocalTime breakOpenTime;
	private LocalTime breakCloseTime;
}
