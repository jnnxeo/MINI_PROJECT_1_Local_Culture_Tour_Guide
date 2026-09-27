package com.tripai.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripai.backend.domain.dto.MonthlyEventResponse;
import com.tripai.backend.domain.entity.MonthlyEvent;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.repository.MonthlyEventMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MonthlyEventServiceTest {

    @Mock
    private MonthlyEventMapper mapper;

    private MonthlyEventService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MonthlyEventService(mapper);
    }

    @Test
    void monthBoundariesAndPageOffsetArePassedToMapper() {
        when(mapper.findMonthlyEvents(any(), any(), isNull(), anyInt(), anyLong())).thenReturn(List.of());
        when(mapper.countMonthlyEvents(any(), any(), isNull())).thenReturn(12L);

        MonthlyEventResponse result = service.getMonthlyEvents("2028-02", null, "1", "10");

        assertEquals("2028-02", result.month());
        assertEquals(12, result.totalCount());
        assertEquals(List.of(), result.items());
        verify(mapper).findMonthlyEvents(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29), null, 10, 10L);
        verify(mapper).countMonthlyEvents(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29), null);
    }

    @Test
    void exhibitionFilterAcceptsRawAndNormalizedTypes() {
        when(mapper.findMonthlyEvents(any(), any(), any(), anyInt(), anyLong())).thenReturn(List.of());
        service.getMonthlyEvents("2026-09", "전시", "0", "10");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> types = ArgumentCaptor.forClass(List.class);
        verify(mapper).findMonthlyEvents(any(), any(), types.capture(), anyInt(), anyLong());
        assertEquals(List.of("전시", "전시/미술"), types.getValue());
        verify(mapper).countMonthlyEvents(any(), any(), types.capture());
        assertEquals(types.getAllValues().get(0), types.getAllValues().get(1));
    }

    @Test
    void performanceFilterIncludesObservedSeoulSubcategories() {
        when(mapper.findMonthlyEvents(any(), any(), any(), anyInt(), anyLong())).thenReturn(List.of());
        service.getMonthlyEvents("2026-09", "공연", "0", "10");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> types = ArgumentCaptor.forClass(List.class);
        verify(mapper).findMonthlyEvents(any(), any(), types.capture(), anyInt(), anyLong());
        assertEquals(List.of("공연", "국악", "독주/독창회", "무용", "뮤지컬/오페라", "연극", "콘서트", "클래식"), types.getValue());
    }

    @Test
    void responseKeepsUnknownFeeAndNormalizesCategory() {
        MonthlyEvent event = new MonthlyEvent();
        event.setEventId("SEOUL-123");
        event.setTitle("문화행사");
        event.setEventType("전시/미술");
        event.setStartDate(LocalDate.of(2026, 8, 25));
        event.setEndDate(LocalDate.of(2026, 9, 5));
        when(mapper.findMonthlyEvents(any(), any(), isNull(), anyInt(), anyLong())).thenReturn(List.of(event));
        when(mapper.countMonthlyEvents(any(), any(), isNull())).thenReturn(1L);

        MonthlyEventResponse result = service.getMonthlyEvents("2026-09", null, "0", "10");

        assertEquals("전시", result.items().get(0).category());
        assertNull(result.items().get(0).freeYn());
        assertNull(result.items().get(0).fee());
        assertEquals(LocalDate.of(2026, 8, 25), result.items().get(0).startDate());
    }

    @Test
    void invalidInputsAreRejectedBeforeDatabaseLookup() {
        assertThrows(CustomException.class, () -> service.getMonthlyEvents("2026-13", null, "0", "10"));
        assertThrows(CustomException.class, () -> service.getMonthlyEvents("2026-09", "", "0", "10"));
        assertThrows(CustomException.class, () -> service.getMonthlyEvents("2026-09", null, "-1", "10"));
        assertThrows(CustomException.class, () -> service.getMonthlyEvents("2026-09", null, "0", "101"));
        assertThrows(CustomException.class, () -> service.getMonthlyEvents("2026-09", null, "no", "10"));
    }
}
