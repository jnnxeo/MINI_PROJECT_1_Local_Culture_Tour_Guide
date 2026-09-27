package com.tripai.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.tripai.backend.domain.dto.PlanListResponse;
import com.tripai.backend.domain.dto.UpdatePlanTitleRequest;
import com.tripai.backend.domain.dto.UpdatePlanTitleResponse;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.PlansService;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;

@RestController 
@RequestMapping("/api/plans")
@RequiredArgsConstructor 
public class PlanController {

    private final PlansService plansService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<PlanListResponse>> getPlans(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
        ) 
    {
        if(page < 0){
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (size != 5 && size != 10 && size != 20) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        System.out.println("debug >>>> plancontroller getPlans : " + userId);

        PlanListResponse response = plansService.getPlans(userId, page, size);
        return ResponseEntity.status(HttpStatus.OK)
                                .body(ApiResponse.success(response));
    }

    @DeleteMapping("/{tripPlanId}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer tripPlanId
    ){
        plansService.deletePlan(userId, tripPlanId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                            .body(ApiResponse.success(null));
    }

    @PatchMapping("/{tripPlanId}")
    public ResponseEntity<ApiResponse<UpdatePlanTitleResponse>> updatePlanTitle(
            @AuthenticationPrincipal Long userId,    
            @PathVariable Integer tripPlanId,
            @RequestBody UpdatePlanTitleRequest request
    ) {
        plansService.updatePlanTitle(userId, tripPlanId, request.getTitle());

        return ResponseEntity.status(HttpStatus.OK)
                            .body(ApiResponse.success(null));
    }

}


