package com.tripai.backend.service;

import com.tripai.backend.domain.entity.TripPlan;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 메인 AI 추천에서 행사를 고르는 조건 — 방문 가능한 날짜들 + 행사 분야·자치구·무료 여부 (docs/07 [제안]).
 * 초안(trip_plan.search_*)에는 쉼표로 이어 저장한다. 행사를 직접 고른 초안은 조건이 없다(dates 가 null).
 * 분야는 화면 분야명(전시·공연 등)이고, 조회할 때 EventCategory 가 원본 event_type 으로 바꾼다.
 */
record EventConditions(List<LocalDate> dates, List<String> categories, String district, Boolean freeYn) {

    static EventConditions of(TripPlan plan) {
        if (plan.getSearchDates() == null || plan.getSearchDates().isBlank()) {
            return null;
        }
        return new EventConditions(
                split(plan.getSearchDates()).stream().map(LocalDate::parse).toList(),
                split(plan.getSearchCategories()),
                plan.getSearchDistrict(),
                plan.getSearchFreeYn());
    }

    /** 행사 조건으로 만든 초안인지 */
    boolean isEmpty() {
        return dates == null || dates.isEmpty();
    }

    /** 무료 행사만 찾는지 (행사 검색 API 의 freeYn=true 와 같은 뜻) */
    boolean freeOnly() {
        return Boolean.TRUE.equals(freeYn);
    }

    /** 원본 event_type 목록 — 분야를 고르지 않으면 빈 목록(전체) */
    List<String> eventTypes() {
        return categories == null ? List.of() : categories.stream()
                .flatMap(label -> EventCategory.fromLabel(label).queryTypes().stream())
                .distinct()
                .toList();
    }

    String datesText() {
        return dates == null || dates.isEmpty() ? null
                : dates.stream().map(LocalDate::toString).collect(Collectors.joining(","));
    }

    String categoriesText() {
        return categories == null || categories.isEmpty() ? null : String.join(",", categories);
    }

    private static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
    }
}
