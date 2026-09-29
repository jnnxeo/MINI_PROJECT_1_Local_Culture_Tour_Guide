# schema — 테이블 생성 SQL

팀 확정 DDL을 둡니다. **현재 기준: `01_TripAI_최종_DDL_v3.2.5.sql`** — 01 기본 DDL(v3.2.1)에 02~05 마이그레이션을 모두 반영한 통합본입니다.

| 항목 | 내용 |
|---|---|
| 범위 | 서울 **당일여행** (숙박 기능 제외) |
| 테이블 | 7개 — `users`, `event`, `place`, `favorite_event`, `trip_plan`, `trip_plan_interest`, `trip_item` |
| 구조 | 컬럼 85개 · 외래키 8개 (식별 3 / 비식별 5) · 조회 인덱스 11개 |
| DBMS | MySQL 8.0.16+ 또는 MariaDB (로컬 MariaDB 12.3에서 실행 확인) |
| 상세 정의 | 팀 공유 `TripAI_테이블정의서_v3.2.1.xlsx` |

## 어떤 파일을 실행하나요?

| 내 상황 | 실행할 파일 |
|---|---|
| **DB를 처음 만든다** | `01_TripAI_최종_DDL_v3.2.5.sql` **하나만** (02~05는 실행하지 않음 — 이미 들어 있어 컬럼 중복 오류가 납니다) |
| 예전 `01_tripai_ddl_v3.2.1.sql`로 만든 DB를 쓰고 있다 | 통합본은 실행하지 말고, **빠진 마이그레이션(02~05)만** 순서대로 |

### DB를 처음 만들 때

스크립트가 `tripai` 데이터베이스까지 만들어 줍니다.

```bash
# MariaDB
mariadb -u root -p < database/schema/01_TripAI_최종_DDL_v3.2.5.sql

# MySQL
mysql -u root -p < database/schema/01_TripAI_최종_DDL_v3.2.5.sql
```

확인 — 테이블 7개, 외래키 8개가 나오면 정상입니다.

```sql
SELECT COUNT(*) AS table_count FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tripai';
SELECT COUNT(*) AS fk_count FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = 'tripai';
```

### 예전 v3.2.1로 만든 DB를 쓰고 있을 때

아래 컬럼이 없으면 해당 마이그레이션을 **한 번만** 실행합니다. 이미 있는 것은 다시 실행하지 않습니다.

```sql
USE tripai;
SHOW COLUMNS FROM place LIKE 'cuisine_type';        -- 없으면 02 (음식점 음식 분류)
SHOW COLUMNS FROM trip_plan LIKE 'food_preference';  -- 없으면 03 (음식 종류·식사 시간대)
SHOW COLUMNS FROM trip_plan LIKE 'cafe_yn';          -- 없으면 04 (끼니별 음식·카페)
SHOW COLUMNS FROM trip_plan LIKE 'search_dates';     -- 없으면 05 (메인 AI 추천 행사 조건)
```

```bash
mariadb -u root -p tripai < database/schema/02_place_food_category_v3.2.2.sql
mariadb -u root -p tripai < database/schema/03_trip_plan_food_condition_v3.2.3.sql
mariadb -u root -p tripai < database/schema/04_trip_plan_meal_food_cafe_v3.2.4.sql
mariadb -u root -p tripai < database/schema/05_trip_plan_event_condition_v3.2.5.sql
```

02~05 파일은 예전 DB를 쓰는 팀원을 위해 남겨 둡니다.

### 처음부터 다시 만들 때

```sql
DROP DATABASE IF EXISTS tripai;   -- 모든 데이터가 삭제됩니다
```

그다음 "DB를 처음 만들 때"를 다시 수행하고, 음식점 시드(`database/seed/README.md`)도 다시 적용합니다.

## 코드 작성 시 꼭 알아둘 점

| 항목 | 내용 |
|---|---|
| 좌표 컬럼 | `mapx` = **경도(lng)**, `mapy` = **위도(lat)** — 지도 API에 넘길 때 순서 주의 |
| `place` | `content_type_cd = 39`(음식점)만 저장 가능 (CHECK 제약) |
| `trip_item` 참조 | `item_type='EVENT'`면 `event_content_id`만, `'PLACE'`면 `place_content_id`만 값이 있어야 함. MariaDB 호환 때문에 DB CHECK를 뺐으므로 **Service/DTO에서 검증** |
| `trip_item` 순서 | `(trip_plan_id, seq_order)`가 UNIQUE → 순서를 UPDATE로 맞바꾸면 **중복 키 오류(1062)**. 일정 항목 수정은 *기존 항목 삭제 → 새 순서로 재삽입*을 한 트랜잭션에서 처리 |
| `trip_item` 중복 | 같은 일정에 같은 행사·같은 장소는 한 번만 (UNIQUE). PLACE 항목끼리 `event_content_id`가 NULL로 겹치는 것은 허용됨 |
| `trip_plan.save_yn` | `FALSE` = 저장 전 초안, `TRUE` = 저장한 일정(내 여행에 표시) |
| 시각 컬럼 | 모두 `TIME` 타입, API에서는 `"HH:mm"` 문자열 |
| 음식점 분류 | `tour_cat3_code`는 TourAPI 원본값, `cuisine_type`은 추천 필터용 서비스 분류 |

## 다음 버전에서 바뀔 예정 (ERD 반영 대기)

`place.first_menu` 추가 · `place.activity_label_name` 삭제 · `transport_md` → `transport_mode` · `break_close_time` → `break_end_time`

> 🔄 스키마를 수정하면 **팀 전체에 공유**하세요. 각자 DB를 다시 만들어야 합니다.
