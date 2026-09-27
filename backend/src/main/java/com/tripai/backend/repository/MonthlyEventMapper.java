package com.tripai.backend.repository;

import com.tripai.backend.domain.entity.MonthlyEvent;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MonthlyEventMapper {

    List<MonthlyEvent> findMonthlyEvents(
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            @Param("eventTypes") List<String> eventTypes,
            @Param("size") int size,
            @Param("offset") long offset
    );

    long countMonthlyEvents(
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            @Param("eventTypes") List<String> eventTypes
    );
}
