package com.tripai.backend.external.tour;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RestaurantCuisineTest {

    @Test
    void tourApiCat3를_서비스음식분류로_변환한다() {
        assertThat(RestaurantCuisine.fromCat3("A05020100")).isEqualTo(RestaurantCuisine.KOREAN);
        assertThat(RestaurantCuisine.fromCat3("A05020200")).isEqualTo(RestaurantCuisine.WESTERN);
        assertThat(RestaurantCuisine.fromCat3("A05020300")).isEqualTo(RestaurantCuisine.JAPANESE);
        assertThat(RestaurantCuisine.fromCat3("A05020400")).isEqualTo(RestaurantCuisine.CHINESE);
    }

    @Test
    void 알수없는_분류는_OTHER로_처리한다() {
        assertThat(RestaurantCuisine.fromCat3("A05020700")).isEqualTo(RestaurantCuisine.OTHER);
        assertThat(RestaurantCuisine.fromCat3(null)).isEqualTo(RestaurantCuisine.OTHER);
    }
}
