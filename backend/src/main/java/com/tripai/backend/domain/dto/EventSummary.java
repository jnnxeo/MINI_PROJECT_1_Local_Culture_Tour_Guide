package com.tripai.backend.domain.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventSummary {
    private final String eventId;
    private final String title;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String address;
    private final String imageUrl;
}
