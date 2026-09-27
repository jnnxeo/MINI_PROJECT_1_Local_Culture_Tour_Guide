# seed — 팀 공통 초기 데이터

개발·시연 시 모두 같은 음식점 추천 결과를 볼 수 있도록 TourAPI에서 수집한 `place` 데이터만 제공한다. 사용자 계정·비밀번호·개인정보는 포함하지 않는다.

## 현재 제공 파일

| 파일 | 내용 | 행 수 |
| --- | --- | ---: |
| `01_place_tourapi_seoul_2026-09-27.sql` | 서울 음식점 TourAPI 수집·전처리 결과 | 800 |
| `02_place_tourapi_seoul_break_time_2026-09-27.sql` | 원문에서 명확히 파싱한 브레이크타임 보완 | 282 |
| `03_place_tourapi_seoul_open_time_fix_2026-09-27.sql` | "24:00" 표기·깨진 시간값 때문에 준비시간이 영업시간으로 잘못 저장된 행 보정 | 7 |
| `04_place_tourapi_seoul_break_time_spacing_fix_2026-09-27.sql` | "준비 시간"처럼 띄어쓴 표현이라 브레이크타임이 비어 있던 행 보완 | 10 |

- 원본: 한국관광공사 TourAPI `KorService2`
- 수집일: 2026-09-27
- 초기 API 확인 총량: 서울 음식점 961건
- 현재 1~8페이지(800건) 수집 완료. 개발키 일일 호출 한도(1,000회)에 도달해 9~10페이지는 다음 날 보완한다.
- `INSERT IGNORE` 형식이라 `content_id` 중복 행은 건너뛴다.

## 적용 순서

```bash
# 프로젝트 루트에서 실행
mysql -u root -p tripai < database/schema/01_tripai_ddl_v3.2.1.sql
mysql -u root -p tripai < database/schema/02_place_food_category_v3.2.2.sql
mysql -u root -p tripai < database/seed/01_place_tourapi_seoul_2026-09-27.sql
mysql -u root -p tripai < database/seed/02_place_tourapi_seoul_break_time_2026-09-27.sql
mysql -u root -p tripai < database/seed/03_place_tourapi_seoul_open_time_fix_2026-09-27.sql
mysql -u root -p tripai < database/seed/04_place_tourapi_seoul_break_time_spacing_fix_2026-09-27.sql
```

이미 팀 DDL이 적용된 DB라면 마지막 다섯 명령만 실행한다. 적용 뒤 아래처럼 확인한다.

```sql
SELECT cuisine_type, COUNT(*)
FROM place
WHERE content_type_cd = 39
GROUP BY cuisine_type;
```

## 주의

- 이 파일은 **개발·시연용 공통 시드**다. 운영 환경에 그대로 적용하지 않는다.
- `02_...break_time...sql`은 원문에서 `준비시간`, `브레이크타임`, `휴게시간`, `휴식시간`으로 명확히 표기된 시간만 보완한다. 요일별·복수 영업 구간은 원문으로 보존한다.
- `03_...open_time_fix...sql`: 파서가 "24:00"(자정) 표기와 오타 섞인 시간값을 못 읽어 준비시간을 영업시간으로 잘못 저장했던 7건을 바로잡는다. 파서 자체도 같이 고쳤으므로 다음 수집부터는 재발하지 않는다. `00:00~24:00`(24시간 영업) 표현은 `open_time = close_time`이 되어 현재 `mealTime` 필터 SQL(자정 넘김만 가정)에서는 항상 제외되는 한계가 남아 있다 — 후속 이슈로 남긴다.
- `04_...break_time_spacing_fix...sql`: 파서가 "준비시간"만 인식하고 "준비 시간"(띄어쓰기)은 놓쳐 브레이크타임이 비어 있던 10건을 보완한다. 파서도 띄어쓴 표현을 인식하도록 고쳤다. `content_id=3443159`(도락)처럼 시간이 "15-17"(분 단위 없음)로 적힌 경우는 이번 범위 밖이라 그대로 NULL로 남아 있다.
- 적재 후 기준 통계(2026-09-27, 01~04 시드 전부 적용 후): 800건 / 영업시간 존재 760건 / 추천 후보(4개 음식 분류 + 좌표 + 영업시간 존재) 634건. 이 수치는 특정 식사 시각·브레이크타임을 적용하기 전의 기초 후보 수다.
- TourAPI 데이터 갱신 및 폐업 여부는 시연 전에 별도로 확인한다.
