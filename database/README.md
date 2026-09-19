# Database (MySQL / MariaDB)

데이터베이스 관련 파일을 모아두는 디렉토리입니다.

> 📌 **현재 상태**: 폴더 구조와 설명만 있고 **실제 SQL 파일은 없습니다.**
> DB 담당자가 엑셀 `DB 테이블 정의` 시트를 보고 작성합니다.

> ⚠️ **DB 데이터 자체는 GitHub로 공유되지 않습니다.**
> 여기에는 **"어떻게 만드는지 적은 SQL 파일"** 만 올리고, 각자 자기 컴퓨터에서 실행합니다.

---

## 폴더 구조

```
database/
├── README.md        ← 지금 이 파일
├── erd/             ← ERD(테이블 관계도) 이미지·파일
├── schema/          ← 테이블 생성 SQL (CREATE TABLE)
└── seed/            ← 테스트용 초기 데이터 SQL (INSERT)
```

| 폴더 | 넣을 것 | 파일명 예시 |
|---|---|---|
| `erd/` | ERDCloud·dbdiagram 등에서 뽑은 이미지 | `erd_v1.png` |
| `schema/` | 테이블 생성·수정 SQL | `01_create_tables.sql` |
| `seed/` | 개발·테스트용 샘플 데이터 | `01_sample_places.sql` |

> 💡 파일명 앞에 **숫자**를 붙이면 실행 순서가 명확해집니다. (`01_`, `02_` …)

---

## 최초 DB 만들기

```sql
CREATE DATABASE tripai
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

> ⚠️ **`utf8mb4`가 아니면 한글·이모지가 `???`로 깨집니다.**

SQL 파일 실행:

```bash
mysql -u root -p tripai < database/schema/01_create_tables.sql
```

---

## 테이블 구성 (엑셀 `DB 테이블 정의` 기준)

```
member (회원)
   │ 1
   │
   │ N
trip_plan (저장된 일정)
   │ 1
   │
   │ N
plan_item (일정 속 개별 장소)   ──── N : 1 ────  place (장소·행사 정보)
```

### member — 회원

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `member_id` | BIGINT (PK, AUTO_INCREMENT) | 회원 번호 |
| `email` | VARCHAR(30) (UNIQUE) | 로그인 아이디 |
| `password` | VARCHAR(255) | **BCrypt 해시** (평문 저장 금지 🚫) |
| `nickname` | VARCHAR(50) | 닉네임 |
| `created_at` | DATETIME | 가입일시 |

### trip_plan — 저장된 일정

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `plan_id` | BIGINT (PK) | 일정 번호 |
| `member_id` | BIGINT (FK → member) | 작성한 회원 |
| `title` | VARCHAR(100) | 일정 제목 |
| `area_code` | VARCHAR(10) | 지역 코드 |
| `start_date` / `end_date` | DATE | 여행 시작일 / 종료일 |
| `headcount` | INT | 인원수 |
| `created_at` | DATETIME | 생성일시 |

### plan_item — 일정 속 개별 장소

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `plan_item_id` | BIGINT (PK) | 항목 번호 |
| `plan_id` | BIGINT (FK → trip_plan) | 소속 일정 |
| `content_id` | VARCHAR(20) (FK → place) | 장소 ID |
| `day_number` | INT | 며칠차 |
| `visit_order` | INT | 방문 순서 |
| `visit_time` | VARCHAR(10) | 방문 시간대 |
| `ai_reason` | VARCHAR(500) | AI가 생성한 추천 이유 |

### place — 장소·행사 정보

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `content_id` | VARCHAR(20) (PK) | 외부 API의 고유 ID |
| `content_type_id` | INT | 12=관광지, 32=숙박, 39=음식점 |
| `title` | VARCHAR(200) | 장소·행사명 |
| `addr` | VARCHAR(255) | 주소 |
| `mapx` / `mapy` | DECIMAL(10,7) | 경도 / 위도 |
| `overview` | TEXT | 소개 |
| `image` | VARCHAR(500) | 대표 이미지 URL |
| `area_code` | VARCHAR(10) | 지역 코드 |
| `collected_at` | DATETIME | 수집 시각 |

### ❗ 아직 정의되지 않은 테이블

즐겨찾기(FAV-01) 기능이 요구사항에 있지만 **테이블 정의가 없습니다.** 팀 논의가 필요합니다.

```sql
-- 예시안 (확정 아님)
CREATE TABLE favorite (
  favorite_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id    BIGINT NOT NULL,
  content_id   VARCHAR(20) NOT NULL,
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_member_content (member_id, content_id)   -- 중복 등록 방지
);
```

---

## 규칙

1. **컬럼명은 `snake_case`** (`start_date`) — API의 `camelCase`(`startDate`)와 구분
2. **비밀번호는 반드시 해시로** 저장 (BCrypt)
3. **스키마를 바꾸면** `schema/` SQL을 수정하고 **팀에 공유** — 각자 다시 실행해야 함
4. **실제 회원 데이터·개인정보는 절대 커밋 금지** — `seed/`에는 가짜 데이터만

---

## ⚠️ 커밋 주의사항

| 커밋 **O** | 커밋 **X** |
|---|---|
| `schema/*.sql` (테이블 구조) | DB 접속 정보·비밀번호 🔐 |
| `seed/*.sql` (가짜 샘플 데이터) | 실제 회원 정보 🔐 |
| `erd/*.png` (관계도) | `*.sqlite`, `*.session.sql` |
