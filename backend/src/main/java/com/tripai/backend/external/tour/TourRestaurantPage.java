package com.tripai.backend.external.tour;

import java.util.List;

/** 서울 음식점 목록 한 페이지와 TourAPI의 전체 건수. */
public record TourRestaurantPage(int totalCount, List<TourRestaurantSummary> items) {
}
