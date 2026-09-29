-- =====================================================================
-- TripAI 최종 DDL v3.2.5 · 보고/신규 구축용
-- 출처: database/schema/01_tripai_ddl_v3.2.1.sql 및 02~05 마이그레이션
-- 범위: 서울 문화행사 + 음식점 기반 당일 여행 (숙박 기능 없음)
-- DBMS: MariaDB 12.3 / MySQL 8.0.16+ (CHECK 제약 사용)
--
-- 빈 tripai 스키마에서 1회 실행하는 통합본입니다.
-- 이미 01~05를 적용한 DB에는 재실행하지 마세요. 이 파일에는 DROP/DML이 없습니다.
-- 식별 관계: users→favorite_event, event→favorite_event,
--            trip_plan→trip_plan_interest (3개)
-- 비식별 관계: users→trip_plan, event→trip_plan,
--              trip_plan/event/place→trip_item (5개)
-- =====================================================================

SET NAMES utf8mb4;

-- (1) 데이터베이스 ---------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `tripai`;
USE `tripai`;

-- (2) 테이블 ---------------------------------------------------------------
-- 사용자 (인증)
CREATE TABLE `tripai`.`users` (
	`user_id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '사용자아이디',
	`email`	VARCHAR(60)	NOT NULL	COMMENT '이메일',
	`password`	VARCHAR(100)	NOT NULL	COMMENT '비밀번호',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성일시',
	PRIMARY KEY (`user_id`)
) ENGINE=InnoDB COMMENT='사용자';

-- 행사 (행사)
CREATE TABLE `tripai`.`event` (
	`event_content_id`	VARCHAR(50)	NOT NULL	COMMENT '이벤트콘텐츠아이디',
	`event_type`	VARCHAR(20)	NOT NULL	COMMENT '행사분류',
	`district_name`	VARCHAR(20)	NULL	COMMENT '자치구명',
	`addr`	VARCHAR(255)	NULL	COMMENT '주소',
	`event_name`	VARCHAR(200)	NOT NULL	COMMENT '행사명',
	`mapx`	DECIMAL(11,7)	NULL	COMMENT '경도',
	`mapy`	DECIMAL(10,7)	NULL	COMMENT '위도',
	`homepage`	VARCHAR(500)	NULL	COMMENT '홈페이지',
	`tel_no`	VARCHAR(50)	NULL	COMMENT '전화번호',
	`event_start_date`	DATE	NOT NULL	COMMENT '행사시작일',
	`event_end_date`	DATE	NOT NULL	COMMENT '행사종료일',
	`date_text`	VARCHAR(200)	NULL	COMMENT '일시내용',
	`event_start_time`	TIME	NULL	COMMENT '행사시작시간',
	`event_end_time`	TIME	NULL	COMMENT '행사종료시간',
	`event_place`	VARCHAR(300)	NULL	COMMENT '행사장소',
	`use_fee`	VARCHAR(500)	NULL	COMMENT '이용요금',
	`free_yn`	BOOLEAN	NULL	COMMENT '무료여부',
	`overview`	TEXT	NULL	COMMENT '개요',
	`image_url`	VARCHAR(1000)	NULL	COMMENT '이미지경로',
	`display_yn`	BOOLEAN	NOT NULL	DEFAULT TRUE	COMMENT '표출여부',
	`collected_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수집일시',
	`upd_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP	COMMENT '수정일시'
) ENGINE=InnoDB COMMENT='행사';

-- 장소 (장소)
CREATE TABLE `tripai`.`place` (
	`content_id`	VARCHAR(20)	NOT NULL	COMMENT '콘텐츠아이디',
	`content_type_cd`	INT	NOT NULL	COMMENT '콘텐츠타입코드',
	`place_name`	VARCHAR(200)	NOT NULL	COMMENT '장소명',
	`addr`	VARCHAR(255)	NULL	COMMENT '주소',
	`district_name`	VARCHAR(20)	NULL	COMMENT '자치구명',
	`mapx`	DECIMAL(11,7)	NOT NULL	COMMENT '경도',
	`mapy`	DECIMAL(10,7)	NOT NULL	COMMENT '위도',
	`image_url`	VARCHAR(500)	NULL	COMMENT '이미지경로',
	`homepage`	VARCHAR(300)	NULL	COMMENT '홈페이지',
	`tel_no`	VARCHAR(50)	NULL	COMMENT '전화번호',
	`activity_label_name`	VARCHAR(50)	NULL	COMMENT '활동유형명',
	`tour_cat3_code`	VARCHAR(20)	NULL	COMMENT 'TourAPI 음식점 세부분류코드',
	`cuisine_type`	VARCHAR(20)	NOT NULL DEFAULT 'OTHER'	COMMENT '서비스 음식분류(KOREAN, WESTERN, JAPANESE, CHINESE, OTHER)',
	`default_duration_min`	INT	NULL	COMMENT '기본소요시간(분)',
	`open_time`	TIME	NULL	COMMENT '영업시작시간',
	`close_time`	TIME	NULL	COMMENT '영업종료시간',
	`break_open_time`	TIME	NULL	COMMENT '브레이크시작시간',
	`break_close_time`	TIME	NULL	COMMENT '브레이크종료시간',
	`business_hours_text`	VARCHAR(500)	NULL	COMMENT '영업시간원문',
	`display_yn`	BOOLEAN	NOT NULL	DEFAULT TRUE	COMMENT '표출여부',
	`collected_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수집일시',
	`upd_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP	COMMENT '수정일시'
) ENGINE=InnoDB COMMENT='장소';

-- 즐겨찾기행사 (행사)
CREATE TABLE `tripai`.`favorite_event` (
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자아이디',
	`event_content_id`	VARCHAR(50)	NOT NULL	COMMENT '행사콘텐츠아이디',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성일시'
) ENGINE=InnoDB COMMENT='즐겨찾기행사';

-- 여행일정 (일정)
CREATE TABLE `tripai`.`trip_plan` (
	`trip_plan_id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '여행일정아이디',
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자아이디',
	`anchor_content_id`	VARCHAR(50)	NOT NULL	COMMENT '기준콘텐츠아이디',
	`title`	VARCHAR(100)	NOT NULL	COMMENT '일정명',
	`trip_date`	DATE	NOT NULL	COMMENT '여행일자',
	`visit_start_time`	TIME	NULL	COMMENT '방문시작가능시각',
	`visit_end_time`	TIME	NULL	COMMENT '방문종료시각',
	`save_yn`	BOOLEAN	NOT NULL	DEFAULT FALSE	COMMENT '저장여부',
	`ai_yn`	BOOLEAN	NOT NULL	DEFAULT FALSE	COMMENT 'AI생성여부',
	`transport_md`	VARCHAR(30)	NULL	COMMENT '이동수단',
	`food_preference`	VARCHAR(20)	NULL	COMMENT '음식선호(ALL, KOREAN, CHINESE, JAPANESE, WESTERN)',
	`meal_type`	VARCHAR(10)	NULL	COMMENT '식사시간대(LUNCH, DINNER)',
	`lunch_food_preference`	VARCHAR(20)	NULL	COMMENT '점심 음식선호(ALL, KOREAN, CHINESE, JAPANESE, WESTERN)',
	`dinner_food_preference`	VARCHAR(20)	NULL	COMMENT '저녁 음식선호(ALL, KOREAN, CHINESE, JAPANESE, WESTERN)',
	`cafe_yn`	BOOLEAN	NOT NULL DEFAULT FALSE	COMMENT '카페 포함 여부',
	`search_dates`	VARCHAR(400)	NULL	COMMENT '방문 가능 날짜(YYYY-MM-DD, 쉼표 구분)',
	`search_categories`	VARCHAR(100)	NULL	COMMENT '행사 분야(화면 분야명, 쉼표 구분)',
	`search_district`	VARCHAR(20)	NULL	COMMENT '서울 자치구',
	`search_free_yn`	BOOLEAN	NULL	COMMENT '무료 행사만 여부',
	`headcount`	INT	NOT NULL	COMMENT '인원수',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성일시',
	`upd_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP	COMMENT '수정일시',
	PRIMARY KEY (`trip_plan_id`)
) ENGINE=InnoDB COMMENT='여행일정';

-- 여행일정관심사 (일정)
CREATE TABLE `tripai`.`trip_plan_interest` (
	`interest_label`	VARCHAR(20)	NOT NULL	COMMENT '관심사명',
	`trip_plan_id`	BIGINT	NOT NULL	COMMENT '여행일정아이디'
) ENGINE=InnoDB COMMENT='여행일정관심사';

-- 일정항목 (일정)
CREATE TABLE `tripai`.`trip_item` (
	`trip_item_id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '일정항목아이디',
	`trip_plan_id`	BIGINT	NOT NULL	COMMENT '여행일정아이디',
	`seq_order`	INT	NOT NULL	COMMENT '방문순번',
	`item_type`	VARCHAR(10)	NOT NULL	COMMENT '항목구분',
	`event_content_id`	VARCHAR(50)	NULL	COMMENT '행사콘텐츠아이디',
	`place_content_id`	VARCHAR(20)	NULL	COMMENT '장소콘텐츠아이디',
	`start_time`	TIME	NOT NULL	COMMENT '방문시작시각',
	`duration_min`	INT	NOT NULL	COMMENT '소요시간(분)',
	`ai_reason`	VARCHAR(100)	NULL	COMMENT 'AI추천이유',
	`time_fix_yn`	BOOLEAN	NOT NULL	DEFAULT FALSE	COMMENT '시간고정여부',
	PRIMARY KEY (`trip_item_id`)
) ENGINE=InnoDB COMMENT='일정항목';

