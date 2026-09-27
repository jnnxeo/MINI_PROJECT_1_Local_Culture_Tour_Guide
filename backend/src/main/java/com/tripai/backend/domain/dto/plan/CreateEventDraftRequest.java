package com.tripai.backend.domain.dto.plan;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateEventDraftRequest(
        @JsonAlias("eventId") @NotBlank String anchorEventId,
        @JsonAlias("visitDate") @NotNull LocalDate tripDate,
        @NotNull @Min(1) Integer headcount,
        @NotBlank String transportMode,
        @NotEmpty List<String> interests,
        Boolean useAi
) {}
