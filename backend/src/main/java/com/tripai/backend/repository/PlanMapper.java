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
    public long countPlansByUserId(@Param("userId") Long userId);
    public Integer deletePlanByPlanId(  @Param("userId") Long userId,
                                        @Param("tripPlanId") Integer tripPlanId);
    public Integer updatePlanTitleByPlanId( @Param("userId") Long userId,
                                            @Param("tripPlanId") Integer tripPlanId, 
                                            @Param("title") String title);

}