-- (3) 기본키(PRIMARY KEY) --------------------------------------------------
-- AUTO_INCREMENT 테이블(users/trip_plan/trip_item)은 CREATE TABLE 내부에서 PK 선언
-- 나머지 테이블은 아래에서 PK 추가
ALTER TABLE `tripai`.`event` ADD PRIMARY KEY (
	`event_content_id`
);
ALTER TABLE `tripai`.`place` ADD PRIMARY KEY (
	`content_id`
);
ALTER TABLE `tripai`.`favorite_event` ADD PRIMARY KEY (
	`user_id`,
	`event_content_id`
);
ALTER TABLE `tripai`.`trip_plan_interest` ADD PRIMARY KEY (
	`interest_label`,
	`trip_plan_id`
);
-- (4) 제약(UNIQUE / CHECK / FOREIGN KEY) --------------------------------------
ALTER TABLE `tripai`.`users` ADD CONSTRAINT `UK_USERS_EMAIL` UNIQUE (`email`);

ALTER TABLE `tripai`.`event` ADD CONSTRAINT `CK_EVENT_DATE` CHECK (event_end_date >= event_start_date);
ALTER TABLE `tripai`.`event` ADD CONSTRAINT `CK_EVENT_TIME` CHECK (event_start_time IS NULL OR event_end_time IS NULL OR event_end_time >= event_start_time);

