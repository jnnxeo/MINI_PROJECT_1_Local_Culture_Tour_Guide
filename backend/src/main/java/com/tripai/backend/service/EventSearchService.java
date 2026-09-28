package com.tripai.backend.service;

import com.tripai.backend.domain.dto.EventSearchResponse;
import com.tripai.backend.domain.dto.MonthlyEventItemResponse;
import com.tripai.backend.domain.entity.MonthlyEvent;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.repository.EventSearchCriteria;
import com.tripai.backend.repository.EventSearchMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventSearchService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_DATES = 31;
    private static final Set<String> SORT_VALUES = Set.of("startDateAsc", "endDateAsc", "titleAsc");

    private final EventSearchMapper eventSearchMapper;

    public EventSearchService(EventSearchMapper eventSearchMapper) {
        this.eventSearchMapper = eventSearchMapper;
    }

    @Transactional(readOnly = true)
    public EventSearchResponse searchEvents(String keyword, String month, List<String> categories,
                                            String district, String freeYn, String sort,
                                            String pageValue, String sizeValue) {
        return searchEvents(keyword, month, categories, district, freeYn, sort, pageValue, sizeValue, null);
    }

    /**
     * dates(YYYY-MM-DD, 여러 개): 메인 날짜 선택 — 고른 날짜 중 하루라도 진행 중인 행사만 (09-28 추가, 최대 31개).
     * 다른 조건과는 AND, 날짜끼리는 OR.
     */
    @Transactional(readOnly = true)
    public EventSearchResponse searchEvents(String keyword, String month, List<String> categories,
                                            String district, String freeYn, String sort,
                                            String pageValue, String sizeValue, List<String> dates) {
        int page = parseNumber(pageValue);
        int size = parseNumber(sizeValue);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || !SORT_VALUES.contains(sort)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        YearMonth yearMonth = parseMonth(month);
        String normalizedKeyword = normalizeOptional(keyword, 200);
        String normalizedDistrict = normalizeOptional(district, 20);
        boolean freeOnly = parseFreeOnly(freeYn);
        List<String> eventTypes = categories == null ? List.of() : categories.stream()
                .flatMap(category -> EventCategory.fromLabel(category).queryTypes().stream())
                .distinct()
                .toList();
        List<String> keywordEventTypes = normalizedKeyword == null
                ? List.of() : EventCategory.queryTypesMatchingKeyword(normalizedKeyword);

        EventSearchCriteria criteria = new EventSearchCriteria(
                normalizedKeyword == null ? null : toLikePattern(normalizedKeyword),
                keywordEventTypes,
                yearMonth == null ? null : yearMonth.atDay(1),
                yearMonth == null ? null : yearMonth.atEndOfMonth(),
                eventTypes,
                normalizedDistrict,
                freeOnly,
                sort,
                size,
                (long) page * size,
                parseDates(dates)
        );

        List<MonthlyEventItemResponse> items = eventSearchMapper.findEvents(criteria).stream()
                .map(this::toResponse)
                .toList();
        long totalCount = eventSearchMapper.countEvents(criteria);
        return new EventSearchResponse(items, page, totalCount);
    }

    private static List<LocalDate> parseDates(List<String> dates) {
        if (dates == null || dates.isEmpty()) {
            return List.of();
        }
        if (dates.size() > MAX_DATES) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        try {
            return dates.stream().map(String::trim).map(LocalDate::parse).distinct().sorted().toList();
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private static YearMonth parseMonth(String month) {
        if (month == null) return null;
        if (!month.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        try {
            YearMonth result = YearMonth.parse(month);
            if (result.getYear() < 1000) throw new CustomException(ErrorCode.INVALID_INPUT);
            return result;
        } catch (DateTimeParseException exception) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private static int parseNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private static String normalizeOptional(String value, int maxLength) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    private static boolean parseFreeOnly(String value) {
        if (value == null || "false".equals(value)) return false;
        if ("true".equals(value)) return true;
        throw new CustomException(ErrorCode.INVALID_INPUT);
    }

    private static String toLikePattern(String keyword) {
        String escaped = keyword.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private MonthlyEventItemResponse toResponse(MonthlyEvent event) {
        return new MonthlyEventItemResponse(
                event.getEventId(), event.getTitle(), EventCategory.displayName(event.getEventType()),
                event.getDistrict(), event.getStartDate(), event.getEndDate(),
                event.getFreeYn(), event.getFee(), event.getImageUrl());
    }
}
