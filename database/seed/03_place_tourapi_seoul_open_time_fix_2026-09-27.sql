-- TourAPI 서울 음식점 시드의 영업시간 보정
-- 배경: OpeningHoursParser가 "24:00"(자정) 표기와 깨진 시간값을 처리하지 못해,
--       01_...sql 적재 당시 아래 7건은 준비시간(브레이크타임) 구간이 영업시간으로 잘못 저장됐다.
--       (open_time = break_open_time, close_time = break_close_time 이 되어
--        mealTime 필터를 켜면 이 음식점들은 어떤 시간을 넣어도 항상 제외됐다.)
-- 사전 적용: 01_place_tourapi_seoul_2026-09-27.sql, 02_place_tourapi_seoul_break_time_2026-09-27.sql
-- 파서 자체 수정은 backend/.../external/tour/OpeningHoursParser.java 에 있다 (다음 수집부터는 재발하지 않음).

-- "12:00~24:00" 계열 — 자정(24:00 표기)을 00:00으로 정규화. 브레이크타임 값은 그대로 둔다.
UPDATE `place` SET `open_time` = '12:00:00', `close_time` = '00:00:00' WHERE `content_id` = '2733861' AND `content_type_cd` = 39; -- 서강팔경
UPDATE `place` SET `open_time` = '12:00:00', `close_time` = '00:00:00' WHERE `content_id` = '2758130' AND `content_type_cd` = 39; -- 오만지아
UPDATE `place` SET `open_time` = '11:15:00', `close_time` = '00:00:00' WHERE `content_id` = '2841097' AND `content_type_cd` = 39; -- 쏭타이 본점
UPDATE `place` SET `open_time` = '11:00:00', `close_time` = '00:00:00' WHERE `content_id` = '2847862' AND `content_type_cd` = 39; -- 꽁티드툴레아
UPDATE `place` SET `open_time` = '11:30:00', `close_time` = '00:00:00' WHERE `content_id` = '2849681' AND `content_type_cd` = 39; -- 긴꼬리초밥

-- "00:00~24:00"(24시간 영업) — open=close=00:00이 되는 표현상 한계가 있다.
-- mealTime 필터는 open<close 또는 open>close(자정 넘김)만 가정하므로, open=close인 이 값은
-- 현재 SQL 조건에서 항상 제외된다. 24시간 영업 판정은 후속 이슈로 남긴다.
UPDATE `place` SET `open_time` = '00:00:00', `close_time` = '00:00:00' WHERE `content_id` = '2850964' AND `content_type_cd` = 39; -- 양천뼈다귀본점

-- 원문 자체가 깨져 있어("11:30~22:3") 영업시간을 신뢰할 수 없다 — 전부 NULL 처리해 자동 추천 후보에서 제외한다.
UPDATE `place` SET `open_time` = NULL, `close_time` = NULL, `break_open_time` = NULL, `break_close_time` = NULL
 WHERE `content_id` = '2867691' AND `content_type_cd` = 39; -- 우텐더
