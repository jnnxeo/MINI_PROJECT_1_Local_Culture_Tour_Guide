package com.tripai.backend.domain.dto.plan;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventPlanRow {
    private Long planId;
    private Long userId;
    private String anchorEventId;
    private String title;
    private LocalDate tripDate;
    private LocalTime visitStartTime;
    private LocalTime visitEndTime;
    private Boolean saved;
    private Boolean aiGenerated;
    private String transportMode;
    private Integer headcount;
}
