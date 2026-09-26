-- TripAI DDL v3.2.1 이후 음식점 추천용 분류 추가 마이그레이션
-- 실행 전: database/schema/01_tripai_ddl_v3.2.1.sql 이 적용되어 있어야 합니다.
-- 기존 place 데이터는 cuisine_type = 'OTHER'로 유지합니다.

ALTER TABLE `tripai`.`place`
    ADD COLUMN `tour_cat3_code` VARCHAR(20) NULL COMMENT 'TourAPI 음식점 세부분류코드' AFTER `activity_label_name`,
    ADD COLUMN `cuisine_type` VARCHAR(20) NOT NULL DEFAULT 'OTHER' COMMENT '서비스 음식분류(KOREAN, WESTERN, JAPANESE, CHINESE, OTHER)' AFTER `tour_cat3_code`;

-- 음식 종류로 먼저 후보를 좁힌 뒤, 기존 좌표 조건과 거리 계산을 적용한다.
CREATE INDEX `IX_PLACE_RESTAURANT_CUISINE`
    ON `tripai`.`place` (`display_yn`, `content_type_cd`, `cuisine_type`);
