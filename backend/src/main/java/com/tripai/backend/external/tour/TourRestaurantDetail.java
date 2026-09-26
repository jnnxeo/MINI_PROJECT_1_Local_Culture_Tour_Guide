package com.tripai.backend.external.tour;

/** detailIntro2 음식점 소개 정보. 시간 원문은 파싱 전 그대로 보존한다. */
public record TourRestaurantDetail(
        String contentId,
        String businessHoursText,
        String closedDaysText,
        String firstMenu,
        String menuText,
        String packingInfo,
        String parkingInfo,
        String reservationInfo
) {
}
