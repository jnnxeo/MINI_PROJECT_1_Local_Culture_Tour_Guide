package com.tripai.backend.service;

import org.springframework.stereotype.Service;

import com.tripai.backend.domain.dto.PlanListResponse;
import com.tripai.backend.domain.dto.PlanSummary;
import com.tripai.backend.domain.dto.UpdatePlanTitleResponse;
import com.tripai.backend.repository.PlanMapper;

import lombok.RequiredArgsConstructor;
import java.util.List;

@Service 
@RequiredArgsConstructor 
public class PlansService {

    private final PlanMapper planMapper;

    public PlanListResponse getPlans(   Long userId,
                                        int page,
                                        int size    )
        {
            int offset = page * size;

            List<PlanSummary> plans =
                    planMapper.findPlansByUserId(userId, offset, size);

            Integer totalCount = planMapper.countPlansByUserId(userId);

            return new PlanListResponse(plans, page, totalCount);
        }

    public void deletePlan(Integer planId){
        planMapper.deletePlanByPlanId(planId);
    }

    public Integer updatePlanTitle( Integer planId,
                                                    String title)
    {
        
        Integer response = planMapper.updatePlanTitleByPlanId(planId, title);
        
        return response;
    }



    

}
