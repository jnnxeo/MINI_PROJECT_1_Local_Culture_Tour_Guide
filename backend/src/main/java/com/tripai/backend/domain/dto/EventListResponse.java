package com.tripai.backend.domain.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventListResponse {

    private final List<EventSummary> events;
    private final int page;
    private final long totalCount;
}
