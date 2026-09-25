package com.tripai.backend.repository;

import com.tripai.backend.domain.entity.PlanItemView;
import com.tripai.backend.domain.entity.TripPlan;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlanDraftMapper {

	Optional<TripPlan> findPlanById(Long tripPlanId);

	List<PlanItemView> findItemsByPlanId(Long tripPlanId);

	int updateTitle(@Param("tripPlanId") Long tripPlanId, @Param("title") String title);
}
