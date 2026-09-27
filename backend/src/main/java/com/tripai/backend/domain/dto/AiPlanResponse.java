package com.tripai.backend.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AiPlanResponse {

    // AI가 생성한 일정
    private List<PlanScheduleItem> schedule;

    // 일정 생성이 어렵거나 조건을 모두 충족하지 못한 경우의 사유
    private List<String> issues;
}