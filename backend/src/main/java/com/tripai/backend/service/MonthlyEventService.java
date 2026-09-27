package com.tripai.backend.service;

import com.tripai.backend.domain.dto.MonthlyEventItemResponse;
import com.tripai.backend.domain.dto.MonthlyEventResponse;
import com.tripai.backend.domain.entity.MonthlyEvent;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.repository.MonthlyEventMapper;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyEventService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MonthlyEventMapper monthlyEventMapper;

    public MonthlyEventService(MonthlyEventMapper monthlyEventMapper) {
        this.monthlyEventMapper = monthlyEventMapper;
    }

    @Transactional(readOnly = true)
    public MonthlyEventResponse getMonthlyEvents(String month, String category, String pageValue, String sizeValue) {
        YearMonth yearMonth = parseMonth(month);
        int page = parseNumber(pageValue);
        int size = parseNumber(sizeValue);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<String> eventTypes = category == null ? null : EventCategory.fromLabel(category).queryTypes();
        long offset = (long) page * size;
        List<MonthlyEventItemResponse> items = monthlyEventMapper.findMonthlyEvents(
                        yearMonth.atDay(1), yearMonth.atEndOfMonth(), eventTypes, size, offset)
                .stream()
                .map(this::toResponse)
                .toList();
        long totalCount = monthlyEventMapper.countMonthlyEvents(
                yearMonth.atDay(1), yearMonth.atEndOfMonth(), eventTypes);
        return new MonthlyEventResponse(yearMonth.toString(), items, totalCount);
    }

    private static YearMonth parseMonth(String value) {
        if (value == null || !value.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        try {
            YearMonth month = YearMonth.parse(value);
            if (month.getYear() < 1000) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            return month;
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

    private MonthlyEventItemResponse toResponse(MonthlyEvent event) {
        return new MonthlyEventItemResponse(
                event.getEventId(),
                event.getTitle(),
                EventCategory.displayName(event.getEventType()),
                event.getDistrict(),
                event.getStartDate(),
                event.getEndDate(),
                event.getFreeYn(),
                event.getFee(),
                event.getImageUrl()
        );
    }
}
