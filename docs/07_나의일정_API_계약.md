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
| API-PLAN-004 | POST | `/api/plans/drafts/{draftId}/regenerate` | 조건으로 다시 추천 | 다시 추천(SCR-011) |
| API-PLAN-005 | PATCH | `/api/plans/drafts/{draftId}/title` | 초안 제목 변경 | 일정 저장 시 반영 |
| API-PLAN-006 | PUT | `/api/plans/drafts/{draftId}/items` | 항목·시간·순서 일괄 수정 | 일정 저장 시 반영 |
| API-PLAN-007 | POST | `/api/plans/drafts/{draftId}/items` | 맛집 추가 | 일정 저장 시 반영 |
| API-PLAN-008 | DELETE | `/api/plans/drafts/{draftId}/items/{itemId}` | 항목 삭제 | 일정 저장 시 반영 |
| API-PLAN-009 | POST | `/api/plans` | 초안을 저장 일정으로 확정 | 일정 저장(TRIP-010) |
| API-PLACE-001 | GET | `/api/places/restaurants` | 행사 주변 맛집 후보 | 장소 변경·추가 팝업 |
| API-PLAN-010~013 | | `/api/plans`, `/api/plans/{planId}` | 저장 일정 목록·상세·수정·삭제 | 내 여행(2팀) 범위 — 사용 안 함 |

### 메인·행사 상세(2팀)에서 나의 일정으로 들어오는 방법
`frontend/src/services/planService.js`의 `recommendDraft()`를 부르고 `/trips/draft`로 이동하면 됩니다.

```js
import { recommendDraft } from '../services/planService.js'

const draft = await recommendDraft({
  eventId,                  // 선택한 행사 event_content_id (필수)
  visitDate: '2026-10-03',  // 방문 날짜 YYYY-MM-DD (필수, 오늘 이후·행사 기간 안)
  foodPreference: 'ALL',    // ALL·KOREAN·CHINESE·JAPANESE·WESTERN (선택, 두 끼 공통)
  mealType: 'BOTH',         // BOTH·LUNCH·DINNER (선택)
  lunchFoodPreference: 'KOREAN',  // 점심 음식 종류 (선택, 없으면 foodPreference) [제안]
  dinnerFoodPreference: 'WESTERN', // 저녁 음식 종류 (선택, 없으면 foodPreference) [제안]
  includeCafe: true,        // 카페 포함 (선택, 기본 false) [제안]
  transportMode: 'WALK_TRANSIT', // 선택
})
navigate('/trips/draft', { state: { draftId: draft.draftId } })
```

