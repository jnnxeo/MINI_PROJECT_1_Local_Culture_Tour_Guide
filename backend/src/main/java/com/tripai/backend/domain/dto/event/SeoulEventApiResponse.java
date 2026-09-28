package com.tripai.backend.domain.dto.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * 서울시 OpenAPI 공통 응답 포맷.
 * 실제 최상위 키 이름(예: "culturalEventInfo")은 요청 서비스명과 같음.
 * TODO: 최상위 래퍼 키 이름이 이게 맞는지 실제 응답으로 확인 필요.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeoulEventApiResponse {

    @JsonProperty("list_total_count")
    private int listTotalCount;

    @JsonProperty("RESULT")
    private Result result;

    @JsonProperty("row")
    private List<SeoulEventApiItem> row;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("CODE")
        private String code;

        @JsonProperty("MESSAGE")
        private String message;
    }
}