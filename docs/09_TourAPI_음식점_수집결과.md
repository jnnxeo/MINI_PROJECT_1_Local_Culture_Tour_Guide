# TourAPI 음식점 수집·전처리 결과

## 목적

문화행사 일정 추천에서 사용자가 고른 음식 종류와 식사 시간에 맞는 음식점을 찾기 위해, TourAPI 음식점 데이터를 `place` 테이블에 사전 적재한다. 사용자 요청마다 외부 API를 호출하지 않으며, 추천 조회는 DB에서 수행한다.

## 수집 범위와 현황

| 항목 | 값 |
| --- | --- |
| 원본 API | 한국관광공사 TourAPI `KorService2` |
| 목록 API | `areaBasedList2` |
| 상세 API | `detailIntro2` |
| 조회 조건 | 서울(`areaCode=1`), 음식점(`contentTypeId=39`) |
| 최초 총량 | 961건 |
| 수집 완료 | 800건 (1~8페이지, 페이지당 100건) |
| 추천 가능 | 635건 |
| 미수집 | 약 161건 (개발키 일일 한도 소진 후 보완 예정) |

## 전처리 규칙

1. 목록 API에서 `contentId`, 이름, 주소, 좌표, 대표 이미지, `cat3`를 수집한다.
2. 상세 API에서 `opentimefood`를 받아 영업시간 원문을 `business_hours_text`에 보존한다.
3. 영업 시작·종료 시간을 파싱할 수 있으면 `open_time`, `close_time`에 넣는다.
4. TourAPI `cat3`를 서비스용 음식 분류로 변환한다.

| DB 값 | 화면 표기 | 설명 |
| --- | --- | --- |
| `KOREAN` | 한식 | TourAPI 한식 세부 분류 |
| `WESTERN` | 양식 | 양식 세부 분류 |
| `JAPANESE` | 일식 | 일식 세부 분류 |
| `CHINESE` | 중식 | 중식 세부 분류 |
| `OTHER` | 기타 | 카페·기타·분류 불명. 자동 추천 후보에서 제외 |

## 자동 추천 후보 기준

아래를 모두 만족할 때만 후보가 된다.

- `display_yn = true`
- `content_type_cd = 39`
- 음식 분류가 한식·양식·일식·중식 중 하나
- 위도·경도와 영업 시작·종료 시간이 존재
- 사용자가 선택한 식사 시작 시간에 영업 중

정렬은 행사 좌표와 음식점 좌표의 거리 오름차순으로 한다. 현재 DDL에는 좌표가 있으므로 SQL의 Haversine 거리 계산 또는 서비스 레이어 계산으로 구현할 수 있다.

## 한계와 다음 작업

- 영업시간 원문은 요일별·브레이크타임 표현이 다양하다. `준비시간`·`브레이크타임`·`휴게시간`·`휴식시간`으로 명확히 적힌 282건은 별도 시드에서 구조화했지만, 요일별·복수 구간은 `business_hours_text`를 함께 검토해야 한다.
- `00:00~01:00`처럼 자정을 넘는 영업시간은 별도 판정이 필요하다.
- TourAPI가 폐업 여부를 보장하지 않는다. 시연 후보는 링크·전화·최종 확인을 거친다.
- API 개발키 한도가 일 1,000회라 전체 재수집은 페이지 단위로 실행한다.

## 관련 파일

- `backend/src/main/java/com/tripai/backend/external/tour/`
- `backend/src/main/java/com/tripai/backend/service/TourRestaurantSyncService.java`
- `backend/src/main/java/com/tripai/backend/external/tour/TourRestaurantSyncRunner.java`
- `database/schema/02_place_food_category_v3.2.2.sql`
- `database/seed/01_place_tourapi_seoul_2026-09-27.sql`
- `docs/08_TourAPI_음식점_수집.md`
