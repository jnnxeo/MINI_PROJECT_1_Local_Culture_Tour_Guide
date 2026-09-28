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

        BoundSql list = configuration.getMappedStatement(statement("findMonthlyEvents")).getBoundSql(parameters);
        BoundSql count = configuration.getMappedStatement(statement("countMonthlyEvents")).getBoundSql(parameters);

        for (BoundSql query : List.of(list, count)) {
            String sql = query.getSql();
            assertTrue(sql.contains("display_yn = TRUE"));
            assertTrue(sql.contains("event_start_date <= ?"));
            assertTrue(sql.contains("event_end_date >= ?"));
            assertFalse(sql.contains("event_type IN"));
        }
        assertTrue(list.getSql().contains("ORDER BY event_start_date ASC, event_content_id ASC"));
        assertTrue(list.getSql().contains("LIMIT ? OFFSET ?"));
        assertEquals(4, list.getParameterMappings().size());
        assertEquals(2, count.getParameterMappings().size());
    }

    @Test
    void categoryFilterIsAppliedToBothListAndCount() throws IOException {
        Configuration configuration = loadMapper();
        Map<String, Object> parameters = parameters(List.of("전시", "전시/미술"));

        for (String method : List.of("findMonthlyEvents", "countMonthlyEvents")) {
            BoundSql query = configuration.getMappedStatement(statement(method)).getBoundSql(parameters);
            assertTrue(query.getSql().replaceAll("\\s+", " ").matches("(?s).*event_type IN \\(\\s*\\?\\s*,\\s*\\?\\s*\\).*"), query.getSql());
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
        try (InputStream input = MonthlyEventMapperTest.class.getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
}
