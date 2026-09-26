package com.tripai.backend.service;

import com.tripai.backend.domain.dto.RestaurantResponse;
import com.tripai.backend.domain.dto.RestaurantSearchResponse;
import com.tripai.backend.domain.entity.EventLocationView;
import com.tripai.backend.domain.entity.RestaurantView;
import com.tripai.backend.repository.PlaceMapper;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-PLACE-001 행사 주변 맛집 후보 조회 (FOOD-001).
 * 기준 위치는 eventId(행사 좌표) 또는 lat·lng. 둘 다 오면 eventId 를 쓴다.
 * 명세에 기본값이 없어 정한 값: 반경 기본 1500m·최대 5000m, size 기본 10·최대 50 (docs/07 [제안])
 */
@Service
public class PlaceService {

    static final int DEFAULT_RADIUS = 1500;
    static final int MAX_RADIUS = 5000;
    static final int DEFAULT_SIZE = 10;
    static final int MAX_SIZE = 50;

    private static final double METERS_PER_DEGREE_LAT = 111_320d;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final PlaceMapper placeMapper;

    public PlaceService(PlaceMapper placeMapper) {
        this.placeMapper = placeMapper;
    }

    @Transactional(readOnly = true)
    public RestaurantSearchResponse searchRestaurants(
            String eventId, Double lat, Double lng, Integer radius, Integer page, Integer size
    ) {
        int searchRadius = radius == null ? DEFAULT_RADIUS : radius;
        int searchPage = page == null ? 0 : page;
        int searchSize = size == null ? DEFAULT_SIZE : size;

        if (searchRadius <= 0 || searchRadius > MAX_RADIUS) {
            throw badRequest("검색 반경은 1~" + MAX_RADIUS + "m 사이로 입력해 주세요.");
        }
        if (searchPage < 0 || searchSize <= 0 || searchSize > MAX_SIZE) {
            throw badRequest("page 는 0 이상, size 는 1~" + MAX_SIZE + " 사이로 입력해 주세요.");
        }

        double[] center = resolveCenter(eventId, lat, lng);
        double latDelta = searchRadius / METERS_PER_DEGREE_LAT;
        double lngDelta = searchRadius / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(center[0])));

        List<RestaurantResponse> items = placeMapper.findRestaurantsNear(
                        center[0], center[1], searchRadius,
                        center[0] - latDelta, center[0] + latDelta,
                        center[1] - lngDelta, center[1] + lngDelta,
                        searchSize, searchPage * searchSize)
                .stream()
                .map(PlaceService::toResponse)
                .toList();
        return new RestaurantSearchResponse(items);
    }

    /** 기준 좌표 [위도, 경도]. 행사가 없으면 404, 좌표가 없으면 400 (FOOD-001 좌표 없음) */
    private double[] resolveCenter(String eventId, Double lat, Double lng) {
        if (eventId != null && !eventId.isBlank()) {
            EventLocationView event = placeMapper.findEventLocation(eventId)
                    .filter(found -> Boolean.TRUE.equals(found.getDisplayYn()))
                    .orElseThrow(() -> new PlaceSearchException(HttpStatus.NOT_FOUND, "존재하지 않는 행사입니다."));
            if (event.getMapx() == null || event.getMapy() == null) {
                throw badRequest("행사 위치 정보가 없어 주변 맛집을 찾을 수 없습니다. 위치를 직접 지정해 주세요.");
            }
            return new double[] {event.getMapy().doubleValue(), event.getMapx().doubleValue()};
        }

        if (lat == null || lng == null) {
            throw badRequest("행사(eventId) 또는 위치(lat, lng)를 함께 보내 주세요.");
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw badRequest("위치 값(lat, lng)을 확인해 주세요.");
        }
        return new double[] {lat, lng};
    }

    private static RestaurantResponse toResponse(RestaurantView view) {
        String breakTime = view.getBreakOpenTime() != null && view.getBreakCloseTime() != null
                ? format(view.getBreakOpenTime()) + "~" + format(view.getBreakCloseTime())
                : null;

        return new RestaurantResponse(
                view.getContentId(),
                view.getPlaceName(),
                view.getAddr(),
                view.getMapy() == null ? null : view.getMapy().doubleValue(),
                view.getMapx() == null ? null : view.getMapx().doubleValue(),
                format(view.getOpenTime()),
                breakTime,
                format(view.getCloseTime()),
                null,
                view.getImageUrl(),
                view.getDistance() == null ? null : Math.round(view.getDistance())
        );
    }

    private static String format(LocalTime time) {
        return time == null ? null : time.format(HH_MM);
    }

    private static PlaceSearchException badRequest(String message) {
        return new PlaceSearchException(HttpStatus.BAD_REQUEST, message);
    }
}
