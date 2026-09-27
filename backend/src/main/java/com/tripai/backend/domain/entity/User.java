package com.tripai.backend.domain.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * users 테이블(user_id, email, password, created_at) — TripAI_DDL.sql v3.0 기준.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

	private Long userId;
	private String email;
	private String password;
	private LocalDateTime createdAt;
}
