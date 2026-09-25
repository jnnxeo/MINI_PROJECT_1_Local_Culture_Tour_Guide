package com.tripai.backend.domain.dto;

public record SignupResponse(
        Long userId,
        String email
) {
}