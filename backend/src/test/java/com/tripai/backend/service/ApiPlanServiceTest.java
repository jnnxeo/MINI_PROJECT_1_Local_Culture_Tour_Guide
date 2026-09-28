package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;
import com.tripai.backend.domain.dto.AiPlanInput;
import com.tripai.backend.domain.dto.AiPlanResponse;
import com.tripai.backend.domain.dto.EventCandidate;
import com.tripai.backend.domain.dto.PlanGenerateRequest;
import com.tripai.backend.domain.dto.RestaurantCandidate;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** OpenAI Responses API 호출 — 실제 키 없이 로컬 서버로 요청 형식과 응답·오류 처리를 확인한다 */
class ApiPlanServiceTest {

    private static final String TEST_KEY = "test-key-for-local-server";

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String responseBody = "";
    private volatile long delayMillis = 0;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void 프롬프트와_후보를_input_하나로_보내고_output_text_를_읽는다() throws Exception {
        responseBody = openAiResponse("""
                {"title":"고궁의 밤 하루","schedule":[
                  {"sequence":1,"startTime":"12:30","endTime":"13:30","placeType":"RESTAURANT","placeContentId":"PL-1","placeName":"한식당","reason":"가까운 한식당"},
                  {"sequence":2,"startTime":"18:00","endTime":"21:00","placeType":"EVENT","placeContentId":"EV-1","placeName":"고궁의 밤","reason":"선택한 행사"}],
                 "issues":[]}""");

        AiPlanResponse response = client(20).generate(input());

        JsonNode sent = objectMapper.readTree(requestBody.get());
        // json_object 형식은 input 에 'json' 단어가 있어야 한다 (instructions 에만 있으면 HTTP 400)
        assertThat(sent.has("instructions")).isFalse();
        assertThat(sent.path("input").asText()).containsIgnoringCase("json").contains("\"requiredEventId\":\"EV-1\"");
        assertThat(sent.path("model").asText()).isEqualTo("gpt-4.1-mini");
        assertThat(sent.path("store").asBoolean(true)).isFalse();
        assertThat(sent.path("text").path("format").path("type").asText()).isEqualTo("json_object");
        assertThat(authorization.get()).isEqualTo("Bearer " + TEST_KEY);

        assertThat(response.getTitle()).isEqualTo("고궁의 밤 하루");
        assertThat(response.getSchedule()).hasSize(2);
        assertThat(response.getSchedule().get(0).getPlaceContentId()).isEqualTo("PL-1");
        assertThat(response.getSchedule().get(1).getStartTime()).isEqualTo("18:00");
    }

    @Test
    void placeType_철자가_틀려도_후보_ID_로_종류를_정한다() {
        // 실제 gpt-4.1-mini 응답에서 나온 철자
        responseBody = openAiResponse("""
                {"title":"t","schedule":[
                  {"sequence":1,"startTime":"12:30","endTime":"13:30","placeType":"RESTARUANT","placeContentId":"PL-1","placeName":"한식당","reason":"r"},
                  {"sequence":2,"startTime":"18:00","endTime":"21:00","placeType":"event","placeContentId":"EV-1","placeName":"고궁의 밤","reason":"r"}],"issues":[]}""");

        AiPlanResponse response = client(20).generate(input());

        assertThat(response.getSchedule()).extracting(item -> item.getPlaceType()).containsExactly("RESTAURANT", "EVENT");
    }

    @Test
    void 후보에_없는_장소를_돌려주면_예외() {
        responseBody = openAiResponse("""
                {"title":"t","schedule":[{"sequence":1,"startTime":"12:30","endTime":"13:30","placeType":"RESTAURANT","placeContentId":"NOT-CANDIDATE","placeName":"x","reason":"x"}],"issues":[]}""");

        assertThatThrownBy(() -> client(20).generate(input()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOT-CANDIDATE");
    }

    @Test
    void 오류_응답이면_상태와_오류_코드만_남기고_키는_남기지_않는다() {
        status = 401;
        // 실제 401 응답 형태 — message 에 가려진 키 일부가 들어 있다
        responseBody = "{\"error\":{\"message\":\"Incorrect API key provided: test-key****rver.\","
                + "\"type\":\"invalid_request_error\",\"code\":\"invalid_api_key\"}}";

        assertThatThrownBy(() -> client(20).generate(input()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageContaining("invalid_api_key")
                .hasMessageNotContaining("test-key");
    }

    @Test
    void 제한_시간을_넘기면_예외() {
        delayMillis = 2500;
        responseBody = openAiResponse("{}");

        assertThatThrownBy(() -> client(1).generate(input()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("시간 초과");
    }

    @Test
    void 키가_비어_있으면_호출하지_않는다() {
        ApiPlanService blank = new ApiPlanService(objectMapper, url(), "  ", "gpt-4.1-mini", 20);

        assertThat(blank.isEnabled()).isFalse();
        assertThatThrownBy(() -> blank.generate(input())).isInstanceOf(IllegalStateException.class);
        assertThat(requestBody.get()).isNull();
    }

    private ApiPlanService client(long timeoutSeconds) {
        return new ApiPlanService(objectMapper, url(), TEST_KEY, "gpt-4.1-mini", timeoutSeconds);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses";
    }

    /** Responses API 응답 형태 — output 배열 안 message.content 의 output_text */
    private String openAiResponse(String outputText) {
        try {
            return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("id", "resp_test")
                    .set("output", objectMapper.createArrayNode().add(objectMapper.createObjectNode()
                            .put("type", "message")
                            .set("content", objectMapper.createArrayNode().add(objectMapper.createObjectNode()
                                    .put("type", "output_text")
                                    .put("text", outputText))))));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static AiPlanInput input() {
        return AiPlanInput.builder()
                .userConditions(PlanGenerateRequest.builder()
                        .tripDate(LocalDate.of(2026, 10, 3))
                        .visitStartTime("10:00")
                        .visitEndTime("21:00")
                        .foodPreference("ALL")
                        .requiredEventId("EV-1")
                        .build())
                .eventCandidates(List.of(EventCandidate.builder()
                        .eventContentId("EV-1")
                        .eventName("고궁의 밤")
                        .eventStartDate(LocalDate.of(2026, 10, 1))
                        .eventEndDate(LocalDate.of(2026, 10, 10))
                        .eventStartTime(LocalTime.of(18, 0))
                        .eventEndTime(LocalTime.of(21, 0))
                        .build()))
                .restaurantCandidates(List.of(RestaurantCandidate.builder()
                        .contentId("PL-1")
                        .placeName("한식당")
                        .cuisineType("KOREAN")
                        .openTime(LocalTime.of(11, 0))
                        .closeTime(LocalTime.of(21, 0))
                        .distanceMeters(300L)
                        .build()))
                .build();
    }
}
