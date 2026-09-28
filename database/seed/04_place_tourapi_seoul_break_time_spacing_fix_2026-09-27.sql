-- TourAPI 서울 음식점 시드의 브레이크타임 보완 (띄어쓴 "준비 시간" 표현 누락분)
-- 배경: OpeningHoursParser가 "준비시간"만 인식하고 "준비 시간"(띄어쓰기)은 놓쳐,
--       아래 10건은 원문에 명확한 준비시간이 있는데도 break_open_time/close_time이 NULL로 남아 있었다.
-- 사전 적용: 01_place_tourapi_seoul_2026-09-27.sql, 02_place_tourapi_seoul_break_time_2026-09-27.sql,
--            03_place_tourapi_seoul_open_time_fix_2026-09-27.sql
-- 파서 자체 수정은 backend/.../external/tour/OpeningHoursParser.java 에 있다 (다음 수집부터는 재발하지 않음).
-- 요일별로 준비시간이 다른 곳([평일]/[주말])은 02와 같은 기준으로 첫 번째(평일) 구간만 반영한다.

UPDATE `place` SET `break_open_time` = '14:30:00', `break_close_time` = '17:30:00' WHERE `content_id` = '132892' AND `content_type_cd` = 39; -- 삼원가든
UPDATE `place` SET `break_open_time` = '14:30:00', `break_close_time` = '17:00:00' WHERE `content_id` = '2542167' AND `content_type_cd` = 39; -- 연화산
UPDATE `place` SET `break_open_time` = '15:00:00', `break_close_time` = '17:00:00' WHERE `content_id` = '2623110' AND `content_type_cd` = 39; -- 도원스타일
UPDATE `place` SET `break_open_time` = '15:00:00', `break_close_time` = '16:00:00' WHERE `content_id` = '2755513' AND `content_type_cd` = 39; -- 다래함박스텍
UPDATE `place` SET `break_open_time` = '15:00:00', `break_close_time` = '16:30:00' WHERE `content_id` = '2815129' AND `content_type_cd` = 39; -- 뉴웨이브서울
UPDATE `place` SET `break_open_time` = '15:00:00', `break_close_time` = '17:00:00' WHERE `content_id` = '2833427' AND `content_type_cd` = 39; -- 닭한마리 공릉본점
UPDATE `place` SET `break_open_time` = '14:00:00', `break_close_time` = '15:00:00' WHERE `content_id` = '2871081' AND `content_type_cd` = 39; -- 생강김밥 본점
UPDATE `place` SET `break_open_time` = '15:00:00', `break_close_time` = '17:00:00' WHERE `content_id` = '323408' AND `content_type_cd` = 39; -- 송림가
UPDATE `place` SET `break_open_time` = '14:30:00', `break_close_time` = '17:00:00' WHERE `content_id` = '403515' AND `content_type_cd` = 39; -- 고래불
UPDATE `place` SET `break_open_time` = '15:30:00', `break_close_time` = '16:30:00' WHERE `content_id` = '678899' AND `content_type_cd` = 39; -- 도일처

-- 참고: content_id 3443159(도락)는 "브레이크 타임"은 인식되지만 시간 표기가 "15-17"(분 단위 없음)이라
-- 이번 파서 수정 범위(HH:mm 형식) 밖이다. business_hours_text만 보존하고 break 컬럼은 NULL로 둔다.
