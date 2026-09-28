package com.tripai.backend.domain.dto.plan;

import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventPlanItemRow {
    private Long itemId;
    private Long planId;
    private Integer seq;
    private String type;
    private String contentId;
    private String name;
    private String category;
    private String address;
    private Double lat;
    private Double lng;
    private String imageUrl;
    private LocalTime startTime;
    private Integer durationMin;
    private Boolean timeFixed;
    private String aiReason;
    private LocalTime openTime;
    private LocalTime closeTime;
}
