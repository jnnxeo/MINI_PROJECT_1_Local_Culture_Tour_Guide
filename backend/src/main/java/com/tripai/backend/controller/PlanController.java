package com.tripai.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.apache.catalina.connector.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.mysql.cj.protocol.a.NativeConstants.IntegerDataType;
import com.tripai.backend.domain.dto.PlanListResponse;
import com.tripai.backend.domain.dto.UpdatePlanTitleRequest;
import com.tripai.backend.domain.dto.UpdatePlanTitleResponse;
import com.tripai.backend.global.response.ApiResponse;
import com.tripai.backend.service.PlansService;

@RestController 
@RequestMapping("/api/plans")
@RequiredArgsConstructor 
public class PlanController {

    private final PlansService plansService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<PlanListResponse>> getPlans(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
        ) 
    {
        PlanListResponse response = plansService.getPlans(userId, page, size);
        return ResponseEntity.status(HttpStatus.OK)
                                .body(ApiResponse.success(response));
    }

    @DeleteMapping("/drafts/{planId}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer planId
    ){
        plansService.deletePlan(userId, planId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                            .body(ApiResponse.success(null));
    }

    @PatchMapping("/drafts/{planId}/title")
    public ResponseEntity<ApiResponse<UpdatePlanTitleResponse>> updatePlanTitle(
            @AuthenticationPrincipal Long userId,    
            @PathVariable Integer planId,
            @RequestBody UpdatePlanTitleRequest request
    ) {
        Integer response =
                plansService.updatePlanTitle(userId, planId, request.getTitle());

        return ResponseEntity.status(HttpStatus.OK)
                            .body(ApiResponse.success(null));
    }

}


