package com.tripai.backend.domain.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * 서울시 문화행사정보 API(culturalEventInfo) 원본 응답 한 건.
 * 필드명은 실제 응답 샘플로 확인한 값이다. 값은 전부 문자열로 온다.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeoulEventApiItem {

    @JsonProperty("CODENAME")
    private String category;        // "콘서트", "전시/미술" ... -> event_type (원문 그대로 저장)

    @JsonProperty("GUNAME")
    private String districtName;    // "강동구" -> district_name

    @JsonProperty("TITLE")
    private String title;           // -> event_name

    @JsonProperty("DATE")
    private String dateText;        // "2026-12-24~2026-12-24" -> date_text

    @JsonProperty("PLACE")
    private String place;           // -> event_place

    @JsonProperty("USE_FEE")
    private String useFeeText;      // 요금 안내 문구 -> use_fee

    @JsonProperty("INQUIRY")
    private String inquiry;         // 문의 전화/시간 -> tel_no (50자 제한)

    @JsonProperty("PROGRAM")
    private String program;         // 프로그램 소개 -> overview (우선)

    @JsonProperty("ETC_DESC")
    private String etcDesc;         // 기타 설명 -> overview (PROGRAM이 비었을 때)

    @JsonProperty("ORG_LINK")
    private String orgLink;         // 주최/예매 원본 페이지 -> homepage (우선)

    @JsonProperty("HMPG_ADDR")
    private String homepage;        // 서울문화포털 상세 (cultcode 포함) -> ID 추출 + homepage 대체

    @JsonProperty("MAIN_IMG")
    private String imageUrl;        // -> image_url

    @JsonProperty("STRTDATE")
    private String startDate;       // "2026-12-24 00:00:00.0" (시간이 붙어 옴)

    @JsonProperty("END_DATE")
    private String endDate;         // "2026-12-24 00:00:00.0"

    @JsonProperty("PRO_TIME")
    private String proTime;         // "19:30", "(일) 16:00" 같은 자유 텍스트 -> event_start_time

    @JsonProperty("LOT")
    private String lng;             // 경도 -> mapx

    @JsonProperty("LAT")
    private String lat;             // 위도 -> mapy

    @JsonProperty("IS_FREE")
    private String isFree;          // "유료" / "무료" -> free_yn
}