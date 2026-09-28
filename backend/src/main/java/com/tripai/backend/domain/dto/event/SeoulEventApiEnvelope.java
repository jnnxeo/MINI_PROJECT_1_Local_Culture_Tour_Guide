package com.tripai.backend.domain.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * 서울시 API 실제 응답의 최상위 래퍼.
 * 정상: {"culturalEventInfo": {list_total_count, RESULT, row[]}}
 * 오류(인증키 오류 등): {"RESULT": {"CODE": "INFO-100", "MESSAGE": "..."}} 처럼 래퍼 없이 올 수 있다.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeoulEventApiEnvelope {

    @JsonProperty("culturalEventInfo")
    private SeoulEventApiResponse culturalEventInfo;

    @JsonProperty("RESULT")
    private SeoulEventApiResponse.Result result;
}