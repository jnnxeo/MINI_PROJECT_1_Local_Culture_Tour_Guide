package com.tripai.backend.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tripai.backend.domain.dto.PlanSummary;

@Mapper 
public interface PlanMapper {

    public List<PlanSummary> findPlansByUserId( @Param("userId") Long userId, 
                                                @Param("offset") Integer offset, 
                                                @Param("size") Integer size);
    public Integer countPlansByUserId(@Param("userId") Long userId);
    public Integer deletePlanByPlanId(@Param("planId") Integer planId);
    public Integer updatePlanTitleByPlanId( @Param("planId") Integer planId, 
                                            @Param("title") String title);

}
