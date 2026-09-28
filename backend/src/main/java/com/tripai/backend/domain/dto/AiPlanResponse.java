package com.tripai.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** AI 가 만든 일정 (title 은 AI-008 일정 제목) */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiPlanResponse {
    // AI가 만든 일정 제목
    private String title;
    // AI가 생성한 일정
    private List<PlanScheduleItem> schedule;
    // 일정 생성이 어렵거나 조건을 모두 충족하지 못한 경우의 사유
    private List<String> issues;
}
