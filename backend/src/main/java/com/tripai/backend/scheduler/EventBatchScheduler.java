package com.tripai.backend.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tripai.backend.service.EventBatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매일 새벽 3시(Asia/Seoul)에 서울시 행사 데이터를 수집·적재한다.
 * cron 형식: 초 분 시 일 월 요일 -> "0 0 3 * * *" = 매일 03:00:00
 * 스케줄링을 켜는 @EnableScheduling은 global/config/SchedulingConfig에 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventBatchScheduler {

    private final EventBatchService eventBatchService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void runNightlyBatch() {
        log.info("=== 행사 데이터 수집 배치 시작 (새벽 3시) ===");
        try {
            eventBatchService.collectAndSave();
        } catch (Exception e) {
            // TODO: 실패 알림(Slack 등)이 필요하면 팀과 협의해서 여기에 추가
            log.error("행사 데이터 수집 배치 실패", e);
        }
        log.info("=== 행사 데이터 수집 배치 종료 ===");
    }
}