package com.tripai.backend.domain.dto.plan;

import java.time.LocalDate;
import java.util.List;

public record EventPlanDetailResponse(
        Long planId,
        String title,
        LocalDate tripDate,
        String visitStartTime,
        String visitEndTime,
        int headcount,
        String transportMode,
        List<String> interests,
        String anchorEventId,
        boolean saved,
        boolean aiGenerated,
        Long dDay,
        List<EventPlanItemResponse> items,
        List<String> warnings
) {}