- 오류는 `error.status`로 구분합니다: 지난 날짜·행사일 불일치 400, 없는 행사 404, 추천 후보 부족 422 (`error.message`에 안내 문구)
- `draftId`는 sessionStorage에도 저장되어 새로고침해도 같은 초안이 열립니다 (UX-005)
- 실제 API를 쓰려면 DB에 `database/schema/02`(#42)·`03`(#46) 마이그레이션과 음식점 시드(#42)가 적용되어 있어야 합니다
- 프론트 `.env`에 `VITE_USE_PLAN_MOCK_API=false`를 꼭 넣어야 합니다. 이 값과 `VITE_USE_MOCK_API`가 모두 없으면 코드는 목업으로 동작합니다

### 화면 편집 → API 반영 순서 [제안]
팝업에서 바꾼 내용은 화면에만 두고(UX-004 미저장 이탈 확인), **일정 저장**을 누르면 아래 순서로 보냅니다.

1. 삭제한 항목 → API-PLAN-008
2. 남은 항목의 시간·순서 → API-PLAN-006 (새 항목 추가 전에 맞춰야 옛 시간과 겹쳤다는 오류가 안 남)
   - 장소를 바꾼 항목도 같은 itemId 로 여기서 함께 바꿉니다 (삭제 + 추가를 하지 않아 요청 수와 중간 실패가 줄어듦)
3. 새로 추가한 맛집 → API-PLAN-007
4. 최종 순서가 다르면 → API-PLAN-006
5. 제목이 바뀌었으면 → API-PLAN-005
6. 확정 → API-PLAN-009

- 중간에 실패하면 앞 단계는 이미 서버에 반영됐을 수 있습니다. 화면의 편집 내용은 그대로 두고 서버 초안만 다시 받아,
  다시 저장하면 남은 부분만 이어서 반영됩니다 (이미 추가된 새 항목은 그 itemId 를 이어받아 중복 추가하지 않음)
- 화면에서 장소나 시간을 바꾼 맛집은 추천 이유를 숨기고, 서버도 저장할 때 해당 이유를 지웁니다 (이유에 식사 시각이 들어가 사실과 달라지기 때문, AI-004·007)

---

## 3. 명세에 모양이 정해지지 않은 부분 — [제안]

08 API 명세는 `items:[...]`, `recommendationReasons:[...]`, `mapPoints`의 내부 모양을 정하지 않았습니다.
목업은 명세의 다른 API에 이미 있는 필드 이름을 가져다 아래처럼 임시로 맞췄습니다.

| 항목 | [제안] 모양 | 가져온 근거 |
|---|---|---|
| `items[]` | `itemId, type(EVENT·PLACE), placeId, sequence, startTime, durationMin` | API-PLAN-006 요청 필드 |
| 〃 화면 표시용 | `name, addr, lat, lng, imageUrl, openTime, breakTime, closeTime, timeFixed` | API-PLACE-001 응답 필드 / TRIP-004 "행사 고정 시간" |
| 〃 음식 분류 | `cuisineType` — 맛집 `KOREAN·CHINESE·JAPANESE·WESTERN`, 카페 `CAFE`, 행사 `null` | 카페 카드를 "카페"로 표시하려고 추가 (조건 수정 팝업·메인 AI 추천 모달의 카페 포함과 함께) |
| `recommendationReasons[]` | `{ itemId, reason }` | AI-007 항목별 추천 이유 |
| `mapPoints[]` | `{ sequence, lat, lng }` | MAP-001·TRIP-008 방문 순서 마커 |
| API-PLAN-002 추가 응답 | `selectedEvent`(API-PLAN-001 응답), `conditions`(API-PLAN-003 응답), `recommendationReasons` | 조건 수정 팝업 초기값·추천 이유 표시에 필요 |
| API-PLACE-001 `distance` | 미터 단위 | 단위 미정 |
| 추천 조건 `foodPreference` / `mealType` | `ALL·KOREAN·CHINESE·JAPANESE·WESTERN` / `BOTH·LUNCH·DINNER` | 음식 종류·식사 시간대는 Figma 조건 팝업에 추가하는 [제안]. `mealType`은 항상 셋 중 하나를 명시적으로 보낸다 — 화면 기본값도 `BOTH`(점심+저녁 모두)라 조건 창을 열고 그대로 적용해도 기존 식사 범위가 좁아지지 않는다. `GET /api/places/restaurants`(API-PLACE-001, cuisineType/mealTime)는 이와 별개로 한 시점(`HH:mm`) 조회이며, `BOTH`·미지정은 시간 필터 없이 조회한다 |
| 끼니별 음식 종류 `lunchFoodPreference` / `dinnerFoodPreference`, 카페 `includeCafe` | 음식 종류 값은 `foodPreference`와 같음 / `true·false` | 메인 AI 추천 모달에서 점심·저녁 음식 종류를 따로 하나씩 고르고 카페 포함을 체크한다(09-28 강산님과 결정). 001·003 요청과 002 응답 `conditions`에 추가. 끼니별 값이 없으면 `foodPreference`(없으면 ALL)를 두 끼에 쓰고, 두 끼가 다르면 응답 `foodPreference`는 `null`. 003에서 `foodPreference`만 보내면 두 끼 모두 바꾸고, 끼니별 값을 보내면 그 끼니만 바꾼다. 카페는 음식 종류 값이 아니다(`CAFE`를 보내면 400) |
| 조건 수정 팝업 (SCR-010) | 점심·저녁 체크 카드 → 켠 끼니마다 음식 종류 칩 하나 → 카페 체크 카드. 인원은 −/+, 이동 방법은 세그먼트(드롭다운 없음) | 메인 AI 추천 모달과 같은 구조. 점심·저녁 모두 해제하면 적용할 수 없음. 저장된 조건이 그대로 채워짐. 장소 변경 팝업은 바꾸려는 항목 시각이 점심·저녁 시간대면 그 끼니 음식 종류로 검색 |
| `tripType`, `transportMode` 값 | `DAY_TRIP` / `WALK_TRANSIT`, `WALK`는 화면에서 쓰는 **예시** 값 | 명세에 값 목록이 없어 서버는 30자 이하 문자열을 받음 (허용 목록을 따로 만들지 않음) |

### DB 대응 (테이블정의서·DDL v3.2.1) [제안]
| API | DB |
|---|---|
| `draftId` | `trip_plan.trip_plan_id` (`save_yn = FALSE`) — API-PLAN-009 후 `save_yn = TRUE` |
| `visitDate` / 조건 `startTime`·`endTime`·`transportMode` | `trip_date` / `visit_start_time`·`visit_end_time`·`transport_md` |
| `items[].type`·`placeId`·`sequence` | `trip_item.item_type` · `event_content_id` 또는 `place_content_id` · `seq_order` |
| `items[].timeFixed` · 추천 이유 | `trip_item.time_fix_yn` · `trip_item.ai_reason` |
| `lat` / `lng` | `mapy`(위도) / `mapx`(경도) |
| (알려진 한계) 순번 임시 이동 | 수정·추가 때 `seq_order`를 +1000(추가는 2000+) 뒤로 미뤘다가 다시 매김 — 한 초안 항목이 1000개를 넘으면 충돌할 수 있으나 하루 일정에서는 생기지 않아 두었음 |
| 조건 `foodPreference`·`mealType` | `trip_plan.food_preference`·`meal_type` — DDL v3.2.1에 없어 `database/schema/03_trip_plan_food_condition_v3.2.3.sql`로 추가 (2팀장 확인 필요) |
| 조건 `lunchFoodPreference`·`dinnerFoodPreference`·`includeCafe` | `trip_plan.lunch_food_preference`·`dinner_food_preference`·`cafe_yn` — `database/schema/04_trip_plan_meal_food_cafe_v3.2.4.sql`로 추가 (강산님 확인 필요). 04 이전 초안은 끼니별 값 NULL(= `food_preference`), 카페 FALSE |
| 카페 | 새 테이블 없이 `place` 중 TourAPI cat3 `A05020900`(카페/전통찻집, 146곳 중 영업시간 있는 144곳). 이색음식점(`A05020700`)은 추천에서 제외. `cuisine_type`이 `OTHER`라 조회할 때 `CAFE`로 바꿔 쓴다 |
| API-PLAN-001 `headcount` | DDL `trip_plan.headcount`가 NOT NULL이라 선택값으로 받고, 없으면 1 |

### API-PLAN-001 초안 생성 규칙 (규칙 기반 — AI 결과를 쓰지 못할 때의 기본값) [제안]
명세 근거: AI-002(행사 1개), AI-004(추천 이유), AI-005(기본 제목), FOOD-002(식사 시간대·영업시간·거리), API-PLAN-001 오류 코드(400 행사일 불일치, 422 추천 후보 부족)

| 항목 | 정한 값 |
|---|---|
| 방문 시간 | 요청 `startTime`·`endTime`, 없으면 10:00~21:00 (행사 시간이 밖이면 행사 시간까지 넓힘) |
| 행사 시간 | 행사 시작 시간에 고정, 소요시간은 종료 시간까지 (시간 정보가 없으면 방문 시작 시각부터 120분, 고정 안 함) |
| 여행 날짜 | 오늘(Asia/Seoul, **오늘 포함**) 이후이고 행사 기간 안이어야 함 — 이미 끝난 행사도 여기서 400 |
| 식사 시간 | 점심 12:30 / 저녁 17:00, 60분. 기본 시각이 행사와 겹치거나 그 시각에 영업하는 맛집이 없으면 행사 직전 → 직후 시각으로 다시 찾음. 단 옮긴 시각도 그 끼니 시간대(점심 11:00~14:30, 저녁 17:00~20:30, AI 결과 검사와 같은 기준) 안일 때만 쓰고, 맞는 시각이 없으면 그 끼니는 빠짐. `mealType`이 없거나 `BOTH`면 두 끼를 모두 **시도**하고, 한 끼만 찾아도 초안을 만듦 (저장은 null — 003 요청에서 값을 안 보내면 '유지', 저장된 null은 '둘 다') |
| 맛집 고르기 | `foodPreference`(없으면 ALL = 한식·중식·일식·양식) 중 식사 60분 동안 영업하고 브레이크타임과 겹치지 않는 곳을 행사장에서 가까운 순. 자정을 넘는 영업·브레이크타임도 판단. 반경 1.5km → 3km → 5km(사각형은 SQL과 같은 구 기준 + 1% 여유), 20곳씩 끝까지 확인, 같은 맛집은 한 번만. 점심은 `lunchFoodPreference`, 저녁은 `dinnerFoodPreference`로 찾음 |
| 카페 (`includeCafe`) | 식사를 정한 뒤 남는 빈 시간에 카페 1곳을 60분. 시작 시각은 점심(11:00~14:30)·저녁(17:00~20:30) 시간대가 아니고 다른 일정과 겹치지 않아야 함. 다른 일정 바로 뒤·바로 앞 시각을 먼저, 그다음 30분 간격 시각을 다른 일정과 가까운 순으로 보고, 그 시간 동안 영업·브레이크 밖인 카페를 행사장에서 가까운 순으로 고름. 자리가 없으면 카페 없이 만듦(422 아님). 다시 추천은 지금 카페를 먼저 피함 |
| 추천 이유 | 행사: `선택한 행사 · M월 D일 진행 · HH:mm 시작` / 맛집: `점심 12:30 · 행사장에서 약 320m · 한식 · 영업 11:30~21:00` / 카페: `카페 16:00 · 행사장에서 약 480m · 영업 11:00~21:00` (확인된 값만) |
| 제목 | `M월 D일 {행사명}` (AI-005 실패 시 기본 제목 규칙) |
| 오류 | 지난 날짜·기간 밖 날짜·방문 시간 밖 행사 400, 없는 행사 404, 맛집 후보 없음·행사 좌표 없음 422 |
| 응답 | API-PLAN-002 초안 조회와 같은 모양 (명세 필드 `draftId, title, tripType, selectedEvent, items, recommendationReasons` 포함) |

### API-PLAN-003 조건 수정 · API-PLAN-004 다시 추천 규칙 [제안]
명세 근거: AI-009(조건 수정 후 전체 초안 다시 생성, 추천 실패 시 기존 초안 유지), AI-010(가능하면 기존과 다른 조합, 대체 후보가 없으면 기존 초안 유지), API-PLAN-003(400·404)·004(422)

| 항목 | 정한 값 |
|---|---|
| 화면 흐름 | 조건 수정: 003 → 002 / 다시 추천: 004 → 002 |
| 003 보내지 않은 값 | 지금 조건 그대로 (`companion`은 저장할 컬럼이 없어 받기만 함). `mealType: BOTH`는 점심·저녁 모두로 바꿈(저장은 null) |
| 003 여행 날짜 | 날짜를 바꿀 때도 오늘 이후·행사 기간 안이어야 함 (400). 단 화면(조건 수정 팝업)에서는 날짜를 바꾸지 않고, 다른 날짜는 메인 화면에서 새 여행으로 만든다 (09-28 팀 결정, API는 그대로 지원) |
| 003 처리 | 새 조건으로 일정을 만든 뒤 **조건과 일정 항목을 한 트랜잭션에서 함께** 바꿈 (AI-009 "조건 수정 후 전체 초안을 다시 생성"). 새 조건에서 가장 알맞은(가까운) 곳부터 고름. 실패하면 아무것도 바뀌지 않음 — 지난 날짜·기간 밖 날짜·방문 시간 밖 행사 400, 후보 부족 422 (명세 003에 422가 없어 001·004와 같은 코드로 둠, 팀 확인 필요) |
| 004 기준 행사 | 사용자가 고른 행사라 바꾸지 않음 (AI-010의 "다른 문화행사"는 1팀 흐름에서 제외) |
| 004 맛집 | 지금 조건 그대로, 식사마다 지금 맛집보다 한 단계 먼 곳 중 가장 가까운 곳 → 없으면 처음(가장 가까운 곳)부터 다시. 누를 때마다 A → B → C … 순서로 바뀜 (같은 거리의 다른 맛집은 건너뛸 수 있음 — 모든 조합을 도는 것은 아님) |
| 지금 맛집이 조건에 안 맞을 때 | 지금 맛집이 현재 조건(음식 종류·식사 시간대·영업시간)에 맞지 않으면 한 단계 먼 곳이 아니라 가장 가까운 곳부터 고름 |
| 004 결과가 지금과 같을 때 | 바꾸지 않고 422 (대체 후보 없음, 기존 초안 유지). 조건 변경은 003이 따로 처리하므로 이 검사는 같은 조건의 다시 추천에만 해당 |
| 생성 중 취소 | 이미 보낸 요청은 서버에서 끝까지 처리됨. 성공했다면 화면을 서버 결과로 맞추고 "이미 반영됨"을 알림, 실패했다면 서버 초안은 그대로. 요청이 끝나기 전에는 새 추천 요청을 받지 않음 |
| 004 제목 | 사용자가 바꿨을 수 있어 유지 |
| 004 항목 번호 | 항목을 새로 만들기 때문에 `itemId`가 바뀜 |

### AI 연동 (API-PLAN-001·003·004 공통) [제안]
명세 근거: AI-002(문화행사 1개 + 맛집 초안, 확인된 데이터 기반 추천 이유, AI 제목은 선택), AI-007(추천 이유), AI-008(AI 일정 제목), AI-009·010(실패 시 기존 초안 유지). 명세에는 외부 AI 서비스·모델·키 관리가 정해져 있지 않아 아래는 모두 [제안]입니다. (AI 요구사항 ID는 08 요구사항 정의서 시트의 두 번째 목록 기준 — 5장 2번 ID 중복 참고)

| 항목 | 정한 값 |
|---|---|
| 외부 AI (EXT-AI) | OpenAI Responses API `POST https://api.openai.com/v1/responses`, 모델 `gpt-4.1-mini`, 응답 형식 `json_object`, `store: false`, 제한 시간 20초 |
| 키 관리 | 각자 `backend/.env`의 `OPENAI_API_KEY`에만 넣음 (GitHub·채팅에 올리지 않음). `.env.example`에는 이름만 있음. 키가 비어 있으면 AI를 부르지 않고 규칙 기반으로만 동작 |
| 순서 | ① 규칙 기반 초안을 먼저 만듦 → ② 키가 있으면 선택 행사 1개 + 주변 맛집 후보로 AI에 일정을 요청 → ③ 서버 검사를 모두 통과하면 AI 결과 저장(`ai_yn = TRUE`), 아니면 ①을 저장(`ai_yn = FALSE`) |
| AI에 넘기는 후보 | 행사: 선택한 행사 1개(이름·분류·자치구·장소·기간·시간·일시 원문·무료 여부). 맛집: 영업시간을 아는 곳 중 음식 종류가 맞고 행사장에서 가까운 순 최대 15곳(반경 1.5km → 3km → 5km, 7곳 이상 모일 때까지). 점심·저녁 음식 종류가 다르면 종류마다 10곳. 카페 포함이면 카페 5곳(`cuisineType: CAFE`)을 더함. 다시 추천(004)은 지금 일정의 맛집·카페를 모두 뺌 |
| 서버 검사 (하나라도 어기면 규칙 기반) | 행사는 선택 행사 1개, 시작 시간이 있으면 그대로(종료는 행사 종료 이내) / 맛집은 후보 안에서만, 같은 곳 두 번 안 됨 / 점심 11:00~14:30·저녁 17:00~20:30 사이 시작, 끼니별 1번, 요청한 식사 시간대만 / 식사 시간 동안 영업하고 브레이크와 겹치지 않음 / 식사 30~120분, 행사 30~360분 / 방문 시간 안, 서로 안 겹침(항목 편집과 같은 규칙) / 규칙 기반보다 식사 수가 적지 않음 / 끼니마다 그 끼니 음식 종류와 같아야 함 / 카페는 요청했을 때만 1곳, 점심·저녁 시간대 밖에서 시작, 규칙 기반이 카페를 넣었으면 AI도 넣어야 함 / 다시 추천이면 지금 일정과 달라야 함 |
| 항목 종류 | AI가 `placeType` 철자를 틀리는 경우가 있어(실제 응답 `RESTARUANT`) 종류는 ID가 행사 후보·맛집 후보 중 어디에 있는지로 정함 |
| 추천 이유 | AI가 쓴 이유를 100자까지 저장 (`trip_item.ai_reason` VARCHAR(100)). 프롬프트에서 입력에 있는 정보만 근거로 80자 이내로 쓰게 함 |
| 제목 | 001에서만 AI 제목 사용(30자 이내 요청, 비어 있거나 100자 초과면 기본 제목). 003·004는 사용자가 바꿨을 수 있어 제목 유지 |
| 실패 처리 | 호출 실패(키 오류·시간 초과 등)·JSON 오류·검사 실패 모두 사용자에게는 오류 없이 규칙 기반 초안을 보여줌. 서버 로그에는 사유만 남김(HTTP 상태·오류 코드, 키·오류 메시지는 남기지 않음) |
| 실제 확인 (09-28, gpt-4.1-mini) | 001·003·004 18번 호출 중 14번 AI 결과 사용, 4번 규칙 기반 대체(저녁 21:00 시작, 행사 시간 변경, 식사 누락 등 — 모두 검사로 걸러짐). 응답 2~5초, 1회 약 4천 토큰 |

---

## 4. 화면·목업이 지키는 규칙 — [명세] 근거

| 규칙 | 근거 | 위반 시 (API 명세 오류 코드) |
|---|---|---|
| 제목 1~100자 | TRIP-003, DDL `title VARCHAR(100)` | 400 (API-PLAN-005) |
| 머무는 시간 1~1440분, 끝나는 시각이 그날 안 | TRIP-004, SEC-005 | 400 (006·007 요청 검증, 규칙 검사에서 한 번 더) |
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
| 3 | 조건 수정 항목이 문서마다 다름 — API-PLAN-003: `companion, foodPreference, transportMode, 시간` (+ `mealType`은 [제안] 필드) / SCR-010: 동행 유형·이동 수단·관심분야·무료 여부 / Figma: 인원·이동 방법·관심사·AI 추천받기. **현재 구현은 Figma 화면을 기준으로 음식 종류·식사 시간대·이동 방법을 전송** | API 명세 ↔ 화면 목록 ↔ Figma |
| 4 | 장소 검색·추가 팝업(SCR-013·014)에 검색어가 있으나 API-PLACE-001에 검색어 파라미터 없음. **현재는 받은 후보 안에서 이름·주소로 거름** | 화면 목록 ↔ API 명세 |
| 5 | API-PLACE-002 숙박, STAY-001·002, AI-004 당일/숙박 판정이 남아 있으나 DDL v3.2.1은 숙박 제외(`place` 음식점만) | API·요구사항 ↔ DDL |
| 6 | API-PLAN-002 응답에 `selectedEvent`·조건·추천 이유가 없어 조건 수정 팝업 초기값·추천 이유를 표시할 수 없음 (3장 [제안]) | API-PLAN-002 ↔ SCR-009·010 |
| 7 | SCR-017 URL은 `/my-trips/{tripId}`, API는 `planId` — 같은 값인지 | 화면 목록 ↔ API 명세 (2팀) |
