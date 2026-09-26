# seed — 팀 공통 초기 데이터

개발·시연 시 모두 같은 음식점 추천 결과를 볼 수 있도록 TourAPI에서 수집한 `place` 데이터만 제공한다. 사용자 계정·비밀번호·개인정보는 포함하지 않는다.

## 현재 제공 파일

| 파일 | 내용 | 행 수 |
| --- | --- | ---: |
| `01_place_tourapi_seoul_2026-09-27.sql` | 서울 음식점 TourAPI 수집·전처리 결과 | 800 |

- 원본: 한국관광공사 TourAPI `KorService2`
- 수집일: 2026-09-27
- 초기 API 확인 총량: 서울 음식점 961건
- 현재 1~8페이지(800건) 수집 완료. 개발키 일일 호출 한도(1,000회)에 도달해 9~10페이지는 다음 날 보완한다.
- `INSERT IGNORE` 형식이라 `source_content_id` 중복 행은 건너뛴다.

## 적용 순서

```bash
# 프로젝트 루트에서 실행
mysql -u root -p tripai < database/schema/01_tripai_ddl_v3.2.1.sql
mysql -u root -p tripai < database/schema/02_place_food_category_v3.2.2.sql
mysql -u root -p tripai < database/seed/01_place_tourapi_seoul_2026-09-27.sql
```

이미 팀 DDL이 적용된 DB라면 마지막 두 명령만 실행한다. 적용 뒤 아래처럼 확인한다.

```sql
SELECT cuisine_type, COUNT(*)
FROM place
WHERE content_type_cd = 39
GROUP BY cuisine_type;
```

## 주의

- 이 파일은 **개발·시연용 공통 시드**다. 운영 환경에 그대로 적용하지 않는다.
- 영업시간 원문은 보존했지만, 일부 장소의 브레이크타임·요일별 시간은 아직 구조화하지 않았다.
- TourAPI 데이터 갱신 및 폐업 여부는 시연 전에 별도로 확인한다.
