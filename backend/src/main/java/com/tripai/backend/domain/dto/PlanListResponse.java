package com.tripai.backend.domain.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlanListResponse {

    private final List<PlanSummary> plans;
    private final int page;
    private final long totalCount;
}
