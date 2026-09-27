package com.tripai.backend.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.tripai.backend.domain.dto.AiPlanInput;
import com.tripai.backend.domain.dto.AiPlanResponse;
import com.tripai.backend.domain.dto.EventCandidate;
import com.tripai.backend.domain.dto.PlanGenerateRequest;
import com.tripai.backend.domain.dto.PlanScheduleItem;
import com.tripai.backend.domain.dto.RestaurantCandidate;

@Service 
@RequiredArgsConstructor 
public class ApiPlanService {
    private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private static final String PLAN_GENERATION_PROMPT = """
        당신은 서울 문화행사 기반 당일치기 여행 일정 추천 전문가입니다.

        아래 INPUT_JSON에는 사용자의 여행 조건과 DB에서 사전 조회한 행사·맛집 후보가 포함되어 있습니다.
        오직 INPUT_JSON의 후보 데이터만 사용하여, 사용자가 실행할 수 있는 하루 일정을 구성하세요.

        [입력 데이터 해석]
        - userConditions: 사용자가 선택한 여행 날짜, 인원, 이동수단, 관심사, 행사 유형, 지역, 무료 여부 조건입니다.
        - eventCandidates: 여행 날짜와 조건을 기준으로 DB에서 조회된 행사 후보입니다.
        - restaurantCandidates: 행사 후보 주변 반경에서 조회된 맛집 후보입니다.
        - requiredEventId가 null이 아니면, eventCandidates 중 eventContentId가 requiredEventId와 같은 행사를 반드시 포함해야 합니다.

        [필수 규칙]
        1. eventCandidates와 restaurantCandidates에 없는 장소·행사는 절대로 추가하지 마세요.
        2. 행사 일정에는 eventCandidates.eventContentId를, 식당 일정에는 restaurantCandidates.contentId를
        placeContentId에 원문 그대로 반환하세요.
        3. requiredEventId가 존재하면 해당 행사를 반드시 포함하세요.
        해당 ID의 행사가 후보 목록에 없으면 일정을 임의로 만들지 말고 issues에 사유를 작성하세요.
        4. userConditions.tripDate가 eventStartDate와 eventEndDate 사이에 포함되는 행사만 선택하세요.
        5. eventStartTime과 eventEndTime이 모두 존재하면 해당 시간 범위 안에 행사를 배치하세요.
        6. 식당은 openTime~closeTime 사이에만 배치하고,
        breakOpenTime~breakCloseTime과 겹치지 않게 배치하세요.
        7. 점심 식사는 11:30~14:00, 저녁 식사는 17:30~20:00 사이를 우선 고려하세요.
        8. 사용자의 interests, eventTypes, districts, freeOnly 조건을 우선 반영하세요.
        9. 후보 장소는 반경 기준으로 사전 조회된 데이터입니다.
        mapx, mapy가 가까운 장소를 연속 배치하여 동선이 단순하도록 구성하세요.
        10. 정확한 이동시간은 계산하거나 단정하지 마세요.
            transportation에는 userConditions.transportation에 포함된 값 중 하나만 사용하세요.
        11. 하루 일정은 행사 1~3개와 식사 1~2개를 적절히 포함하세요.
            단, 조건에 맞는 후보가 부족하면 무리하게 채우지 마세요.
        12. 행사 시간이 없으면 90~120분을 기본 관람 시간으로 배정할 수 있습니다.
            이 경우 note에 "행사 운영시간 확인 필요"를 작성하세요.
        13. 일정 시작은 10:00 이후, 마지막 일정 종료는 20:00 이전을 우선으로 구성하세요.

        [출력 규칙]
        반드시 아래 JSON 형식만 반환하세요.
        Markdown, 코드 블록, JSON 이외의 설명은 절대로 작성하지 마세요.

        {
        "schedule": [
            {
            "sequence": 1,
            "startTime": "10:00",
            "endTime": "11:30",
            "placeType": "EVENT",
            "placeContentId": "eventContentId 또는 contentId 원문",
            "placeName": "후보 데이터의 행사명 또는 장소명",
            "reason": "사용자 관심사, 행사 유형, 지역 또는 동선을 고려한 선택 이유",
            "transportation": "WALK 또는 PUBLIC_TRANSIT 또는 NONE",
            "note": "운영시간 확인 등 필요한 경우에만 작성, 없으면 빈 문자열"
            }
        ],
        "issues": [
            "일정 생성에 필요한 후보가 부족하거나 조건 충돌이 있을 때만 작성"
        ]
        }

        [INPUT_JSON]
        %s
        """;

