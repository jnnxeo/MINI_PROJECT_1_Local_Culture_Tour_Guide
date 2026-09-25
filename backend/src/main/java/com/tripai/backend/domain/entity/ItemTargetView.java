package com.tripai.backend.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정에 넣으려는 행사·맛집이 실제로 있는지 확인할 때 쓰는 조회 결과.
 * 맛집은 날짜·시각 값이 비어 있다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemTargetView {

	private String contentId;
	private String name;
	private Boolean displayYn;
	private LocalDate eventStartDate;
	private LocalDate eventEndDate;
	private LocalTime eventStartTime;
}
