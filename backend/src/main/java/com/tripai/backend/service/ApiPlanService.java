package com.tripai.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tripai.backend.domain.dto.AiPlanInput;
import com.tripai.backend.domain.dto.AiPlanResponse;
import com.tripai.backend.domain.dto.EventCandidate;
import com.tripai.backend.domain.dto.PlanScheduleItem;
import com.tripai.backend.domain.dto.RestaurantCandidate;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OpenAI Responses API 로 일정 초안을 만든다 (AI-002·007·008, 외부 AI 연동은 docs/07 [제안]).
 * 키는 각자 backend/.env 의 OPENAI_API_KEY 에만 둔다. 키가 없으면 isEnabled() 가 false 라 호출하지 않는다.
 */
@Service
public class ApiPlanService implements AiPlanClient {

    private static final String PLAN_GENERATION_PROMPT = """
        당신은 서울 문화행사를 기준으로 당일 여행 일정을 짜는 도우미입니다.
        아래 INPUT_JSON에는 사용자의 조건(userConditions)과 서버가 미리 고른 행사 후보(eventCandidates)·맛집 후보(restaurantCandidates)가 들어 있습니다.
        오직 INPUT_JSON에 있는 후보만 사용해 하루 일정을 만들고, 결과는 JSON으로만 답하세요.

        [필수 규칙]
        1. 행사는 userConditions.requiredEventId와 eventContentId가 같은 행사 1개만 넣습니다. 다른 행사는 넣지 않습니다.
        2. 그 행사에 eventStartTime이 있으면 startTime은 반드시 eventStartTime과 같아야 하고, endTime은 eventEndTime을 넘지 않습니다.
           eventStartTime이 없으면 방문 가능 시간 안에서 90~120분을 배정하고, reason에 "행사 운영시간 확인 필요"를 적습니다.
        3. 식당은 restaurantCandidates의 contentId를 그대로 쓰고, 같은 식당을 두 번 넣지 않습니다.
        4. 식사 횟수: userConditions.mealType이 LUNCH면 점심 1번, DINNER면 저녁 1번, BOTH이거나 없으면 점심·저녁 각 1번입니다(맞는 후보가 없으면 가능한 만큼만).
           식사에는 cuisineType이 CAFE인 곳을 쓰지 않습니다.
           점심은 11:00~14:30 사이에, 저녁은 17:00~20:30 사이에 시작하고 식사는 60분으로 합니다.
        5. 식당은 openTime~closeTime 안에만 두고 breakOpenTime~breakCloseTime과 겹치지 않게 합니다. openTime과 closeTime이 같으면 24시간 영업입니다.
        6. 모든 일정은 userConditions.visitStartTime~visitEndTime 안에 두고, 일정끼리 시간이 겹치지 않게 합니다.
        7. 점심은 lunchFoodPreference, 저녁은 dinnerFoodPreference가 ALL이 아니면 cuisineType이 같은 식당만 고르고, 그중 distanceMeters가 작은(가까운) 곳을 우선합니다.
           ALL이면 KOREAN·CHINESE·JAPANESE·WESTERN 중에서 고릅니다.
        8. reason은 80자 이내 한국어로, INPUT_JSON에 있는 정보(거리, 영업시간, 음식 종류, 행사 일시 문구 등)만 근거로 씁니다. 없는 정보를 지어내지 않습니다.
        9. title은 행사와 하루의 특징을 담은 30자 이내 한국어 일정 제목입니다.
        10. includeCafe가 true이면 cuisineType이 CAFE인 후보 중 1곳을 60분 넣습니다. 카페는 점심(11:00~14:30)·저녁(17:00~20:30) 시간대가 아닌 빈 시간에 시작하고,
           다른 일정과 겹치지 않으며 영업시간·브레이크 규칙(5번)을 지킵니다. includeCafe가 false이면 카페를 넣지 않습니다.

        [출력 JSON 형식]
        {"title": "일정 제목",
         "schedule": [{"sequence": 1, "startTime": "HH:mm", "endTime": "HH:mm", "placeType": "EVENT 또는 RESTAURANT (철자 그대로)",
                       "placeContentId": "후보의 eventContentId 또는 contentId 원문", "placeName": "후보 이름", "reason": "선택 이유"}],
         "issues": ["조건을 모두 맞추지 못한 경우에만 사유"]}
        sequence는 시간 순서대로 1부터 매깁니다. Markdown이나 설명 없이 JSON만 반환합니다.

        [INPUT_JSON]
        %s
        """;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String responsesUrl;
    private final String apiKey;
    private final String model;
    private final Duration requestTimeout;

