# 07. 나의 일정 구현 기준 (요구사항정의서 08 대조)

> **기준 문서는 `08_TripAI_요구사항정의서`(요구사항·화면 목록·API 명세)와 테이블정의서/DDL v3.2.1입니다.**
> 이 문서는 새 규칙을 만드는 곳이 아니라, 1팀 나의 일정 구현이 명세의 어느 부분을 따르는지와
> 명세에 비어 있거나 서로 어긋나 **팀 확인이 필요한 부분**을 모아 둔 메모입니다.
>
> - 작성: 홍준서 · 이슈 #27 · 이전 판(임의 API·이름을 정의했던 초안)은 폐기하고 이 판으로 대체
> - 표시: **[명세]** 문서에 있는 내용 / **[제안]** 명세에 없어 임시로 정한 것 — 팀 확인 전까지 확정 아님
> - 진행 방식(강산님 확인, 09-25): 기준은 **요구사항 정의서 엑셀(08)**. 구현 중 추가가 필요한 항목(3장 [제안])은 엑셀에 추가하고 디스코드·카톡으로 공유합니다.

---

## 1. 담당 범위

역할 분담 3안(투표 확정) 기준: 1팀(준서·승진·석주) = 로그인·회원가입·**나의 일정**, 2팀 = 메인·검색 결과·문화행사 상세·**내 여행**

| 화면 (08 「화면 목록」) | URL | 담당 |
|---|---|---|
| SCR-008 AI 일정 생성 중 팝업 | - | 1팀 나의 일정 |
| SCR-009 나의 일정(저장 전 초안) | `/trips/draft` | 1팀 나의 일정 |
| SCR-010~015 조건 수정 · 다시 추천 · 방문 시간 변경 · 장소 검색·변경 · 장소 추가 · 항목 삭제 팝업 | - | 1팀 나의 일정 |
| SCR-016 내 여행 · SCR-017 저장 일정 상세·편집 · SCR-018 저장 일정 삭제 팝업 | `/my-trips`, `/my-trips/{tripId}` | 2팀 내 여행 |

> 참고: "일정 카드"는 **내 여행 > 저장한 일정 탭의 카드**(MY-002: 일정명·여행 날짜·D-day, API-PLAN-010 `{planId,title,visitDate,dDay}`)를 말합니다. 메인페이지 행사 카드(MAIN-005)와는 다릅니다.

> **팀 확인 필요:** MY-005는 "저장 일정을 **동일한 일정 편집 화면**으로 연다"고 되어 있습니다.
> SCR-017을 2팀이 만들 때 1팀 편집 화면(`PlanEditorPage`)을 재사용할지, 별도로 만들지 정해야 합니다.

---

## 2. 사용하는 API — [명세] 08 「API 명세」 그대로

| API ID | Method | Endpoint | 설명 | 나의 일정에서 쓰는 곳 |
|---|---|---|---|---|
| API-PLAN-001 | POST | `/api/plans/recommend` | 행사 1개 기준 맛집 조합 초안 생성 | 메인·행사 상세(2팀 화면)에서 호출 → `draftId`를 들고 `/trips/draft`로 이동 |
| API-PLAN-002 | GET | `/api/plans/drafts/{draftId}` | 저장 전 초안 조회 | 화면 진입·새로고침 복원(UX-005) |
| API-PLAN-003 | PATCH | `/api/plans/drafts/{draftId}/conditions` | 추천 조건 수정 | 조건 수정 팝업(SCR-010) |
| API-PLAN-004 | POST | `/api/plans/drafts/{draftId}/regenerate` | 조건으로 다시 추천 | 다시 추천(SCR-011), 조건 수정 후 |
| API-PLAN-005 | PATCH | `/api/plans/drafts/{draftId}/title` | 초안 제목 변경 | 일정 저장 시 반영 |
| API-PLAN-006 | PUT | `/api/plans/drafts/{draftId}/items` | 항목·시간·순서 일괄 수정 | 일정 저장 시 반영 |
| API-PLAN-007 | POST | `/api/plans/drafts/{draftId}/items` | 맛집 추가 | 일정 저장 시 반영 |
| API-PLAN-008 | DELETE | `/api/plans/drafts/{draftId}/items/{itemId}` | 항목 삭제 | 일정 저장 시 반영 |
| API-PLAN-009 | POST | `/api/plans` | 초안을 저장 일정으로 확정 | 일정 저장(TRIP-010) |
| API-PLACE-001 | GET | `/api/places/restaurants` | 행사 주변 맛집 후보 | 장소 변경·추가 팝업 |
| API-PLAN-010~013 | | `/api/plans`, `/api/plans/{planId}` | 저장 일정 목록·상세·수정·삭제 | 내 여행(2팀) 범위 — 사용 안 함 |

### 화면 편집 → API 반영 순서 [제안]
팝업에서 바꾼 내용은 화면에만 두고(UX-004 미저장 이탈 확인), **일정 저장**을 누르면 아래 순서로 보냅니다.

1. 삭제한 항목 → API-PLAN-008
2. 남은 항목의 시간·순서 → API-PLAN-006 (새 항목 추가 전에 맞춰야 옛 시간과 겹쳤다는 오류가 안 남)
3. 새로 추가·교체한 맛집 → API-PLAN-007 (장소 변경 = 기존 항목 삭제 + 새 항목 추가)
4. 최종 순서가 다르면 → API-PLAN-006
5. 제목이 바뀌었으면 → API-PLAN-005
6. 확정 → API-PLAN-009

---

## 3. 명세에 모양이 정해지지 않은 부분 — [제안]

