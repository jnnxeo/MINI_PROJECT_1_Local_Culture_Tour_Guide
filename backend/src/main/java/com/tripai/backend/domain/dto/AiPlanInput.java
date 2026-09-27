package com.tripai.backend.domain.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder 
public class AiPlanInput {

    // 프론트엔드에서 받은 조건
    private PlanGenerateRequest userConditions;

    // 반경·날짜·필터 조건으로 DB에서 조회한 후보 ()
    private List<EventCandidate> eventCandidates;
    private List<RestaurantCandidate> restaurantCandidates;
}