package com.tripai.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tripai.backend.domain.dto.EventSearchResponse;
import com.tripai.backend.domain.entity.MonthlyEvent;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.repository.EventSearchCriteria;
import com.tripai.backend.repository.EventSearchMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class EventSearchServiceTest {

    @Mock
    private EventSearchMapper mapper;

    private EventSearchService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EventSearchService(mapper);
    }

    @Test
    void keywordSearchDoesNotAddMonthAndMatchesDisplayCategory() {
        when(mapper.findEvents(any())).thenReturn(List.of());
        service.searchEvents(" 공연 ", null, null, null, null, "startDateAsc", "0", "10");

        ArgumentCaptor<EventSearchCriteria> criteria = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(mapper).findEvents(criteria.capture());
        assertEquals("%공연%", criteria.getValue().keywordPattern());
        assertTrue(criteria.getValue().keywordEventTypes().contains("콘서트"));
        assertNull(criteria.getValue().monthStart());
        assertNull(criteria.getValue().monthEnd());
        verify(mapper).countEvents(criteria.getValue());
    }

    @Test
    void literalWildcardCharactersAreEscaped() {
        when(mapper.findEvents(any())).thenReturn(List.of());
        service.searchEvents("A_%!", null, null, null, null, "startDateAsc", "0", "10");

        ArgumentCaptor<EventSearchCriteria> criteria = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(mapper).findEvents(criteria.capture());
        assertEquals("%a!_!%!!%", criteria.getValue().keywordPattern());
    }

    @Test
    void categoriesAreCombinedAndDefaultFeeHasNoFilter() {
        when(mapper.findEvents(any())).thenReturn(List.of());
        service.searchEvents(null, "2028-02", List.of("전시", "공연", "전시"),
                "종로구", null, "endDateAsc", "2", "10");

        ArgumentCaptor<EventSearchCriteria> criteria = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(mapper).findEvents(criteria.capture());
        assertEquals(LocalDate.of(2028, 2, 1), criteria.getValue().monthStart());
        assertEquals(LocalDate.of(2028, 2, 29), criteria.getValue().monthEnd());
        assertTrue(criteria.getValue().eventTypes().contains("전시/미술"));
        assertTrue(criteria.getValue().eventTypes().contains("콘서트"));
        assertEquals(criteria.getValue().eventTypes().size(),
                criteria.getValue().eventTypes().stream().distinct().count());
        assertFalse(criteria.getValue().freeOnly());
        assertEquals("종로구", criteria.getValue().district());
        assertEquals(20L, criteria.getValue().offset());
        verify(mapper).countEvents(criteria.getValue());
    }

    @Test
    void responseUsesCardFieldsAndFullCount() {
        MonthlyEvent event = new MonthlyEvent();
        event.setEventId("SEOUL-123");
        event.setTitle("현대미술 전시");
        event.setEventType("전시/미술");
        event.setStartDate(LocalDate.of(2026, 9, 1));
        event.setEndDate(LocalDate.of(2026, 9, 30));
        when(mapper.findEvents(any())).thenReturn(List.of(event));
        when(mapper.countEvents(any())).thenReturn(25L);

        EventSearchResponse result = service.searchEvents(null, null, null, null,
                "true", "titleAsc", "1", "10");

        assertEquals(1, result.page());
        assertEquals(25, result.totalCount());
        assertEquals("전시", result.items().get(0).category());
        assertNull(result.items().get(0).freeYn());
        ArgumentCaptor<EventSearchCriteria> criteria = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(mapper).findEvents(criteria.capture());
        assertTrue(criteria.getValue().freeOnly());
    }

    @Test
    void invalidInputsAreRejectedBeforeDatabaseLookup() {
        assertThrows(CustomException.class, () -> service.searchEvents(null, "2026-13", null, null, null, "startDateAsc", "0", "10"));
        assertThrows(CustomException.class, () -> service.searchEvents(null, null, List.of("알수없음"), null, null, "startDateAsc", "0", "10"));
        assertThrows(CustomException.class, () -> service.searchEvents(null, null, null, null, null, "invalid", "0", "10"));
        assertThrows(CustomException.class, () -> service.searchEvents(null, null, null, null, null, "startDateAsc", "-1", "10"));
        assertThrows(CustomException.class, () -> service.searchEvents(null, null, null, null, null, "startDateAsc", "0", "101"));
        assertThrows(CustomException.class, () -> service.searchEvents(null, null, null, null, "yes", "startDateAsc", "0", "10"));
        assertThrows(CustomException.class, () -> service.searchEvents("  ", null, null, null, null, "startDateAsc", "0", "10"));
        verifyNoInteractions(mapper);
    }

    @Test
    void selectedDatesAreSortedWithoutDuplicatesAndInvalidDateIsRejected() {
        // 메인 날짜 선택(09-28 추가): date 를 여러 개 보내면 그중 하루라도 진행 중인 행사
        when(mapper.findEvents(any())).thenReturn(List.of());
        service.searchEvents(null, null, null, null, null, "startDateAsc", "0", "10",
                List.of("2026-10-04", "2026-10-03", "2026-10-04"));

        ArgumentCaptor<EventSearchCriteria> criteria = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(mapper).findEvents(criteria.capture());
        assertEquals(List.of(java.time.LocalDate.of(2026, 10, 3), java.time.LocalDate.of(2026, 10, 4)), criteria.getValue().dates());

        assertThrows(CustomException.class, () -> service.searchEvents(null, null, null, null, null, "startDateAsc", "0", "10",
                List.of("2026/10/03")));
    }
}