    public ApiPlanService(
            ObjectMapper objectMapper,
            @Value("${openai.responses-url:https://api.openai.com/v1/responses}") String responsesUrl,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4.1-mini}") String model,
            @Value("${openai.timeout-seconds:20}") long timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.responsesUrl = responsesUrl;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public boolean isEnabled() {
        return !apiKey.isBlank();
    }

    @Override
    public AiPlanResponse generate(AiPlanInput aiPlanInput) {
        if (!isEnabled()) {
            throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다.");
        }
        try {
            String inputJson = objectMapper.writeValueAsString(aiPlanInput);

            // json_object 형식은 input 메시지에 'json' 단어가 있어야 해서, 프롬프트에 데이터를 넣어 input 으로 보낸다
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("input", PLAN_GENERATION_PROMPT.formatted(inputJson));
            requestBody.put("store", false);
            requestBody.putObject("text").putObject("format").put("type", "json_object");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(responsesUrl))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // 오류 메시지에는 가려진 키 일부가 들어 있어(sk-xxxx****xxxx) 오류 코드·종류만 남긴다
                JsonNode error = objectMapper.readTree(response.body()).path("error");
                String code = error.path("code").asText(error.path("type").asText(""));
                throw new IllegalStateException("OpenAI API 호출 실패 (HTTP " + response.statusCode() + "): " + code);
            }

            AiPlanResponse aiPlanResponse = objectMapper.readValue(extractOutputText(response.body()), AiPlanResponse.class);
            validateCandidates(aiPlanResponse.getSchedule(), aiPlanInput);
            return aiPlanResponse;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 요청 또는 응답 JSON 변환에 실패했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI 일정 생성 요청이 중단되었습니다.", e);
        } catch (IOException e) {
            throw new IllegalStateException("OpenAI API 통신 중 오류가 발생했습니다(시간 초과 포함).", e);
        }
    }

    private String extractOutputText(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);

        // output 배열 순서가 바뀔 수 있어 첫 번째로 고정하지 않고 모두 살핀다
        for (JsonNode outputItem : root.path("output")) {
            for (JsonNode content : outputItem.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText();
                }
            }
        }
        throw new IllegalStateException("OpenAI 응답에서 일정 JSON을 찾지 못했습니다.");
    }

    /**
     * AI 가 후보에 없는 행사·식당을 돌려주지 않았는지 확인한다.
     * placeType 철자를 틀리는 경우가 있어(실제 응답 "RESTARUANT") 종류는 ID 가 어느 후보에 있는지로 정한다.
     */
    private void validateCandidates(List<PlanScheduleItem> schedule, AiPlanInput aiPlanInput) {
        if (schedule == null || schedule.isEmpty()) {
            throw new IllegalStateException("AI 응답에 일정이 없습니다.");
        }
        Set<String> eventIds = aiPlanInput.getEventCandidates().stream()
                .map(EventCandidate::getEventContentId)
                .collect(Collectors.toSet());
        Set<String> restaurantIds = aiPlanInput.getRestaurantCandidates().stream()
                .map(RestaurantCandidate::getContentId)
                .collect(Collectors.toSet());

        for (PlanScheduleItem item : schedule) {
            String id = item.getPlaceContentId();
            if (eventIds.contains(id) && !restaurantIds.contains(id)) {
                item.setPlaceType("EVENT");
            } else if (restaurantIds.contains(id) && !eventIds.contains(id)) {
                item.setPlaceType("RESTAURANT");
            } else {
                throw new IllegalStateException("AI가 후보 목록에 없는 장소를 반환했습니다: "
                        + item.getPlaceType() + " " + id);
            }
        }
    }
}
