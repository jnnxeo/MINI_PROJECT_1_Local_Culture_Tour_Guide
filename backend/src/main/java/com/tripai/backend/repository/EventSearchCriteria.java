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
        long offset,
        List<LocalDate> dates
) {
    /** 날짜 조건 없이 만드는 기존 호출 */
    public EventSearchCriteria(String keywordPattern, List<String> keywordEventTypes, LocalDate monthStart,
                               LocalDate monthEnd, List<String> eventTypes, String district, boolean freeOnly,
                               String sort, int size, long offset) {
        this(keywordPattern, keywordEventTypes, monthStart, monthEnd, eventTypes, district, freeOnly, sort, size,
                offset, List.of());
    }
}