08 API 명세는 `items:[...]`, `recommendationReasons:[...]`, `mapPoints`의 내부 모양을 정하지 않았습니다.
목업은 명세의 다른 API에 이미 있는 필드 이름을 가져다 아래처럼 임시로 맞췄습니다.

| 항목 | [제안] 모양 | 가져온 근거 |
|---|---|---|
| `items[]` | `itemId, type(EVENT·PLACE), placeId, sequence, startTime, durationMin` | API-PLAN-006 요청 필드 |
| 〃 화면 표시용 | `name, addr, lat, lng, imageUrl, openTime, breakTime, closeTime, timeFixed` | API-PLACE-001 응답 필드 / TRIP-004 "행사 고정 시간" |
| `recommendationReasons[]` | `{ itemId, reason }` | AI-007 항목별 추천 이유 |
| `mapPoints[]` | `{ sequence, lat, lng }` | MAP-001·TRIP-008 방문 순서 마커 |
| API-PLAN-002 추가 응답 | `selectedEvent`(API-PLAN-001 응답), `conditions`(API-PLAN-003 응답), `recommendationReasons` | 조건 수정 팝업 초기값·추천 이유 표시에 필요 |
| API-PLACE-001 `distance` | 미터 단위 | 단위 미정 |
| 추천 조건 `foodPreference` / `mealType` | `ALL·KOREAN·CHINESE·JAPANESE·WESTERN` / `LUNCH·DINNER` | 음식 종류·식사 시간대는 Figma 조건 팝업에 추가하는 [제안] |
| `tripType`, `transportMode` 값 | `DAY_TRIP` / `WALK_TRANSIT`, `WALK` | 값 목록 미정 |

### DB 대응 (테이블정의서·DDL v3.2.1) [제안]
| API | DB |
|---|---|
| `draftId` | `trip_plan.trip_plan_id` (`save_yn = FALSE`) — API-PLAN-009 후 `save_yn = TRUE` |
| `visitDate` / 조건 `startTime`·`endTime`·`transportMode` | `trip_date` / `visit_start_time`·`visit_end_time`·`transport_md` |
| `items[].type`·`placeId`·`sequence` | `trip_item.item_type` · `event_content_id` 또는 `place_content_id` · `seq_order` |
| `items[].timeFixed` · 추천 이유 | `trip_item.time_fix_yn` · `trip_item.ai_reason` |
| `lat` / `lng` | `mapy`(위도) / `mapx`(경도) |

---

## 4. 화면·목업이 지키는 규칙 — [명세] 근거

| 규칙 | 근거 | 위반 시 (API 명세 오류 코드) |
|---|---|---|
| 제목 1~100자 | TRIP-003, DDL `title VARCHAR(100)` | 400 (API-PLAN-005) |
| 시간 겹침·일정 범위(조건 startTime~endTime) 초과 차단 | TRIP-004 | 400 (API-PLAN-006), 422 (API-PLAN-007) |
| 행사 고정 시간 변경 불가 | TRIP-004 | 400 |
| 중복 장소 차단, 문화행사 일정당 최대 2개 | TRIP-006 | 409 (API-PLAN-007) |
| 핵심 문화행사 삭제 불가 | TRIP-007 | 409 (API-PLAN-008) |
| 이미 저장된 초안 재저장 불가 | TRIP-010 | 409 (API-PLAN-009) |
| 영업시간 미확인·영업시간 밖 맛집 안내 (저장은 막지 않음) | FOOD-002 | 화면 안내 |

---

## 5. 명세끼리 어긋나는 부분 — 팀 확인 필요 (1팀 나의 일정에 영향 있는 것만)

제가 임의로 정하지 않고 목록으로만 남깁니다. 점검 회의에서 결정해 주세요.

| # | 내용 | 어긋나는 문서 |
|---|---|---|
| 1 | 08 「DB 테이블 정의」 시트는 `member`, `plan_item`, `area_code` 등 **구버전**. 실제 DB는 테이블정의서/DDL v3.2.1(`users`, `trip_item` …) | 08 DB 시트 ↔ DDL v3.2.1 |
| 2 | 08 「요구사항 정의서」 시트 안에 **같은 ID가 두 번**, 내용이 다름 (예: TRIP-002 = 장소 추가 / 여행 날짜 표시, TRIP-003 = 일정 저장 / 일정명 수정, AI-003·004) | 08 요구사항 시트 앞뒤 |
| 3 | 조건 수정 항목이 문서마다 다름 — API-PLAN-003: `companion, foodPreference, mealType, transportMode, 시간` / SCR-010: 동행 유형·이동 수단·관심분야·무료 여부 / Figma: 인원·이동 방법·관심사·AI 추천받기. **현재 구현은 Figma 화면을 기준으로 음식 종류·식사 시간대·이동 방법을 전송** | API 명세 ↔ 화면 목록 ↔ Figma |
| 4 | 장소 검색·추가 팝업(SCR-013·014)에 검색어가 있으나 API-PLACE-001에 검색어 파라미터 없음. **현재는 받은 후보 안에서 이름·주소로 거름** | 화면 목록 ↔ API 명세 |
| 5 | API-PLACE-002 숙박, STAY-001·002, AI-004 당일/숙박 판정이 남아 있으나 DDL v3.2.1은 숙박 제외(`place` 음식점만) | API·요구사항 ↔ DDL |
| 6 | API-PLAN-002 응답에 `selectedEvent`·조건·추천 이유가 없어 조건 수정 팝업 초기값·추천 이유를 표시할 수 없음 (3장 [제안]) | API-PLAN-002 ↔ SCR-009·010 |
| 7 | SCR-017 URL은 `/my-trips/{tripId}`, API는 `planId` — 같은 값인지 | 화면 목록 ↔ API 명세 (2팀) |
