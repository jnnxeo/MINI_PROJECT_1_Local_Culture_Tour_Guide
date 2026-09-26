package com.tripai.backend.external.tour;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * 한국관광공사 국문 관광정보 서비스(KorService2) 호출 전용 클라이언트.
 * 브라우저가 아닌 서버에서만 호출하므로 서비스 키가 프론트엔드에 노출되지 않는다.
 */
@Component
@EnableConfigurationProperties(TourApiProperties.class)
public class TourApiClient {

    private static final int RESTAURANT_CONTENT_TYPE_ID = 39;
    private static final int SEOUL_AREA_CODE = 1;

    private final RestClient restClient;
    private final TourApiProperties properties;

    public TourApiClient(RestClient.Builder restClientBuilder, TourApiProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        this.properties = properties;
    }

    /** 서울 음식점 목록. pageNo는 TourAPI 규칙에 맞춰 1부터 시작한다. */
    public TourRestaurantPage getSeoulRestaurants(int pageNo, int numOfRows) {
        if (pageNo < 1 || numOfRows < 1 || numOfRows > 100) {
            throw new IllegalArgumentException("TourAPI 페이지는 1 이상이고 한 번에 1~100건만 조회할 수 있습니다.");
        }

        JsonNode body = request(uriBuilder -> commonQuery(uriBuilder.path("/areaBasedList2"))
                .queryParam("contentTypeId", RESTAURANT_CONTENT_TYPE_ID)
                .queryParam("areaCode", SEOUL_AREA_CODE)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .build());

        List<TourRestaurantSummary> restaurants = new ArrayList<>();
        for (JsonNode item : items(body)) {
            String contentId = text(item, "contentid");
            if (contentId == null) {
                continue;
            }

            String cat3 = text(item, "cat3");
            restaurants.add(new TourRestaurantSummary(
                    contentId,
                    text(item, "title"),
                    text(item, "addr1"),
                    text(item, "mapx"),
                    text(item, "mapy"),
                    firstText(item, "firstimage", "firstimage2"),
                    text(item, "tel"),
                    cat3,
                    RestaurantCuisine.fromCat3(cat3)
            ));
        }

        return new TourRestaurantPage(body.path("totalCount").asInt(), List.copyOf(restaurants));
    }

    /** 음식점 1건의 영업·메뉴 정보. 응답 원문은 수집 단계에서 보존한다. */
    public TourRestaurantDetail getRestaurantDetail(String contentId) {
        if (contentId == null || contentId.isBlank()) {
            throw new IllegalArgumentException("TourAPI 음식점 contentId가 필요합니다.");
        }

        JsonNode body = request(uriBuilder -> commonQuery(uriBuilder.path("/detailIntro2"))
                .queryParam("contentId", contentId)
                .queryParam("contentTypeId", RESTAURANT_CONTENT_TYPE_ID)
                .build());

        JsonNode item = firstItem(body);
        if (item == null) {
            throw new TourApiException("TourAPI 음식점 상세 정보가 없습니다. contentId=" + contentId);
        }

        return new TourRestaurantDetail(
                contentId,
                text(item, "opentimefood"),
                text(item, "restdatefood"),
                text(item, "firstmenu"),
                text(item, "treatmenu"),
                text(item, "packing"),
                text(item, "parkingfood"),
                text(item, "reservationfood")
        );
    }

    private UriBuilder commonQuery(UriBuilder uriBuilder) {
        if (!properties.hasServiceKey()) {
            throw new TourApiException("TOUR_API_SERVICE_KEY가 설정되지 않았습니다. backend/.env를 확인해 주세요.");
        }

        return uriBuilder
                .queryParam("serviceKey", normalizedServiceKey())
                .queryParam("MobileOS", properties.getMobileOs())
                .queryParam("MobileApp", properties.getMobileApp())
                .queryParam("_type", "json");
    }

    /** 포털이 URL-encoded 키를 주는 경우 한 번만 원문으로 되돌린 뒤 URI Builder에 맡긴다. */
    private String normalizedServiceKey() {
        String serviceKey = properties.getServiceKey();
        return serviceKey.contains("%")
                ? URLDecoder.decode(serviceKey, StandardCharsets.UTF_8)
                : serviceKey;
    }

    private JsonNode request(Function<UriBuilder, URI> uriFunction) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode header = response == null ? null : response.path("response").path("header");
            String resultCode = header == null ? null : text(header, "resultCode");
            if (!"0000".equals(resultCode)) {
                String resultMessage = header == null ? null : text(header, "resultMsg");
                throw new TourApiException("TourAPI 요청이 실패했습니다. code=" + resultCode + ", message=" + resultMessage);
            }

            return response.path("response").path("body");
        } catch (RestClientException exception) {
            throw new TourApiException("TourAPI에 연결하지 못했습니다.", exception);
        }
    }

    private static List<JsonNode> items(JsonNode body) {
        JsonNode itemNode = body.path("items").path("item");
        List<JsonNode> result = new ArrayList<>();
        if (itemNode.isArray()) {
            itemNode.forEach(result::add);
        } else if (itemNode.isObject()) {
            result.add(itemNode);
        }
        return result;
    }

    private static JsonNode firstItem(JsonNode body) {
        List<JsonNode> items = items(body);
        return items.isEmpty() ? null : items.get(0);
    }

    private static String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = text(node, fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }
}
