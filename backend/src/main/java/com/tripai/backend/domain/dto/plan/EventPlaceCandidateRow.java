package com.tripai.backend.domain.dto.plan;

import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventPlaceCandidateRow {
    private String contentId;
    private LocalTime openTime;
    private LocalTime closeTime;
}
