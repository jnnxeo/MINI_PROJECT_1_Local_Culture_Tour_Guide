package com.tripai.backend.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter 
public enum ErrorCode {

    // ① 값들 — 각각 괄호로 데이터를 넘김 (각각 예외사항 추가 바랍니다.)
    // 인증/회원 (API-001, 002)
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),      // 회원가입
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."), // 로그인
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),              // 공통 (인증 필요)

    //일정 (API-003, 004, 005)
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),                  // 남의 일정 (공통)

    //즐겨찾기(FAV-001, 002)
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "저장되지 않은 행사입니다."),
    ALREADY_FAVORITED(HttpStatus.CONFLICT, "이미 저장된 행사입니다."),

    //행사(EVENT-001)
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 행사입니다."),        // 상세조회 시 없는 행사

    //공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값을 확인해주세요."),          // 필수값 누락 등
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다."); // 서버 오류

    // ② 필드 — 각 값이 들고 있을 데이터의 자리
    private final HttpStatus status;
    private final String message;
    
    // ③ 생성자 — 괄호로 넘긴 데이터를 필드에 담는 것
    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
    
}
