-- TripAI DDL v3.2.3 이후 끼니별 음식 종류·카페 포함 조건 추가 마이그레이션
-- 실행 전: database/schema/03_trip_plan_food_condition_v3.2.3.sql 이 적용되어 있어야 합니다.
-- 메인 화면 AI 추천 모달과 나의 일정 조건 수정 팝업에서 점심·저녁 음식 종류를 따로 고르고
-- 카페(TourAPI cat3 A05020900 카페/전통찻집) 포함 여부를 정한 값을 초안에 저장해
-- 초안 조회(API-PLAN-002)와 조건 수정·다시 추천(API-PLAN-003·004)에서 같은 조건을 쓰기 위한 컬럼입니다.
-- 기존 일정은 끼니별 음식 종류가 NULL(food_preference 를 두 끼 모두에 적용), 카페는 FALSE 로 남습니다.

ALTER TABLE `tripai`.`trip_plan`
    ADD COLUMN `lunch_food_preference` VARCHAR(20) NULL COMMENT '점심 음식선호(ALL, KOREAN, CHINESE, JAPANESE, WESTERN)' AFTER `meal_type`,
    ADD COLUMN `dinner_food_preference` VARCHAR(20) NULL COMMENT '저녁 음식선호(ALL, KOREAN, CHINESE, JAPANESE, WESTERN)' AFTER `lunch_food_preference`,
    ADD COLUMN `cafe_yn` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '카페 포함 여부' AFTER `dinner_food_preference`;
