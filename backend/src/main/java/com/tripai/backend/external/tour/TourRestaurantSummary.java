package com.tripai.backend.external.tour;

/** areaBasedList2 음식점 목록에서 저장에 필요한 기본 정보만 정규화한 값이다. */
public record TourRestaurantSummary(
        String contentId,
        String name,
        String address,
        String longitude,
        String latitude,
        String imageUrl,
        String telNo,
        String cat3,
        RestaurantCuisine cuisine
) {
}
