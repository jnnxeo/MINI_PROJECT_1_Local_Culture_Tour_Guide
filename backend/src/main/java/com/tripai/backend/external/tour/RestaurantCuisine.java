package com.tripai.backend.external.tour;

import java.util.Map;

/** TourAPI 음식점 cat3를 TripAI 추천 조건에서 쓸 음식 분류로 변환한다. */
public enum RestaurantCuisine {
    KOREAN,
    WESTERN,
    JAPANESE,
    CHINESE,
    OTHER;

    private static final Map<String, RestaurantCuisine> BY_CAT3 = Map.of(
            "A05020100", KOREAN,
            "A05020200", WESTERN,
            "A05020300", JAPANESE,
            "A05020400", CHINESE
    );

    public static RestaurantCuisine fromCat3(String cat3) {
        if (cat3 == null || cat3.isBlank()) {
            return OTHER;
        }
        return BY_CAT3.getOrDefault(cat3, OTHER);
    }
}
