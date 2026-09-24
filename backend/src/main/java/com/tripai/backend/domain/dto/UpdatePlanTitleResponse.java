package com.tripai.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdatePlanTitleResponse {
    private final Long planId;
    private final String title;
}
