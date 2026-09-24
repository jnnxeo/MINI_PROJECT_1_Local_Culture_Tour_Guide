package com.tripai.backend.domain.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventSummary {
    private final Long eventId;
    private final String title;
    private final LocalDate date;
    private final String place;
    private final String imageUrl;
}
