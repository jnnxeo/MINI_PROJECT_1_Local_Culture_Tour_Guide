package com.tripai.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 를 켜는 설정.
 * BackendApplication은 .env 로딩 때문에 팀이 수정한 파일이라, 충돌을 피하려고 별도 클래스로 둔다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}