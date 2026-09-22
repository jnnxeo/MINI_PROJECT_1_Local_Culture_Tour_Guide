package com.tripai.backend.domain.dto;

public record LoginResponse(
		String accessToken,
		String refreshToken,
		long expiresIn
) {
}