    public List<EventCandidate> generateEventCandiate(PlanGenerateRequest request){
        return null;
    }

    public List<RestaurantCandidate> generateRestaurantCandiate(PlanGenerateRequest request){
        return null;
    }

    public List<PlanScheduleItem> generatePlan(AiPlanInput aiPlanInput) {
        try {
            // 1. AiPlanInput DTO -> JSON 문자열
            String inputJson = objectMapper.writeValueAsString(aiPlanInput);

            // 2. OpenAI Responses API 요청 JSON 구성
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("instructions", PLAN_GENERATION_PROMPT);
            requestBody.put("input", inputJson);
            requestBody.put("store", false);

            // JSON 형태 응답 강제
            ObjectNode text = requestBody.putObject("text");
            ObjectNode format = text.putObject("format");
            format.put("type", "json_object");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_RESPONSES_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(requestBody)
                    ))
                    .build();

            // 3. OpenAI API 호출
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "OpenAI API 호출 실패: " + response.body()
                );
            }

            // 4. Responses API 응답에서 AI가 작성한 JSON 문자열 추출
            String aiOutputJson = extractOutputText(response.body());

            // 5. AI JSON -> AiPlanResponse DTO
            AiPlanResponse aiPlanResponse = objectMapper.readValue(
                    aiOutputJson,
                    AiPlanResponse.class
            );

            // 6. AI가 후보에 없는 ID를 반환하지 않았는지 서버에서 검증
            validateSchedule(aiPlanResponse.getSchedule(), aiPlanInput);

            // 7. 최종 일정 목록 반환
            return aiPlanResponse.getSchedule();

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 요청 또는 응답 JSON 변환에 실패했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI 일정 생성 요청이 중단되었습니다.", e);
        } catch (IOException e) {
            throw new IllegalStateException("OpenAI API 통신 중 오류가 발생했습니다.", e);
        }
    }

    private String extractOutputText(String responseBody)
            throws JsonProcessingException {

        JsonNode root = objectMapper.readTree(responseBody);

        // output[0]으로 고정하면 안 됨. output 배열을 순회해야 함.
        for (JsonNode outputItem : root.path("output")) {
            for (JsonNode content : outputItem.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText();
                }
            }
        }

        throw new IllegalStateException("OpenAI 응답에서 일정 JSON을 찾지 못했습니다.");
    }

    private void validateSchedule(
            List<PlanScheduleItem> schedule,
            AiPlanInput aiPlanInput
    ) {
        Set<String> eventIds = aiPlanInput.getEventCandidates().stream()
                .map(EventCandidate::getEventContentId)
                .collect(Collectors.toSet());

        Set<String> restaurantIds = aiPlanInput.getRestaurantCandidates().stream()
                .map(RestaurantCandidate::getContentId)
                .collect(Collectors.toSet());

        for (PlanScheduleItem item : schedule) {
            boolean validEvent = "EVENT".equals(item.getPlaceType())
                    && eventIds.contains(item.getPlaceContentId());

            boolean validRestaurant = "RESTAURANT".equals(item.getPlaceType())
                    && restaurantIds.contains(item.getPlaceContentId());

            if (!validEvent && !validRestaurant) {
                throw new IllegalStateException(
                        "AI가 후보 목록에 없는 장소를 반환했습니다: "
                                + item.getPlaceContentId()
                );
            }
        }
    }
}

