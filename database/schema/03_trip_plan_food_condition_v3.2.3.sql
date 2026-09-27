-- TripAI DDL v3.2.1 이후 일정 추천 조건 추가 마이그레이션
-- 실행 전: database/schema/01_tripai_ddl_v3.2.1.sql 이 적용되어 있어야 합니다.
-- API-PLAN-001·003 의 foodPreference 와 음식점 추천의 식사 시간대(mealType)를 초안에 저장해
-- 초안 조회(API-PLAN-002)와 다시 추천(API-PLAN-004)에서 같은 조건을 쓰기 위한 컬럼입니다.
-- 기존 일정은 NULL(조건 없음)로 남습니다.

ALTER TABLE `tripai`.`trip_plan`
    ADD COLUMN `food_preference` VARCHAR(20) NULL COMMENT '음식선호(ALL, KOREAN, CHINESE, JAPANESE, WESTERN)' AFTER `transport_md`,
    ADD COLUMN `meal_type` VARCHAR(10) NULL COMMENT '식사시간대(LUNCH, DINNER)' AFTER `food_preference`;
