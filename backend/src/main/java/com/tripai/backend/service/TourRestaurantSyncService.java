package com.tripai.backend.service;

import com.tripai.backend.external.tour.OpeningHours;
import com.tripai.backend.external.tour.OpeningHoursParser;
import com.tripai.backend.external.tour.TourApiClient;
import com.tripai.backend.external.tour.TourRestaurantDetail;
import com.tripai.backend.external.tour.TourRestaurantPage;
import com.tripai.backend.external.tour.TourRestaurantRecord;
import com.tripai.backend.external.tour.TourRestaurantSummary;
import com.tripai.backend.external.tour.TourRestaurantSyncResult;
import com.tripai.backend.repository.TourRestaurantMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서울 음식점 한 페이지를 TourAPI에서 수집해 place에 upsert한다.
 * 개발계정 호출 한도를 고려해 한 번에 한 페이지만 실행하도록 두며, 공개 HTTP API로 노출하지 않는다.
 */
@Service
public class TourRestaurantSyncService {

    private static final Pattern DISTRICT_PATTERN = Pattern.compile("(?:서울특별시|서울시|서울)\\s+([^\\s]+구)");

    private final TourApiClient tourApiClient;
    private final TourRestaurantMapper tourRestaurantMapper;

    public TourRestaurantSyncService(TourApiClient tourApiClient, TourRestaurantMapper tourRestaurantMapper) {
        this.tourApiClient = tourApiClient;
        this.tourRestaurantMapper = tourRestaurantMapper;
    }

    @Transactional
    public TourRestaurantSyncResult syncSeoulRestaurantPage(int pageNo, int numOfRows) {
        TourRestaurantPage page = tourApiClient.getSeoulRestaurants(pageNo, numOfRows);
        int savedCount = 0;
        List<String> skippedContentIds = new ArrayList<>();

        for (TourRestaurantSummary summary : page.items()) {
            Optional<TourRestaurantRecord> restaurant = toRecord(summary);
            if (restaurant.isEmpty()) {
                skippedContentIds.add(summary.contentId());
                continue;
            }

            tourRestaurantMapper.upsertRestaurant(restaurant.get());
            savedCount++;
        }

        return new TourRestaurantSyncResult(
                page.totalCount(), page.items().size(), savedCount, List.copyOf(skippedContentIds)
        );
    }

    private Optional<TourRestaurantRecord> toRecord(TourRestaurantSummary summary) {
        if (summary.name() == null || summary.longitude() == null || summary.latitude() == null) {
            return Optional.empty();
        }

        final BigDecimal longitude;
        final BigDecimal latitude;
        try {
            longitude = new BigDecimal(summary.longitude());
            latitude = new BigDecimal(summary.latitude());
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }

        TourRestaurantDetail detail = tourApiClient.getRestaurantDetail(summary.contentId());
        Optional<OpeningHours> openingHours = OpeningHoursParser.parse(detail.businessHoursText());

        return Optional.of(new TourRestaurantRecord(
                summary.contentId(),
                summary.name(),
                summary.address(),
                districtFrom(summary.address()),
                longitude,
                latitude,
                summary.imageUrl(),
                summary.telNo(),
                summary.cat3(),
                summary.cuisine(),
                openingHours.map(OpeningHours::openTime).orElse(null),
                openingHours.map(OpeningHours::closeTime).orElse(null),
                detail.businessHoursText()
        ));
    }

    private String districtFrom(String address) {
        if (address == null) {
            return null;
        }

        Matcher matcher = DISTRICT_PATTERN.matcher(address);
        return matcher.find() ? matcher.group(1) : null;
    }
}
