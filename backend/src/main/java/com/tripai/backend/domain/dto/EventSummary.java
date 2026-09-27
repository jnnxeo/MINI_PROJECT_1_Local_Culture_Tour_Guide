package com.tripai.backend.domain.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventSummary {
    private final String eventContentId;
    private final String title;
    private final LocalDate eventStartDate;
    private final LocalDate eventEndDate;
    private final String addr;
    private final String imageUrl;
}
