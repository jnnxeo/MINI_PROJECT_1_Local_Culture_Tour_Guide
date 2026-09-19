# schema — 테이블 생성 SQL

`CREATE TABLE` 문을 담은 SQL 파일을 둡니다.

## 파일명 규칙

실행 순서대로 **숫자를 앞에** 붙입니다. (외래키 때문에 순서가 중요합니다)

```
01_create_member.sql
02_create_place.sql
03_create_trip_plan.sql
04_create_plan_item.sql
05_create_favorite.sql
```

## 실행 방법

```bash
mysql -u root -p tripai < database/schema/01_create_member.sql
```

## 작성 시 주의

```sql
CREATE TABLE member (
    member_id   BIGINT       NOT NULL AUTO_INCREMENT,
    email       VARCHAR(30)  NOT NULL,
    password    VARCHAR(255) NOT NULL,          -- BCrypt 해시 (60자+)
    nickname    VARCHAR(50)  NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id),
    UNIQUE KEY uq_member_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- **`utf8mb4`** 로 지정 (한글·이모지)
- `password`는 해시가 들어가므로 **255자** 확보
- 중복 방지가 필요한 컬럼에 `UNIQUE KEY`

> 🔄 스키마를 수정하면 **팀 전체에 공유**하세요. 각자 다시 실행해야 합니다.
