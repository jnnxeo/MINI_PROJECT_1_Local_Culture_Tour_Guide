package com.tripai.backend.domain.dto;

import java.util.List;

public record MonthlyEventResponse(
        String month,
        List<MonthlyEventItemResponse> items,
        long totalCount
) {
}
