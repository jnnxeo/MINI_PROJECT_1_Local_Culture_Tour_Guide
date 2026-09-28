package com.tripai.backend.service;

import org.springframework.http.HttpStatus;

/** 맛집 후보 조회 요청 오류 — PlaceController 에서 상태 코드·문구 그대로 응답한다 */
public class PlaceSearchException extends RuntimeException {

    private final HttpStatus status;

    public PlaceSearchException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
