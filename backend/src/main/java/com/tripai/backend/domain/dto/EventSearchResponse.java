package com.tripai.backend.domain.dto;

import java.util.List;

public record EventSearchResponse(
        List<MonthlyEventItemResponse> items,
        int page,
        long totalCount
) {
}
