package com.tripai.backend.domain.dto;

import java.time.LocalDate;

public record MonthlyEventItemResponse(
        String eventId,
        String title,
        String category,
        String district,
        LocalDate startDate,
        LocalDate endDate,
        Boolean freeYn,
        String fee,
        String imageUrl
) {
}
