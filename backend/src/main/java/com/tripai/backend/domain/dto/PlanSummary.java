package com.tripai.backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 내 여행 > 저장한 일정 목록 한 줄.
 * 목록 카드에 대표 행사 이미지·장소와 방문 시간·코스를 보여 주려고 선택한 행사와 일정 항목 요약을 함께 담는다.
 * 주의: @AllArgsConstructor 라 MyBatis 가 SELECT 컬럼 순서대로 채운다 — PlanMapper.xml 의 컬럼 순서를 필드 순서와 맞출 것.
 */
@Getter
@AllArgsConstructor
public class PlanSummary {
    private final Long tripPlanId;
    private final String title;
    private final LocalDate tripDate;
    private final Integer dDay;

    // 선택한(대표) 행사 — 행사가 삭제·비표출이면 null
    private final String eventId;
    private final String eventName;
    private final String eventImageUrl;
    private final String eventCategory;
    private final String district;
    private final String eventPlace;

    // 일정 항목 요약 — 첫 항목 시작 ~ 마지막 항목 종료(HH:mm), 항목 수, 방문 순서대로 이름
    private final String startTime;
    private final String endTime;
    private final Integer itemCount;
    @JsonIgnore
    private final String courseText;

    @JsonGetter("dDay")
    public Integer getDDay() {
        return dDay;
    }

    /** 방문 순서대로 장소·행사 이름 (SQL 에서 줄바꿈으로 이어 받은 값을 나눈다) */
    @JsonGetter("course")
    public List<String> getCourse() {
        if (courseText == null || courseText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(courseText.split("\n")).filter(name -> !name.isBlank()).toList();
    }
}
