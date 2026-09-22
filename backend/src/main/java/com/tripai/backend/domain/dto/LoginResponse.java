package com.tripai.backend.domain.dto;

public record LoginResponse(
		String accessToken,
		long expiresIn
) {
}
