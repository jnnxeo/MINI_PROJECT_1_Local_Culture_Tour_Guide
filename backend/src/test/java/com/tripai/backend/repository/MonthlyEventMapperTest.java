package com.tripai.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class MonthlyEventMapperTest {

    @Test
    void listAndCountUseTheSameInclusiveMonthConditions() throws IOException {
        Configuration configuration = loadMapper();
        Map<String, Object> parameters = parameters(null);

        BoundSql list = configuration
                .getMappedStatement(statement("findMonthlyEvents"))
                .getBoundSql(parameters);

        BoundSql count = configuration
                .getMappedStatement(statement("countMonthlyEvents"))
                .getBoundSql(parameters);

        for (BoundSql query : List.of(list, count)) {
            String sql = query.getSql();

            assertTrue(sql.contains("display_yn = TRUE"));
            assertTrue(sql.contains("event_start_date <= ?"));
            assertTrue(sql.contains("event_end_date >= ?"));
            assertFalse(sql.contains("event_type IN"));
        }

        String listSql = list.getSql().replaceAll("\\s+", " ").trim();

        // 선택한 월에 시작하는 행사를 최우선으로 정렬
        assertTrue(
                listSql.contains(
                        "CASE WHEN event_start_date BETWEEN ? AND ? THEN 0 ELSE 1 END ASC"
                ),
                listSql
        );

        // 선택 월 내부에서는 시작일이 빠른 순
        assertTrue(
                listSql.contains(
                        "CASE WHEN event_start_date BETWEEN ? AND ? THEN event_start_date END ASC"
                ),
                listSql
        );

        // 이전부터 진행 중인 행사는 선택 월에 가까운 최근 시작일 순
        assertTrue(
                listSql.contains(
                        "CASE WHEN event_start_date < ? THEN event_start_date END DESC"
                ),
                listSql
        );

        // 동일 조건에서는 event_content_id로 정렬 순서 고정
        assertTrue(
                listSql.contains("event_content_id ASC"),
                listSql
        );

        assertTrue(
                listSql.contains("LIMIT ? OFFSET ?"),
                listSql
        );

        // WHERE 2개
        // + 첫 번째 CASE 2개
        // + 두 번째 CASE 2개
        // + 세 번째 CASE 1개
        // + LIMIT/OFFSET 2개
        assertEquals(9, list.getParameterMappings().size());

        // count 쿼리는 기존 월 조건 2개만 사용
        assertEquals(2, count.getParameterMappings().size());
    }

    @Test
    void categoryFilterIsAppliedToBothListAndCount() throws IOException {
        Configuration configuration = loadMapper();
        Map<String, Object> parameters = parameters(List.of("전시", "전시/미술"));

        for (String method : List.of("findMonthlyEvents", "countMonthlyEvents")) {
            BoundSql query = configuration
                    .getMappedStatement(statement(method))
                    .getBoundSql(parameters);

            assertTrue(
                    query.getSql()
                            .replaceAll("\\s+", " ")
                            .matches("(?s).*event_type IN \\(\\s*\\?\\s*,\\s*\\?\\s*\\).*"),
                    query.getSql()
            );
        }
    }

    private static String statement(String method) {
        return MonthlyEventMapper.class.getName() + "." + method;
    }

    private static Map<String, Object> parameters(List<String> eventTypes) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("monthStart", LocalDate.of(2026, 9, 1));
        parameters.put("monthEnd", LocalDate.of(2026, 9, 30));
        parameters.put("eventTypes", eventTypes);
        parameters.put("size", 10);
        parameters.put("offset", 0L);

        return parameters;
    }

    private static Configuration loadMapper() throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/MonthlyEventMapper.xml";

        try (InputStream input = MonthlyEventMapperTest.class
                .getClassLoader()
                .getResourceAsStream(resource)) {

            new XMLMapperBuilder(
                    input,
                    configuration,
                    resource,
                    configuration.getSqlFragments()
            ).parse();
        }

        return configuration;
    }
}