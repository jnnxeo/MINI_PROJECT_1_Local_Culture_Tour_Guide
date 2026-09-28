package com.tripai.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class MonthlyEventIntegrationTest {

    private static final String EXHIBITION = "TRIPAI-IT-MONTH-EXHIBITION";
    private static final String PERFORMANCE = "TRIPAI-IT-MONTH-PERFORMANCE";
    private static final String OUTSIDE = "TRIPAI-IT-MONTH-OUTSIDE";
    private static final String HIDDEN = "TRIPAI-IT-MONTH-HIDDEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void monthlyEndpointReturnsMappedRowsWithInclusiveBoundariesAndPagination() throws Exception {
        assertEquals(0, fixtureCount());

        insert(
                EXHIBITION,
                "전시/미술",
                "월 경계 전시",
                "종로구",
                "2098-01-31",
                "2098-02-01",
                null,
                null,
                true
        );

        insert(
                PERFORMANCE,
                "콘서트",
                "월 경계 공연",
                "마포구",
                "2098-02-28",
                "2098-03-01",
                false,
                "10,000원",
                true
        );

        insert(
                OUTSIDE,
                "전시/미술",
                "다음 달 전시",
                "중구",
                "2098-03-01",
                "2098-03-02",
                true,
                null,
                true
        );

        insert(
                HIDDEN,
                "전시/미술",
                "비공개 전시",
                "중구",
                "2098-02-10",
                "2098-02-11",
                true,
                null,
                false
        );

        String authorization =
                "Bearer " + jwtTokenProvider.createAccessToken(
                        1L,
                        "integration-test@example.com"
                );

        // 첫 페이지:
        // 선택 월(2098-02)에 시작한 PERFORMANCE가 우선 노출된다.
        mockMvc.perform(
                        get("/api/events/months/2098-02")
                                .header("Authorization", authorization)
                                .param("size", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.month").value("2098-02"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value(PERFORMANCE))
                .andExpect(jsonPath("$.data.items[0].category").value("공연"))
                .andExpect(jsonPath("$.data.items[0].startDate").value("2098-02-28"))
                .andExpect(jsonPath("$.data.items[0].endDate").value("2098-03-01"))
                .andExpect(jsonPath("$.data.items[0].freeYn").value(false))
                .andExpect(jsonPath("$.data.items[0].fee").value("10,000원"));

        // 다음 페이지:
        // 이전 달에 시작해 2월에도 진행 중인 EXHIBITION이 뒤에 노출된다.
        mockMvc.perform(
                        get("/api/events/months/2098-02")
                                .header("Authorization", authorization)
                                .param("page", "1")
                                .param("size", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items[0].eventId").value(EXHIBITION))
                .andExpect(jsonPath("$.data.items[0].category").value("전시"))
                .andExpect(jsonPath("$.data.items[0].startDate").value("2098-01-31"))
                .andExpect(jsonPath("$.data.items[0].endDate").value("2098-02-01"));

        mockMvc.perform(
                        get("/api/events/months/2098-02")
                                .header("Authorization", authorization)
                                .param("category", "전시")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value(EXHIBITION));

        mockMvc.perform(
                        get("/api/events/months/2098-02")
                                .header("Authorization", authorization)
                                .param("category", "공연")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value(PERFORMANCE));

        mockMvc.perform(
                        get("/api/events/months/2098-04")
                                .header("Authorization", authorization)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));

        mockMvc.perform(
                        get("/api/events/months/2098-02")
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/events/months/2098-13")
                                .header("Authorization", authorization)
                )
                .andExpect(status().isBadRequest());
    }

    @AfterTransaction
    void fixtureRowsAreRolledBack() {
        assertEquals(0, fixtureCount());
    }

    private void insert(
            String id,
            String category,
            String name,
            String district,
            String start,
            String end,
            Boolean freeYn,
            String fee,
            boolean displayYn
    ) {
        jdbcTemplate.update("""
                INSERT INTO event (
                    event_content_id,
                    event_type,
                    event_name,
                    district_name,
                    event_start_date,
                    event_end_date,
                    free_yn,
                    use_fee,
                    display_yn
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                category,
                name,
                district,
                Date.valueOf(LocalDate.parse(start)),
                Date.valueOf(LocalDate.parse(end)),
                freeYn,
                fee,
                displayYn
        );
    }

    private int fixtureCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM event
                WHERE event_content_id IN (?, ?, ?, ?)
                """,
                Integer.class,
                EXHIBITION,
                PERFORMANCE,
                OUTSIDE,
                HIDDEN
        );
    }
}