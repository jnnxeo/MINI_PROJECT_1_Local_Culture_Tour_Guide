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
    void searchMatchesKeywordAcrossMonthsAndCombinesFiltersWithoutDuplicateRows() throws Exception {
        assertEquals(0, fixtureCount());
        insert(EXHIBITION, "전시/미술", "Z-IT53키워드_ 전시", "종로구", "세종문화회관", "2098-01-31", "2098-03-31", null, true);
        insert(PERFORMANCE, "콘서트", "A-IT53키워드 공연", "종로구", "서울광장", "IT53한강대로 123", "2098-02-28", "2098-03-10", true, true);
        insert(OUTSIDE, "전시/미술", "M-IT53키워드 다음 달", "중구", "미술관", "2098-03-02", "2098-03-03", false, true);
        insert(HIDDEN, "콘서트", "H-IT53키워드 비공개", "종로구", "광장", "2098-02-10", "2098-02-11", true, false);
        String authorization = authorization();

        mockMvc.perform(get("/api/events").header("Authorization", authorization).param("keyword", "IT53키워드"))
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