ALTER TABLE `tripai`.`place` ADD CONSTRAINT `CK_PLACE_TYPE` CHECK (content_type_cd = 39);
ALTER TABLE `tripai`.`place` ADD CONSTRAINT `CK_PLACE_DURATION` CHECK (default_duration_min IS NULL OR default_duration_min > 0);

ALTER TABLE `tripai`.`favorite_event` ADD CONSTRAINT `FK_FAVORITE_EVENT_USER` FOREIGN KEY (
	`user_id`
)
REFERENCES `tripai`.`users` (
	`user_id`
)
ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE `tripai`.`favorite_event` ADD CONSTRAINT `FK_FAVORITE_EVENT_EVENT` FOREIGN KEY (
	`event_content_id`
)
REFERENCES `tripai`.`event` (
	`event_content_id`
)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `tripai`.`trip_plan` ADD CONSTRAINT `CK_TRIP_PLAN_HEADCOUNT` CHECK (headcount > 0);
ALTER TABLE `tripai`.`trip_plan` ADD CONSTRAINT `CK_TRIP_PLAN_TIME` CHECK (visit_start_time IS NULL OR visit_end_time IS NULL OR visit_end_time > visit_start_time);
ALTER TABLE `tripai`.`trip_plan` ADD CONSTRAINT `FK_TRIP_PLAN_USER` FOREIGN KEY (
	`user_id`
)
REFERENCES `tripai`.`users` (
	`user_id`
)
ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE `tripai`.`trip_plan` ADD CONSTRAINT `FK_TRIP_PLAN_ANCHOR_EVENT` FOREIGN KEY (
	`anchor_content_id`
)
REFERENCES `tripai`.`event` (
	`event_content_id`
)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `tripai`.`trip_plan_interest` ADD CONSTRAINT `FK_TRIP_PLAN_INTEREST_PLAN` FOREIGN KEY (
	`trip_plan_id`
)
REFERENCES `tripai`.`trip_plan` (
	`trip_plan_id`
)
ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `tripai`.`trip_item` ADD CONSTRAINT `UK_TRIP_ITEM_SEQ` UNIQUE (`trip_plan_id`, `seq_order`);
ALTER TABLE `tripai`.`trip_item` ADD CONSTRAINT `UK_TRIP_ITEM_EVENT` UNIQUE (`trip_plan_id`, `event_content_id`);
ALTER TABLE `tripai`.`trip_item` ADD CONSTRAINT `UK_TRIP_ITEM_PLACE` UNIQUE (`trip_plan_id`, `place_content_id`);
ALTER TABLE `tripai`.`trip_item` ADD CONSTRAINT `CK_TRIP_ITEM_TYPE` CHECK (item_type IN ('EVENT','PLACE'));
-- MariaDB 호환성:
-- CK_TRIP_ITEM_REF(복수 컬럼 배타 참조 CHECK)는 일부 MariaDB 버전에서
-- ERROR 1901을 발생시키므로 DB CHECK에서 제외한다.
-- 규칙: item_type='EVENT'이면 event_content_id만 값이 있어야 하고,
--       item_type='PLACE'이면 place_content_id만 값이 있어야 한다.
-- 위 규칙은 Service/DTO validation에서 검증한다.
ALTER TABLE `tripai`.`trip_item` ADD CONSTRAINT `CK_TRIP_ITEM_SEQ` CHECK (seq_order > 0);
ALTER TABLE `tripai`.`trip_item` ADD CONSTRAINT `CK_TRIP_ITEM_DURATION` CHECK (duration_min > 0);
ALTER TABLE `tripai`.`trip_item` ADD CONSTRAINT `FK_TRIP_ITEM_PLAN` FOREIGN KEY (
	`trip_plan_id`
)
REFERENCES `tripai`.`trip_plan` (
	`trip_plan_id`
)
ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE `tripai`.`trip_item` ADD CONSTRAINT `FK_TRIP_ITEM_EVENT` FOREIGN KEY (
	`event_content_id`
)
REFERENCES `tripai`.`event` (
	`event_content_id`
)
ON UPDATE CASCADE ON DELETE RESTRICT;
ALTER TABLE `tripai`.`trip_item` ADD CONSTRAINT `FK_TRIP_ITEM_PLACE` FOREIGN KEY (
	`place_content_id`
)
REFERENCES `tripai`.`place` (
	`content_id`
)
ON UPDATE CASCADE ON DELETE RESTRICT;

