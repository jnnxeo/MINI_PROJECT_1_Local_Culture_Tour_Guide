package com.tripai.backend.domain.entity;

import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * trip_item 테이블 — DDL v3.2.1 기준 (저장·수정용).
 * item_type = EVENT 이면 event_content_id, PLACE 이면 place_content_id 만 채운다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripItem {

	private Long tripItemId;
	private Long tripPlanId;
	private Integer seqOrder;
	private String itemType;
	private String eventContentId;
	private String placeContentId;
	private LocalTime startTime;
	private Integer durationMin;
	private String aiReason;
	private Boolean timeFixYn;
}
