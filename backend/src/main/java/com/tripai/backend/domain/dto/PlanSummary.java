package com.tripai.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonGetter;

@Getter 
@AllArgsConstructor 
public class PlanSummary {
    private final Long tripPlanId;
    private final String title;
    private final LocalDate tripDate;
    private final Integer dDay;

    @JsonGetter("dDay")
    public Integer getDDay() {
        return dDay;
    }
}
