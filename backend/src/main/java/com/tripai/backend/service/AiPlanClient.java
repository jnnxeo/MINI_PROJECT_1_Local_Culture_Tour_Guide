package com.tripai.backend.service;

import com.tripai.backend.domain.dto.AiPlanInput;
import com.tripai.backend.domain.dto.AiPlanResponse;

/**
 * AI 일정 생성 호출 (AI-002·007·008).
 * 나의 일정 추천(PlanRecommendService)은 이 인터페이스만 보고, 키가 없거나 실패하면 규칙 기반 추천으로 대체한다.
 */
public interface AiPlanClient {

    /** API 키가 설정되어 있어 AI 를 호출할 수 있는지 */
    boolean isEnabled();

    /**
     * 후보 안에서 일정을 만든다. 호출·응답 형식·후보 밖 장소 등 문제가 있으면 예외를 던진다.
     * 시간·영업시간 같은 일정 규칙 검사는 호출한 쪽에서 한 번 더 한다.
     */
    AiPlanResponse generate(AiPlanInput input);
}
