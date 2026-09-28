# schema — 테이블 생성 SQL

팀 확정 DDL을 둡니다. **현재 기준: `01_tripai_ddl_v3.2.1.sql`** (ERDCloud 확정본 · MariaDB 호환 수정판)

| 항목 | 내용 |
|---|---|
| 범위 | 서울 **당일여행** (숙박 기능 제외) |
| 테이블 | 7개 — `users`, `event`, `place`, `favorite_event`, `trip_plan`, `trip_plan_interest`, `trip_item` |
| 외래키 | 8개 (식별 3 / 비식별 5) |
| DBMS | MySQL 8.0.16+ 또는 MariaDB (로컬 MariaDB 12.3에서 실행 확인) |
| 상세 정의 | 팀 공유 `TripAI_테이블정의서_v3.2.1.xlsx` |

## 실행 방법

스크립트가 `tripai` 데이터베이스까지 만들어 줍니다.

```bash
# MariaDB
mariadb -u root -p < database/schema/01_tripai_ddl_v3.2.1.sql

# MySQL
mysql -u root -p < database/schema/01_tripai_ddl_v3.2.1.sql
```

마지막에 `table_count = 7`, `fk_count = 8`이 나오면 정상입니다.

### 음식점 추천 기능을 사용할 때

v3.2.1을 만든 뒤 아래 마이그레이션을 **한 번만** 실행합니다. TourAPI의 `cat3` 원본값과
서비스 음식 분류를 저장해 한식·중식·일식·양식 조건 검색에 사용합니다.

```bash
mariadb -u root -p tripai < database/schema/02_place_food_category_v3.2.2.sql
```

이미 만든 DB에는 `01_...sql`을 다시 실행하지 말고 `02_...sql`만 적용합니다.

### 처음부터 다시 만들 때

```sql
DROP DATABASE IF EXISTS tripai;   -- 모든 데이터가 삭제됩니다
```

그다음 위 실행 방법을 다시 수행합니다.

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
