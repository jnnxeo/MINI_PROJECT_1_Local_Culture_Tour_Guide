package com.tripai.backend.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * jwt.secret은 SHA-256으로 정규화해서 길이에 상관없이 HS256 키로 쓸 수 있게 한다.
 */
@Component
public class JwtTokenProvider {

	private static final String USER_ID_CLAIM = "userId";

	private final SecretKey key;
	private final long accessTokenExpirationMs;

	public JwtTokenProvider(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration}") long accessTokenExpirationMs
	) {
		this.key = Keys.hmacShaKeyFor(sha256(secret));
		this.accessTokenExpirationMs = accessTokenExpirationMs;
	}

	public String createAccessToken(Long userId, String email) {
		return createToken(userId, email, accessTokenExpirationMs);
	}

	public long getAccessTokenExpirationSeconds() {
		return accessTokenExpirationMs / 1000;
	}

	public boolean isValid(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public Long getUserId(String token) {
		return parseClaims(token).get(USER_ID_CLAIM, Long.class);
	}

	private String createToken(Long userId, String email, long expirationMs) {
		Date now = new Date();
		return Jwts.builder()
				.subject(email)
				.claim(USER_ID_CLAIM, userId)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMs))
				.signWith(key)
				.compact();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}

	private static byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}
}
