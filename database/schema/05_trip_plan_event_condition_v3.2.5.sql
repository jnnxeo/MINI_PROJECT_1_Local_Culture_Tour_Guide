-- TripAI DDL v3.2.4 이후 행사 검색 조건 저장 마이그레이션
-- 실행 전: database/schema/04_trip_plan_meal_food_cafe_v3.2.4.sql 이 적용되어 있어야 합니다.
-- 메인 화면 AI 추천에서 행사 ID 없이 날짜·행사 분야·지역·무료 여부로 행사를 골라 초안을 만들 때,
-- 그 조건을 초안에 남겨 조건 수정(API-PLAN-003)과 다시 추천(API-PLAN-004, 같은 조건에서 다른 행사)에 쓰기 위한 컬럼입니다.
-- 행사 상세에서 행사를 직접 골라 만든 초안은 모두 NULL(행사 유지)로 남습니다.

ALTER TABLE `tripai`.`trip_plan`
    ADD COLUMN `search_dates` VARCHAR(400) NULL COMMENT '방문 가능 날짜(YYYY-MM-DD, 쉼표 구분)' AFTER `cafe_yn`,
    ADD COLUMN `search_categories` VARCHAR(100) NULL COMMENT '행사 분야(화면 분야명, 쉼표 구분)' AFTER `search_dates`,
    ADD COLUMN `search_district` VARCHAR(20) NULL COMMENT '서울 자치구' AFTER `search_categories`,
    ADD COLUMN `search_free_yn` BOOLEAN NULL COMMENT '무료 행사만 여부' AFTER `search_district`;
