package com.tripai.backend.external.tour;

import java.math.BigDecimal;
import java.time.LocalTime;

/** TourAPI 응답을 TripAI place 저장 형식으로 변환한 내부 값이다. */
public record TourRestaurantRecord(
        String contentId,
        String name,
        String address,
        String districtName,
        BigDecimal longitude,
        BigDecimal latitude,
        String imageUrl,
        String telNo,
        String tourCat3Code,
        RestaurantCuisine cuisineType,
        LocalTime openTime,
        LocalTime closeTime,
        String businessHoursText
) {
}
