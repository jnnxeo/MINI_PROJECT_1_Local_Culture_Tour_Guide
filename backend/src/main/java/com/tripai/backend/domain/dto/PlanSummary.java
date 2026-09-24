package com.tripai.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;

@Getter 
@AllArgsConstructor 
public class PlanSummary {
    private final Long planId;
    private final String title;
    private final LocalDate visitDate;
    private final Integer dDay;
}
