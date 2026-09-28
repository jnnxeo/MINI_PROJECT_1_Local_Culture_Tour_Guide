package com.tripai.backend.service;

import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** 서울시 문화행사 CODENAME과 화면 분야의 대응표. 원본 값은 event_type에 그대로 저장한다. */
public enum EventCategory {
    EXHIBITION("전시", List.of("전시/미술")),
    PERFORMANCE("공연", List.of("국악", "독주/독창회", "무용", "뮤지컬/오페라", "연극", "콘서트", "클래식")),
    TRADITIONAL("전통문화", List.of("축제-전통/역사")),
    FESTIVAL("축제", List.of("축제-관광/체육", "축제-기타", "축제-문화/예술", "축제-시민화합", "축제-자연/경관")),
    EDUCATION("교육·체험", List.of("교육/체험")),
    OTHER("기타", List.of("영화", "기타"));

    private final String label;
    private final List<String> sourceTypes;

    EventCategory(String label, List<String> sourceTypes) {
        this.label = label;
        this.sourceTypes = sourceTypes;
    }

    public static EventCategory fromLabel(String label) {
        return Arrays.stream(values())
                .filter(category -> category.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
    }

    public static String displayName(String sourceType) {
        return Arrays.stream(values())
                .filter(category -> category.label.equals(sourceType) || category.sourceTypes.contains(sourceType))
                .map(category -> category.label)
                .findFirst()
                .orElse(sourceType);
    }

    public List<String> queryTypes() {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(label), sourceTypes.stream()).toList();
    }

    public static List<String> queryTypesMatchingKeyword(String keyword) {
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(category -> category.label.toLowerCase(Locale.ROOT).contains(normalized))
                .flatMap(category -> category.queryTypes().stream())
                .distinct()
                .toList();
    }
}