-- (5) 인덱스(조회 성능) ------------------------------------------------------
CREATE INDEX `IX_EVENT_SEARCH` ON `tripai`.`event` (display_yn, district_name, event_type);  -- 목록 검색(API-EVENT-001)
CREATE INDEX `IX_EVENT_PERIOD` ON `tripai`.`event` (event_start_date, event_end_date);  -- 월별 조회(기간 겹침)
CREATE INDEX `IX_EVENT_NAME` ON `tripai`.`event` (event_name);  -- 이름 검색
CREATE INDEX `IX_PLACE_SEARCH` ON `tripai`.`place` (display_yn, content_type_cd, district_name);  -- 후보 조회(API-PLACE-001)
CREATE INDEX `IX_PLACE_COORD` ON `tripai`.`place` (mapy, mapx);  -- 반경 검색
CREATE INDEX `IX_PLACE_RESTAURANT_CUISINE` ON `tripai`.`place` (display_yn, content_type_cd, cuisine_type);  -- 음식 종류별 후보 조회 (02)
CREATE INDEX `IX_FAV_USER` ON `tripai`.`favorite_event` (user_id, created_at);  -- 내 관심 행사 목록(최신순)
CREATE INDEX `IX_TRIP_PLAN_USER_DATE` ON `tripai`.`trip_plan` (user_id, save_yn, trip_date);  -- 내 여행 목록(API-PLAN-010)
CREATE INDEX `IX_TRIP_PLAN_ANCHOR` ON `tripai`.`trip_plan` (anchor_content_id);  -- FK 성능
CREATE INDEX `IX_TRIP_ITEM_EVENT` ON `tripai`.`trip_item` (event_content_id);  -- 행사 기준 조회
CREATE INDEX `IX_TRIP_ITEM_PLACE` ON `tripai`.`trip_item` (place_content_id);  -- 장소 기준 조회

-- 구조 요약: 테이블 7개, 컬럼 85개, 외래키 8개, 명시적 조회 인덱스 11개.
-- trip_item의 EVENT/PLACE 배타 참조는 MariaDB 호환성을 위해
-- DB CHECK가 아닌 서비스/DTO 검증 대상입니다.
