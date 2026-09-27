package com.tripai.backend.domain.entity;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthlyEvent {

    private String eventId;
    private String title;
    private String eventType;
    private String district;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean freeYn;
    private String fee;
    private String imageUrl;
}
