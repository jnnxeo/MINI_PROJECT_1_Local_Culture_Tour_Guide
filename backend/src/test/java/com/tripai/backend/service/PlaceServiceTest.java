package com.tripai.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.tripai.backend.domain.dto.RestaurantResponse;
import com.tripai.backend.domain.dto.RestaurantSearchResponse;
import com.tripai.backend.domain.entity.EventLocationView;
import com.tripai.backend.domain.entity.RestaurantView;
import com.tripai.backend.repository.PlaceMapper;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PlaceServiceTest {

    /** 넘겨받은 검색 조건을 기록하는 가짜 Mapper */
    static class FakePlaceMapper implements PlaceMapper {
        final Map<String, EventLocationView> events = new HashMap<>();
        List<RestaurantView> result = List.of();
        double lat, lng, minLat, maxLat, minLng, maxLng;
        int radius, limit, offset;
        int searchCalls;
        String cuisineType;
        LocalTime mealTime;

        @Override
        public Optional<EventLocationView> findEventLocation(String eventContentId) {
            return Optional.ofNullable(events.get(eventContentId));
        }

        @Override
        public List<RestaurantView> findRestaurantsNear(double lat, double lng, int radius, double minLat, double maxLat,
                                                        double minLng, double maxLng, String cuisineType, LocalTime mealTime,
                                                        int limit, int offset) {
            this.lat = lat;
            this.lng = lng;
            this.radius = radius;
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLng = minLng;
            this.maxLng = maxLng;
            this.cuisineType = cuisineType;
            this.mealTime = mealTime;
            this.limit = limit;
            this.offset = offset;
            searchCalls++;
            return result;
        }
    }

    private FakePlaceMapper mapper;
    private PlaceService service;

    @BeforeEach
    void setUp() {
        mapper = new FakePlaceMapper();
        service = new PlaceService(mapper);
        mapper.events.put("DEV-EV-001", EventLocationView.builder()
                .eventContentId("DEV-EV-001").mapx(new BigDecimal("126.9770000")).mapy(new BigDecimal("37.5796000"))
                .displayYn(true).build());
        mapper.events.put("NO-COORD", EventLocationView.builder().eventContentId("NO-COORD").displayYn(true).build());
        mapper.events.put("HIDDEN", EventLocationView.builder()
                .eventContentId("HIDDEN").mapx(new BigDecimal("126.9")).mapy(new BigDecimal("37.5")).displayYn(false).build());
    }

    @Test
    void 행사_좌표를_기준으로_기본_반경_1500m_10개를_찾는다() {
        service.searchRestaurants("DEV-EV-001", null, null, null, null, null);

        assertThat(mapper.lat).isEqualTo(37.5796);
        assertThat(mapper.lng).isEqualTo(126.977);
        assertThat(mapper.radius).isEqualTo(1500);
        assertThat(mapper.limit).isEqualTo(10);
        assertThat(mapper.offset).isZero();
    }

    @Test
    void 검색_사각형은_반경을_모두_덮는다() {
        service.searchRestaurants(null, 37.5796, 126.977, 1500, null, null);

        // GPT 검토 E7: 북쪽 약 1,499m(위도 37.5930808) 지점이 예전 사각형(111,320m/도) 밖이었다
        assertThat(37.5930808).isBetween(mapper.minLat, mapper.maxLat);
        // 원 위의 점(8방향)이 모두 사각형 안에 있어야 한다 — SQL 과 같은 반지름 6,371km 구 기준
        for (int bearing = 0; bearing < 360; bearing += 45) {
            double[] point = destination(37.5796, 126.977, bearing, 1500);
            assertThat(PlanRecommendService.distance(37.5796, 126.977, point[0], point[1])).isCloseTo(1500, within(0.01));
            assertThat(point[0]).isBetween(mapper.minLat, mapper.maxLat);
            assertThat(point[1]).isBetween(mapper.minLng, mapper.maxLng);
        }
    }

    @Test
    void page_와_size_를_곱한_값이_int_를_넘으면_400() {
        // GPT 검토 E10: 42,949,673 × 50 이 넘쳐 음수 OFFSET 이 DB 로 가던 문제
        assertStatus(() -> service.searchRestaurants("DEV-EV-001", null, null, null, 42_949_673, 50), HttpStatus.BAD_REQUEST);
        assertThat(mapper.searchCalls).isZero();
    }

    /** 구면에서 시작점으로부터 방위각·거리만큼 떨어진 점 [위도, 경도] */
    private static double[] destination(double lat, double lng, double bearingDegree, double meters) {
        double angular = meters / 6_371_000d;
        double bearing = Math.toRadians(bearingDegree);
        double lat1 = Math.toRadians(lat);
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(angular) + Math.cos(lat1) * Math.sin(angular) * Math.cos(bearing));
        double lng2 = Math.toRadians(lng) + Math.atan2(Math.sin(bearing) * Math.sin(angular) * Math.cos(lat1),
                Math.cos(angular) - Math.sin(lat1) * Math.sin(lat2));
        return new double[] {Math.toDegrees(lat2), Math.toDegrees(lng2)};
    }

    @Test
    void 두_번째_페이지는_size만큼_건너뛴다() {
        service.searchRestaurants("DEV-EV-001", null, null, 800, 2, 5);

        assertThat(mapper.limit).isEqualTo(5);
        assertThat(mapper.offset).isEqualTo(10);
        assertThat(mapper.radius).isEqualTo(800);
    }

    @Test
    void 명세_필드로_바꿔_주고_거리는_m_단위로_반올림한다() {
        mapper.result = List.of(RestaurantView.builder()
                .contentId("DEV-PL-001").placeName("[샘플] 경복궁 앞 한식당").addr("서울 종로구 사직로 125")
                .mapx(new BigDecimal("126.9731000")).mapy(new BigDecimal("37.5758000"))
                .openTime(LocalTime.of(11, 0)).closeTime(LocalTime.of(21, 0))
                .breakOpenTime(LocalTime.of(15, 0)).breakCloseTime(LocalTime.of(17, 0))
                .distance(547.63).build());

        RestaurantSearchResponse response = service.searchRestaurants("DEV-EV-001", null, null, null, null, null);
        RestaurantResponse item = response.items().get(0);

        assertThat(item.placeId()).isEqualTo("DEV-PL-001");
        assertThat(item.name()).isEqualTo("[샘플] 경복궁 앞 한식당");
        assertThat(item.lat()).isEqualTo(37.5758);
        assertThat(item.lng()).isEqualTo(126.9731);
        assertThat(item.openTime()).isEqualTo("11:00");
        assertThat(item.breakTime()).isEqualTo("15:00~17:00");
        assertThat(item.closeTime()).isEqualTo("21:00");
        assertThat(item.firstMenu()).isNull();
        assertThat(item.distance()).isEqualTo(548L);
    }

    @Test
    void 결과가_없으면_오류가_아니라_빈_목록() {
        assertThat(service.searchRestaurants("DEV-EV-001", null, null, null, null, null).items()).isEmpty();
    }

    @Test
    void 행사가_없거나_비표출이면_404() {
        assertStatus(() -> service.searchRestaurants("NO-SUCH", null, null, null, null, null), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.searchRestaurants("HIDDEN", null, null, null, null, null), HttpStatus.NOT_FOUND);
    }

    @Test
    void 행사_좌표가_없으면_400() {
        assertStatus(() -> service.searchRestaurants("NO-COORD", null, null, null, null, null), HttpStatus.BAD_REQUEST);
    }

    @Test
    void 기준_위치가_없거나_잘못되면_400() {
        assertStatus(() -> service.searchRestaurants(null, null, null, null, null, null), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.searchRestaurants(null, 37.5, null, null, null, null), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.searchRestaurants(null, 91.0, 126.9, null, null, null), HttpStatus.BAD_REQUEST);
    }

    @Test
    void 반경과_페이지_범위를_벗어나면_400() {
        assertStatus(() -> service.searchRestaurants("DEV-EV-001", null, null, 0, null, null), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.searchRestaurants("DEV-EV-001", null, null, 5001, null, null), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.searchRestaurants("DEV-EV-001", null, null, null, -1, null), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.searchRestaurants("DEV-EV-001", null, null, null, null, 51), HttpStatus.BAD_REQUEST);
        assertThat(mapper.searchCalls).isZero();
    }

    @Test
    void 음식종류와_식사시간을_그대로_매퍼에_전달한다() {
        service.searchRestaurants("DEV-EV-001", null, null, null, null, null, "KOREAN", "12:30");

        assertThat(mapper.cuisineType).isEqualTo("KOREAN");
        assertThat(mapper.mealTime).isEqualTo(LocalTime.of(12, 30));
    }

    @Test
    void 음식종류_ALL은_추천용_음식분류_전체로_전달하고_비어있으면_필터를_안_건다() {
        service.searchRestaurants("DEV-EV-001", null, null, null, null, null, "ALL", null);
        assertThat(mapper.cuisineType).isEqualTo("ALL");

        service.searchRestaurants("DEV-EV-001", null, null, null, null, null, "", "");
        assertThat(mapper.cuisineType).isNull();
        assertThat(mapper.mealTime).isNull();

        service.searchRestaurants("DEV-EV-001", null, null, null, null, null, null, null);
        assertThat(mapper.cuisineType).isNull();
        assertThat(mapper.mealTime).isNull();
    }

    @Test
    void 알수없는_음식종류는_400() {
        assertStatus(() -> service.searchRestaurants("DEV-EV-001", null, null, null, null, null, "이탈리안", null),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.searchRestaurants("DEV-EV-001", null, null, null, null, null, "OTHER", null),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void 식사시간_형식이_HHmm이_아니면_400() {
        assertStatus(() -> service.searchRestaurants("DEV-EV-001", null, null, null, null, null, null, "12시30분"),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.searchRestaurants("DEV-EV-001", null, null, null, null, null, null, "25:00"),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.searchRestaurants("DEV-EV-001", null, null, null, null, null, null, "24:00"),
                HttpStatus.BAD_REQUEST);
    }

    private static void assertStatus(Runnable call, HttpStatus expected) {
        try {
            call.run();
        } catch (PlaceSearchException exception) {
            assertThat(exception.getStatus()).isEqualTo(expected);
            return;
        }
        throw new AssertionError("PlaceSearchException(" + expected + ")이 발생해야 합니다.");
    }
}
