package com.tripai.backend.domain.dto;

public record SignupResponse(
        Long memberId,
        String email
) {
}