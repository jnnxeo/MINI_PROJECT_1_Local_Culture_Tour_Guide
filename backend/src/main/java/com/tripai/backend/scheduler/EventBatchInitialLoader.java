package com.tripai.backend.scheduler;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.tripai.backend.service.EventBatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버 시작 직후 한 번 수집한다 (초기 적재용).
 * batch.event.initial-load=true 일 때만 동작. 기본값은 false라서 평소엔 실행되지 않는다.
 * 첫 적재 후에는 .env의 BATCH_EVENT_INITIAL_LOAD를 지우거나 false로 되돌린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.event.initial-load", havingValue = "true")
public class EventBatchInitialLoader implements ApplicationRunner {

    private final EventBatchService eventBatchService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== 행사 데이터 초기 적재 시작 ===");
        try {
            eventBatchService.collectAndSave();
        } catch (Exception e) {
            // 서버 기동은 막지 않는다.
            log.error("행사 데이터 초기 적재 실패", e);
        }
        log.info("=== 행사 데이터 초기 적재 종료 ===");
    }
}