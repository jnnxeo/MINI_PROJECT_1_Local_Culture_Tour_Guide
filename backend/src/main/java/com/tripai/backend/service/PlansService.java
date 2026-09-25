package com.tripai.backend.service;

import org.springframework.stereotype.Service;

import com.tripai.backend.domain.dto.PlanListResponse;
import com.tripai.backend.domain.dto.PlanSummary;
import com.tripai.backend.domain.dto.UpdatePlanTitleResponse;
import com.tripai.backend.repository.PlanMapper;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;

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

    public void deletePlan(Long userId, Integer planId){
        Integer deleteRows = planMapper.deletePlanByPlanId(userId, planId);

        if(deleteRows == 0){
            throw new CustomException(ErrorCode.PLAN_NOT_FOUND);
        }

    }

    public void updatePlanTitle(     Long userId,             
                                        Integer planId,
                                        String title)
    {
        
        Integer updatedRows = planMapper.updatePlanTitleByPlanId(userId, planId, title);
        if (updatedRows == 0) {
            throw new CustomException(ErrorCode.PLAN_NOT_FOUND);
        }
    }



    

}
