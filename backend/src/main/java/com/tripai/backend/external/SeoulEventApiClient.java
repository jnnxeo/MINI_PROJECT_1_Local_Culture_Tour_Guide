package com.tripai.backend.external;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripai.backend.domain.dto.event.SeoulEventApiEnvelope;
import com.tripai.backend.domain.dto.event.SeoulEventApiItem;
import com.tripai.backend.domain.dto.event.SeoulEventApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 서울시 문화행사정보 API 호출.
 * URL: http://openapi.seoul.go.kr:8088/{인증키}/json/culturalEventInfo/{시작}/{끝}/
 * 한 번에 최대 1000건이라 전체(약 2만 건)는 페이지를 나눠 가져온다.
 */
@Slf4j
@Component
public class SeoulEventApiClient {

    private static final String SERVICE_NAME = "culturalEventInfo";
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 100; // 무한 루프 방지 안전장치

    private final RestClient restClient = RestClient.builder()
            .baseUrl("http://openapi.seoul.go.kr:8088")
            .build();
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public SeoulEventApiClient(@Value("${seoul.api.key:}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    /**
     * 전체 행사를 가져온다. 한 페이지라도 실패하면 예외를 던진다.
     * (일부만 받은 채로 성공처럼 끝나지 않게 하기 위함)
     */
    public List<SeoulEventApiItem> fetchAll() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("SEOUL_API_KEY가 설정되지 않았습니다 (.env 확인)");
        }

        List<SeoulEventApiItem> all = new ArrayList<>();
        int start = 1;

        for (int page = 0; page < MAX_PAGES; page++) {
            int end = start + PAGE_SIZE - 1;
            SeoulEventApiResponse response = fetchPage(start, end);
            List<SeoulEventApiItem> rows = response.getRow();

            if (rows == null || rows.isEmpty()) {
                break;
            }

            all.addAll(rows);
            log.info("서울시 API 수집: {}~{} ({}/{})", start, end, all.size(), response.getListTotalCount());

            if (all.size() >= response.getListTotalCount()) {
                break;
            }
            start += PAGE_SIZE;
        }

        return all;
    }

    private SeoulEventApiResponse fetchPage(int start, int end) {
        String path = String.format("/%s/json/%s/%d/%d/", apiKey, SERVICE_NAME, start, end);

        // Content-Type이 json이 아니어도 읽히도록 문자열로 받아 직접 파싱한다.
        String body = restClient.get().uri(path).retrieve().body(String.class);

        SeoulEventApiEnvelope envelope;
        try {
            envelope = objectMapper.readValue(body, SeoulEventApiEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("서울시 API 응답을 읽지 못했습니다 (start=" + start + ")", e);
        }

        SeoulEventApiResponse info = envelope.getCulturalEventInfo();
        if (info == null) {
            SeoulEventApiResponse.Result r = envelope.getResult();
            String reason = (r == null) ? "알 수 없는 응답 형식" : r.getCode() + " " + r.getMessage();
            throw new IllegalStateException("서울시 API 오류: " + reason);
        }

        SeoulEventApiResponse.Result result = info.getResult();
        if (result != null && result.getCode() != null
                && !"INFO-000".equals(result.getCode()) && !"INFO-200".equals(result.getCode())) {
            // INFO-200 = 해당 범위에 데이터 없음(끝에 도달). 그 외는 오류.
            throw new IllegalStateException("서울시 API 오류: " + result.getCode() + " " + result.getMessage());
        }

        return info;
    }
}