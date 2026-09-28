package com.tripai.backend.repository;

import com.tripai.backend.domain.entity.MonthlyEvent;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EventSearchMapper {

    List<MonthlyEvent> findEvents(@Param("criteria") EventSearchCriteria criteria);

    long countEvents(@Param("criteria") EventSearchCriteria criteria);
}
