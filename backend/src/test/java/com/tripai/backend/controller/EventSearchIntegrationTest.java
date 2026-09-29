package com.tripai.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tripai.backend.global.jwt.JwtTokenProvider;
import java.sql.Date;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "TRIPAI_DB_INTEGRATION_TEST", matches = "true")
class EventSearchIntegrationTest {

    private static final String EXHIBITION = "TRIPAI-IT-SEARCH-EXHIBITION";
    private static final String PERFORMANCE = "TRIPAI-IT-SEARCH-PERFORMANCE";
    private static final String OUTSIDE = "TRIPAI-IT-SEARCH-OUTSIDE";
    private static final String HIDDEN = "TRIPAI-IT-SEARCH-HIDDEN";
    private static final String ENDED = "TRIPAI-IT-SEARCH-ENDED";
    private static final String UPCOMING = "TRIPAI-IT-SEARCH-UPCOMING";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void selectedDatesUseOrAndKeepOtherFiltersWithoutDuplicates() throws Exception {
        assertEquals(0, fixtureCount());
        insert(EXHIBITION, "전시/미술", "IT53날짜 전시", "종로구", "전시장", "2098-10-03", "2098-10-04", true, true);
        insert(PERFORMANCE, "콘서트", "IT53날짜 공연", "종로구", "공연장", "2098-10-08", "2098-10-08", true, true);
        insert(OUTSIDE, "전시/미술", "IT53날짜 제외", "중구", "미술관", "2098-10-05", "2098-10-06", false, true);
        String authorization = authorization();
        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT53날짜").param("date", "2098-10-03", "2098-10-04", "2098-10-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2));
        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT53날짜").param("date", "2098-10-03", "2098-10-04", "2098-10-08")
                        .param("category", "전시").param("freeYn", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value(EXHIBITION));
    }

    @Test
    void selectedDateKeepsYearAndIncludesOnlyEventsRunningOnThatDay() throws Exception {
        assertEquals(0, fixtureCount());
        insert(ENDED, "전시/미술", "IT61연도 과거 종료", "종로구", "전시장",
                "2021-09-28", "2021-09-29", true, true);
        insert(PERFORMANCE, "콘서트", "IT61연도 장기 진행", "종로구", "공연장",
                "2021-09-28", "2026-09-29", true, true);
        insert(EXHIBITION, "전시/미술", "IT61연도 선택일", "종로구", "전시장",
                "2026-09-29", "2026-09-29", true, true);
        insert(OUTSIDE, "전시/미술", "IT61연도 다음 해", "종로구", "전시장",
                "2027-09-29", "2027-09-29", true, true);
        insert(HIDDEN, "전시/미술", "IT61연도 비표시", "종로구", "전시장",
                "2026-09-29", "2026-09-29", true, false);

        mockMvc.perform(get("/api/events").header("Authorization", authorization())
                        .param("keyword", "IT61연도").param("date", "2026-09-29").param("sort", "startDateAsc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].eventId").value(PERFORMANCE))
                .andExpect(jsonPath("$.data.items[1].eventId").value(EXHIBITION));
    }

    @Test
    void datesFromDifferentYearsArePreservedWhenSortingAndPaging() throws Exception {
        assertEquals(0, fixtureCount());
        insert(EXHIBITION, "전시/미술", "B-IT61다중연도 연말", "종로구", "전시장",
                "2026-12-31", "2026-12-31", true, true);
        insert(PERFORMANCE, "콘서트", "A-IT61다중연도 새해", "종로구", "공연장",
                "2027-01-01", "2027-01-01", true, true);
        insert(OUTSIDE, "전시/미술", "C-IT61다중연도 과거", "종로구", "전시장",
                "2025-12-31", "2026-01-01", true, true);
        String authorization = authorization();

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT61다중연도").param("date", "2026-12-31", "2027-01-01")
                        .param("sort", "titleAsc").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value(PERFORMANCE));
        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT61다중연도").param("date", "2026-12-31", "2027-01-01")
                        .param("sort", "titleAsc").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value(EXHIBITION));
    }

    @Test
    void defaultDateSortPrioritizesMatchingDatesAndKeepsCountAcrossPages() throws Exception {
        assertEquals(0, fixtureCount());
        insert(EXHIBITION, "전시/미술", "IT61날짜정렬 당일", "종로구", "전시장", "2026-09-28", "2026-09-30", true, true);
        insert(UPCOMING, "전시/미술", "IT61날짜정렬 당일 빠른종료", "종로구", "전시장", "2026-09-28", "2026-09-29", true, true);
        insert(PERFORMANCE, "콘서트", "IT61날짜정렬 두번째선택일", "종로구", "공연장", "2026-10-03", "2026-10-04", true, true);
        insert(OUTSIDE, "전시/미술", "IT61날짜정렬 전날", "종로구", "전시장", "2026-09-27", "2026-10-10", true, true);
        insert(ENDED, "전시/미술", "IT61날짜정렬 장기", "종로구", "전시장", "2021-09-28", "2026-12-31", true, true);
        String authorization = authorization();

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT61날짜정렬").param("date", "2026-09-28", "2026-10-03").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.items[0].eventId").value(UPCOMING))
                .andExpect(jsonPath("$.data.items[1].eventId").value(EXHIBITION));
        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT61날짜정렬").param("date", "2026-09-28", "2026-10-03")
                        .param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.items[0].eventId").value(PERFORMANCE))
                .andExpect(jsonPath("$.data.items[1].eventId").value(OUTSIDE));
        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT61날짜정렬").param("date", "2026-09-28", "2026-10-03")
                        .param("size", "2").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value(ENDED));
    }

    @Test
    void defaultRecentSortGroupsOngoingUpcomingAndEndedEvents() throws Exception {
        assertEquals(0, fixtureCount());
        LocalDate today = jdbcTemplate.queryForObject("SELECT CURRENT_DATE", Date.class).toLocalDate();
        insert(EXHIBITION, "전시/미술", "IT61최근정렬 최근진행", "종로구", "전시장", today.minusDays(1).toString(), today.plusDays(10).toString(), true, true);
        insert(PERFORMANCE, "콘서트", "IT61최근정렬 장기진행", "종로구", "공연장", today.minusDays(365).toString(), today.plusDays(10).toString(), true, true);
        insert(OUTSIDE, "전시/미술", "IT61최근정렬 곧시작", "종로구", "전시장", today.plusDays(1).toString(), today.plusDays(2).toString(), true, true);
        insert(HIDDEN, "전시/미술", "IT61최근정렬 나중시작", "종로구", "전시장", today.plusDays(20).toString(), today.plusDays(21).toString(), true, true);
        insert(ENDED, "전시/미술", "IT61최근정렬 최근종료", "종로구", "전시장", today.minusDays(2).toString(), today.minusDays(1).toString(), true, true);
        insert(UPCOMING, "전시/미술", "IT61최근정렬 과거종료", "종로구", "전시장", today.minusDays(60).toString(), today.minusDays(50).toString(), true, true);

        mockMvc.perform(get("/api/events").header("Authorization", authorization()).param("keyword", "IT61최근정렬"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(6))
                .andExpect(jsonPath("$.data.items[0].eventId").value(EXHIBITION))
                .andExpect(jsonPath("$.data.items[1].eventId").value(PERFORMANCE))
                .andExpect(jsonPath("$.data.items[2].eventId").value(OUTSIDE))
                .andExpect(jsonPath("$.data.items[3].eventId").value(HIDDEN))
                .andExpect(jsonPath("$.data.items[4].eventId").value(ENDED))
                .andExpect(jsonPath("$.data.items[5].eventId").value(UPCOMING));
    }

    @Test
    void searchMatchesKeywordAcrossMonthsAndCombinesFiltersWithoutDuplicateRows() throws Exception {
        assertEquals(0, fixtureCount());
        insert(EXHIBITION, "전시/미술", "Z-IT53키워드_ 전시", "종로구", "세종문화회관", "2098-01-31", "2098-03-31", null, true);
        insert(PERFORMANCE, "콘서트", "A-IT53키워드 공연", "종로구", "서울광장", "IT53한강대로 123", "2098-02-28", "2098-03-10", true, true);
        insert(OUTSIDE, "전시/미술", "M-IT53키워드 다음 달", "중구", "미술관", "2098-03-02", "2098-03-03", false, true);
        insert(HIDDEN, "콘서트", "H-IT53키워드 비공개", "종로구", "광장", "2098-02-10", "2098-02-11", true, false);
        String authorization = authorization();

        mockMvc.perform(get("/api/events").header("Authorization", authorization).param("keyword", "IT53키워드").param("sort", "startDateAsc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.items[0].eventId").value(EXHIBITION));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT53키워드").param("month", "2098-02")
                        .param("category", "전시", "공연", "전시").param("district", "종로구")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value(EXHIBITION))
                .andExpect(jsonPath("$.data.items[0].freeYn").value(nullValue()));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("month", "2098-02").param("category", "전시", "공연")
                        .param("district", "종로구"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT53키워드").param("month", "2098-02")
                        .param("category", "전시", "공연").param("district", "종로구")
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items[0].eventId").value(PERFORMANCE));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT53키워드").param("month", "2098-02")
                        .param("freeYn", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value(PERFORMANCE));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT53키워드").param("sort", "endDateAsc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].eventId").value(OUTSIDE))
                .andExpect(jsonPath("$.data.items[1].eventId").value(PERFORMANCE))
                .andExpect(jsonPath("$.data.items[2].eventId").value(EXHIBITION));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT53키워드").param("sort", "titleAsc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].eventId").value(PERFORMANCE))
                .andExpect(jsonPath("$.data.items[1].eventId").value(OUTSIDE))
                .andExpect(jsonPath("$.data.items[2].eventId").value(EXHIBITION));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "공연").param("month", "2098-02").param("district", "종로구"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].category").value("공연"));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT53키워드_").param("month", "2098-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value(EXHIBITION));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "서울광장").param("month", "2098-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].eventId").value(PERFORMANCE));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT53한강대로").param("month", "2098-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].eventId").value(PERFORMANCE));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "종로구").param("month", "2098-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "it53키워드").param("month", "2098-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2));

        mockMvc.perform(get("/api/events").header("Authorization", authorization)
                        .param("keyword", "IT53키워드").param("month", "2098-02")
                        .param("freeYn", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2));

        mockMvc.perform(get("/api/events").header("Authorization", authorization).param("keyword", "IT53없음"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void endSoonSortPlacesEndedEventsAfterUpcomingEvents() throws Exception {
        assertEquals(0, fixtureCount());
        insert(ENDED, "전시/미술", "IT53종료순 과거", "중구", "미술관",
                "2020-01-01", "2020-01-02", null, true);
        insert(UPCOMING, "전시/미술", "IT53종료순 미래", "중구", "미술관",
                "2098-01-01", "2098-01-02", null, true);

        mockMvc.perform(get("/api/events").header("Authorization", authorization())
                        .param("keyword", "IT53종료순").param("sort", "endDateAsc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items[0].eventId").value(UPCOMING))
                .andExpect(jsonPath("$.data.items[1].eventId").value(ENDED));
    }

    @Test
    void invalidQueriesAndMissingAuthenticationAreRejected() throws Exception {
        String authorization = authorization();
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/events").header("Authorization", authorization).param("month", "2098-13"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/events").header("Authorization", authorization).param("category", "없는분야"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/events").header("Authorization", authorization).param("sort", "wrong"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/events").header("Authorization", authorization).param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @AfterTransaction
    void fixtureRowsAreRolledBack() {
        assertEquals(0, fixtureCount());
    }

    private String authorization() {
        return "Bearer " + jwtTokenProvider.createAccessToken(1L, "integration-test@example.com");
    }

    private void insert(String id, String category, String name, String district, String place,
                        String start, String end, Boolean freeYn, boolean displayYn) {
        insert(id, category, name, district, place, null, start, end, freeYn, displayYn);
    }

    private void insert(String id, String category, String name, String district, String place, String address,
                        String start, String end, Boolean freeYn, boolean displayYn) {
        jdbcTemplate.update("""
                INSERT INTO event (event_content_id, event_type, event_name, district_name, event_place, addr,
                                   event_start_date, event_end_date, free_yn, display_yn)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, category, name, district, place, address,
                Date.valueOf(LocalDate.parse(start)), Date.valueOf(LocalDate.parse(end)), freeYn, displayYn);
    }

    private int fixtureCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM event
                WHERE event_content_id IN (?, ?, ?, ?, ?, ?)
                """, Integer.class, EXHIBITION, PERFORMANCE, OUTSIDE, HIDDEN, ENDED, UPCOMING);
    }
}
