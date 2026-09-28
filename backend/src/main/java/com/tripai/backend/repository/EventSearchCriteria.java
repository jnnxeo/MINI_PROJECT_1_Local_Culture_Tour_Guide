package com.tripai.backend.repository;

import java.time.LocalDate;
import java.util.List;

public record EventSearchCriteria(
        String keywordPattern,
        List<String> keywordEventTypes,
        LocalDate monthStart,
        LocalDate monthEnd,
        List<String> eventTypes,
        String district,
        boolean freeOnly,
        String sort,
        int size,
        long offset
) {
}
