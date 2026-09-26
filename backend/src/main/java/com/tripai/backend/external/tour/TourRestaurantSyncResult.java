package com.tripai.backend.external.tour;

import java.util.List;

/** 한 TourAPI 목록 페이지 수집 결과. 좌표가 없거나 잘못된 항목은 저장하지 않고 ID를 남긴다. */
public record TourRestaurantSyncResult(
        int totalCount,
        int requestedCount,
        int savedCount,
        List<String> skippedContentIds
) {
}
