package com.tripai.backend.external.tour;

import com.tripai.backend.service.TourRestaurantSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * `tour-sync` 프로필일 때만 실행되는 TourAPI 음식점 수집 명령이다.
 * 일반 백엔드 서버 실행에서는 등록되지 않으므로 공개 API나 자동 수집으로 동작하지 않는다.
 */
@Component
@Profile("tour-sync")
@EnableConfigurationProperties(TourRestaurantSyncProperties.class)
public class TourRestaurantSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TourRestaurantSyncRunner.class);

    private final TourRestaurantSyncService syncService;
    private final TourRestaurantSyncProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final ConfigurableApplicationContext applicationContext;

    public TourRestaurantSyncRunner(
            TourRestaurantSyncService syncService,
            TourRestaurantSyncProperties properties,
            JdbcTemplate jdbcTemplate,
            ConfigurableApplicationContext applicationContext
    ) {
        this.syncService = syncService;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        validateRange();

        TourRestaurantSyncResult result = syncService.syncSeoulRestaurantPage(
                properties.getPageNo(), properties.getNumOfRows()
        );
        RestaurantCollectionSummary summary = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) AS collected_count,
                       COALESCE(SUM(CASE
                           WHEN display_yn = TRUE
                            AND cuisine_type IN ('KOREAN', 'WESTERN', 'JAPANESE', 'CHINESE')
                            AND open_time IS NOT NULL
                            AND close_time IS NOT NULL
                           THEN 1 ELSE 0 END), 0) AS recommendable_count
                  FROM place
                 WHERE content_type_cd = 39
                """,
                (rs, rowNum) -> new RestaurantCollectionSummary(
                        rs.getInt("collected_count"), rs.getInt("recommendable_count")
                )
        );

        log.info(
                "TourAPI 음식점 수집 완료: page={}, 요청={}, 저장={}, 건너뜀={}, TourAPI 전체={}, 누적 저장={}, 추천 가능={}",
                properties.getPageNo(), result.requestedCount(), result.savedCount(), result.skippedContentIds().size(),
                result.totalCount(), summary.collectedCount(), summary.recommendableCount()
        );

        applicationContext.close();
    }

    private void validateRange() {
        if (properties.getPageNo() < 1 || properties.getNumOfRows() < 1 || properties.getNumOfRows() > 100) {
            throw new IllegalArgumentException("tour-api.sync.page-no는 1 이상, num-of-rows는 1~100이어야 합니다.");
        }
    }

    private record RestaurantCollectionSummary(int collectedCount, int recommendableCount) {
    }
}
