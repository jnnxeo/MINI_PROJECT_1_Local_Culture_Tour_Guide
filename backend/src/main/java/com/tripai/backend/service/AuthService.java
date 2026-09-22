package com.tripai.backend.service;

import com.tripai.backend.domain.dto.LoginRequest;
import com.tripai.backend.domain.dto.LoginResponse;
import com.tripai.backend.domain.entity.User;
import com.tripai.backend.global.exception.InvalidCredentialsException;
import com.tripai.backend.global.jwt.JwtTokenProvider;
import com.tripai.backend.repository.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "이메일 또는 비밀번호가 올바르지 않습니다.";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getEmail());

        return new LoginResponse(accessToken, jwtTokenProvider.getAccessTokenExpirationSeconds());
    }

    public void logout() {
        // Stateless JWT + 서버 토큰 블랙리스트 미구현: 클라이언트가 토큰을 버리는 것으로 로그아웃이 끝난다.
        // 만료되었거나 형식이 이상한 토큰으로 호출해도 예외 없이 성공 처리한다 (AUTH-003 "이미 만료된 세션이어도 안전하게 로그아웃").
    }
}
