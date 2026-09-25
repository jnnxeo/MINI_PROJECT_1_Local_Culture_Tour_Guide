package com.tripai.backend.service;

import org.springframework.http.HttpStatus;

/**
 * 일정 편집 규칙 위반. API 명세마다 같은 위반이라도 응답 코드가 달라서(예: 시간 초과 006=400, 007=422)
 * 상태 코드와 문구를 같이 들고 다닌다. PlanDraftController 에서 응답으로 바꾼다.
 */
public class PlanRuleException extends RuntimeException {

    private final HttpStatus status;

    public PlanRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
