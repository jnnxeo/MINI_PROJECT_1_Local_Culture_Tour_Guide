package com.tripai.backend.domain.dto.event;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

/**
 * EVENT-001 행사 상세 조회 응답 DTO
 * API-EVENT-002 GET /api/events/{eventId} 명세 기준
 * 필드명은 API 명세(camelCase, 영문 축약)를 그대로 따름 — DB 컬럼명과 다름 주의
 */
@Getter
@Setter
public class EventDetailResponse {

    private String eventId;        // event_content_id
    private String title;          // event_name
    private String category;       // event_type
    private LocalDate startDate;   // event_start_date
    private LocalDate endDate;     // event_end_date
    private String place;          // event_place
    private Double lat;            // mapy
    private Double lng;            // mapx
    private String fee;            // use_fee
    private Boolean freeYn;        // free_yn
    private String overview;       // overview
    private String imageUrl;       // image_url
    private String homepage;       // homepage (EVENT-002 공식안내 이동이 이 필드를 사용)
    private String inquiry;        // tel_no
    private String district;
    private String dateText;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean favorited;   // 로그인 사용자의 관심 행사 저장 여부 (하트 초기 상태)
}