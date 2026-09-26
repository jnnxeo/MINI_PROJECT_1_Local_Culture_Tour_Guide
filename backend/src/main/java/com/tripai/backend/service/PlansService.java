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

            System.out.println("debug >>>> planservice getplans userId : " + userId);

            long totalCount = planMapper.countPlansByUserId(userId);
            System.out.println("debug >>>> planservice getplans totalCount : " + totalCount);

            List<PlanSummary> plans =
                    planMapper.findPlansByUserId(userId, offset, size);
            System.out.println("debug >>>> planservice getplans plans : " + plans);

            return new PlanListResponse(plans, page, totalCount);
        }

    public void deletePlan(Long userId, Integer tripPlanId){
        Integer deleteRows = planMapper.deletePlanByPlanId(userId, tripPlanId);

        if(deleteRows == 0){
            throw new CustomException(ErrorCode.PLAN_NOT_FOUND);
        }

    }

    public void updatePlanTitle(     Long userId,             
                                        Integer tripPlanId,
                                        String title)
    {
        
        Integer updatedRows = planMapper.updatePlanTitleByPlanId(userId, tripPlanId, title);
        if (updatedRows == 0) {
            throw new CustomException(ErrorCode.PLAN_NOT_FOUND);
        }
    }



    

}
