# 07. 나의 일정 API 계약

> 1팀(나의 일정)과 2팀(내 여행·행사 상세)이 **같은 일정 테이블**을 쓰기 때문에, 구현 전에 주고받을 데이터 모양을 먼저 확정합니다.
> 프론트는 이 문서의 JSON으로 mock을 만들어 화면을 먼저 개발하고, 백엔드는 같은 모양으로 API를 구현합니다.
>
> - 기준 스키마: [`database/schema/01_tripai_ddl_v3.2.1.sql`](../database/schema/01_tripai_ddl_v3.2.1.sql)
> - 기준 요구사항: `TripAI_요구사항명세서_수정본.xlsx`
> - 작성: 홍준서 · 상태: **초안 (점검 회의에서 확정)** · 이슈 #20

---

## 0. 범위

| 포함 | 제외 (DDL v3.2.1: 당일여행 · 숙박 제외) |
|---|---|
| AI-001·002·006·007·008·009·010, TRIP-001~011, FOOD-001·002, MAP-001, UX-004·005, SEC-003·004, EVENT-003 | STAY-001·002 (숙박), AI-004 (당일/숙박 판정 → 항상 당일) |

| 화면 | URL | 사용하는 API |
|---|---|---|
| SCR-009 나의 일정(저장 전 초안) | `/trips/draft` | PLAN-002 → 편집 → PLAN-004 |
| SCR-017 저장 일정 상세·편집 | `/my-trips/{planId}` | PLAN-003 → 편집 → PLAN-004 |
| 팝업: 생성 중 / 조건 수정 / 다시 추천 | (위 화면 위) | PLAN-001, PLAN-005 |
| 팝업: 시간 변경 / 장소 변경·추가 / 삭제 | (위 화면 위) | 프론트 상태만 변경 → 저장 시 PLAN-004, 후보는 PLACE-001 |

---

## 1. 핵심 설계 결정

1. **초안도 DB에 저장한다.** 초안 생성 시 `trip_plan.save_yn = FALSE`로 행을 만들고, 사용자가 "일정 저장"을 누르면 `TRUE`가 된다.
   → 로그인 이동·새로고침 후에도 서버에서 초안을 복원할 수 있다 (UX-005, SEC-003).
2. **편집은 프론트 상태에서만 하고, 저장 버튼에서 한 번에 보낸다.** 시간 변경·장소 변경·추가·삭제·제목 수정은 화면 상태만 바꾸고 `PUT /api/plans/{planId}`로 전체를 저장한다.
   → TRIP-011(명시적 저장)과 UX-004(미저장 이탈 방지)가 자연스럽게 성립한다.
3. **저장은 항목 전체 교체.** `trip_item(trip_plan_id, seq_order)`가 UNIQUE라서 순서를 UPDATE로 바꾸면 중복 키 오류가 난다(로컬 검증 완료). 저장 시 *기존 항목 삭제 → 새 순서로 재삽입*을 **한 트랜잭션**에서 처리한다.
4. **서버가 검증의 기준이다.** 프론트도 같은 규칙으로 버튼을 막지만, 최종 판정은 서버가 한다 (SEC-005).
5. **AI 없이도 동작한다.** 코스 구성은 규칙 기반으로 먼저 만들고, AI는 제목·추천 이유를 다듬는 역할로 붙인다. AI가 실패해도 규칙 기반 결과로 초안을 만든다 (FN-07).

---

## 2. 공통

- 모든 API는 **로그인 필요** (`Authorization: Bearer {accessToken}`) — `trip_plan.user_id`가 NOT NULL이므로 비회원 초안은 만들 수 없다.
  비회원이 AI 추천을 누르면 프론트가 조건을 `sessionStorage`에 보관 → 로그인 → 복귀 후 PLAN-001 호출 (UX-005).
- 응답은 기존 공통 형식 `{ success, data, message }` (`ApiResponse`).
- 날짜 `YYYY-MM-DD`, 시각 `HH:mm`.
- 좌표: DB `mapy` → JSON `lat`(위도), DB `mapx` → JSON `lng`(경도).
- **남의 일정**: 403 `FORBIDDEN` (SEC-004, 상세 정보는 응답에 넣지 않음).

---

## 3. 응답 모델 — `PlanDetail`

**PlanDetail = 일정 하나를 통째로 열었을 때 서버가 돌려주는 데이터 모양**입니다.
일정 정보(제목·날짜·인원 등) + 방문 항목 목록(`items`) + 안내 사항(`warnings`)으로 이루어집니다.

| 어디서 쓰나 | API |
|---|---|
| 일정 상세 화면 열기 (`/trips/draft`, `/my-trips/{planId}`) | PLAN-002, PLAN-003 |
| 저장 후 결과 | PLAN-004 |
| 초안 생성 · 다시 추천 결과 | PLAN-001, PLAN-005 |
| 행사 상세 "저장한 일정에 추가"(EVENT-004) — 불러와서 행사 넣고 저장 | PLAN-003 → PLAN-004 |

> 내 여행 **목록**(PLAN-010)은 PlanDetail이 아니라 카드용 **요약 모양(`PlanSummary`)** 을 씁니다 → 4장 PLAN-010 참고.

초안·저장 일정 모두 같은 모양입니다.

```json
{
  "planId": 12,
  "title": "고궁의 밤을 기다리는 하루",
  "tripDate": "2026-09-20",
  "visitStartTime": "11:00",
  "visitEndTime": "21:00",
  "headcount": 2,
  "transportMode": "WALK_TRANSIT",
  "interests": ["전통문화", "전시"],
  "anchorEventId": "EV-2026-0918-001",
  "saved": false,
  "aiGenerated": true,
  "dDay": null,
  "items": [
    {
      "itemId": 101,
      "seq": 1,
      "type": "PLACE",
      "contentId": "KK-1234567",
      "name": "행사장 근처 한식당",
      "category": "음식점",
      "address": "서울 종로구 사직로 ...",
      "lat": 37.5759,
      "lng": 126.9768,
      "imageUrl": null,
      "startTime": "12:30",
      "durationMin": 60,
      "endTime": "13:30",
      "timeFixed": false,
      "aiReason": "행사장에서 약 450m · 점심 시간대 영업",
      "openTime": "11:00",
      "closeTime": "21:00"
    },
    {
      "itemId": 103,
      "seq": 3,
      "type": "EVENT",
      "contentId": "EV-2026-0918-001",
      "name": "고궁의 밤, 달빛 산책",
      "category": "전통문화",
      "address": "서울 종로구 사직로 161",
      "lat": 37.5796,
      "lng": 126.9770,
      "imageUrl": "https://...",
      "startTime": "18:00",
      "durationMin": 180,
      "endTime": "21:00",
      "timeFixed": true,
      "aiReason": "검색 조건 '고궁'과 9월 관심분야 '전통문화'에 일치",
      "openTime": null,
      "closeTime": null
    }
  ],
  "warnings": [
    { "itemSeq": 1, "code": "HOURS_UNKNOWN", "message": "영업시간 정보가 없어 방문 전 확인이 필요합니다." }
  ]
}
```

| 필드 | 설명 |
|---|---|
| `saved` | `trip_plan.save_yn` — 초안이면 `false` |
| `dDay` | 저장 일정에서만 값 (`0` = 당일, 음수 = 지난 여행). 초안은 `null` (MY-003) |
| `items[].seq` | 1부터 연속, 시간순 |
| `items[].endTime` | `startTime + durationMin` (저장하지 않는 계산값) |
| `items[].timeFixed` | 행사 시작 시각이 정해진 EVENT 항목은 `true` → 시간 변경 불가 |
| `items[].aiReason` | 최대 100자. 근거가 부족하면 `null` (확인되지 않은 이유를 만들지 않음, AI-007) |
| `warnings` | 저장을 막지는 않지만 알려야 하는 사항 (영업시간 미확인·영업시간 밖 방문 등) |

`transportMode` 값: `WALK_TRANSIT`(도보+대중교통) · `WALK`(도보 위주)

---

## 4. 엔드포인트

| ID | 메서드 | URL | 설명 | 담당(제안) |
|---|---|---|---|---|
| API-PLAN-001 | POST | `/api/plans/drafts` | 조건으로 일정 초안 생성 | 1팀 |
| API-PLAN-002 | GET | `/api/plans/drafts/latest` | 내 최근 초안 복원 | 1팀 |
| API-PLAN-003 | GET | `/api/plans/{planId}` | 일정 상세 (초안·저장 공통) | 1팀 |
| API-PLAN-004 | PUT | `/api/plans/{planId}` | 일정 저장 / 수정 저장 | 1팀 |
| API-PLAN-005 | POST | `/api/plans/{planId}/regenerate` | 다시 추천 · 조건 수정 | 1팀 |
| API-PLACE-001 | GET | `/api/places` | 맛집 후보 조회 (장소 변경·추가) | 1팀 |
| API-PLAN-010 | GET | `/api/plans` | 내 여행 저장 일정 목록 | **2팀 (확정)** |
| API-PLAN-011 | DELETE | `/api/plans/{planId}` | 저장 일정 삭제 | **2팀 (확정)** |

> 행사 상세의 "저장한 일정에 추가"(EVENT-004)는 PLAN-003으로 불러와 행사를 넣은 뒤 PLAN-004로 저장하면 된다.
> 일정에 **문화행사를 추가**할 때의 후보는 2팀 행사 검색 API를 그대로 사용한다.

---

### API-PLAN-001 · 초안 생성

`POST /api/plans/drafts` → `201 Created`, `data: PlanDetail`

진입 경로 두 가지:
- **메인 + "AI 추천받기" 체크 후 검색** (MAIN-002): `anchorEventId` 없이 조건만 보냄 → 서버가 행사 1개 선정 (AI-002)
- **행사 상세 → "이 행사로 일정 만들기"** (EVENT-003): `anchorEventId` 지정

```json
{
  "anchorEventId": null,
  "keyword": "고궁",
  "month": 9,
  "interests": ["전통문화"],
  "tripDate": "2026-09-20",
  "visitStartTime": "11:00",
  "visitEndTime": "21:00",
  "headcount": 2,
  "transportMode": "WALK_TRANSIT",
  "useAi": true
}
```

| 필드 | 필수 | 규칙 |
|---|---|---|
| `tripDate` | ⭕ | 오늘 이후. 기준 행사 기간 안이어야 함 |
| `headcount` | ⭕ | 1 이상 |
| `anchorEventId` | | 있으면 그 행사 기준, 없으면 `keyword`·`month`·`interests`로 선정 |
| `visitStartTime` / `visitEndTime` | | 없으면 기본 `10:00`~`21:00`. 시작 < 종료 |
| `useAi` | | 기본 `true`. `false`면 규칙 기반만 (ai_yn = FALSE) |

처리 순서:
1. 기준 행사 1개 결정 — 표출 중(`display_yn`)이고 `tripDate`가 행사 기간 안이며 좌표가 있는 행사
2. 행사가 시작 시각을 가지면 EVENT 항목을 그 시각에 고정 (`time_fix_yn = TRUE`)
3. 행사 시간과 겹치지 않는 식사 시간대(점심 11:30~13:30 / 저녁 17:00~19:00)에 행사장 주변 음식점 배치 — 영업시간이 확인되고 그 시간에 영업하는 곳만, 가까운 순 (FOOD-002)
4. 추천 이유 작성 — 규칙 문장 생성 후 `useAi`면 AI로 다듬음(100자 이내). AI 실패 시 규칙 문장 유지
5. 제목 — AI 생성, 실패 시 `"MM.dd 행사명"` 기본 제목 (AI-008)
6. **이 사용자의 기존 미저장 초안은 삭제**하고 새 초안 저장 (한 사람당 초안 1개 — 5장 미결정 1번)

| 상황 | 코드 | ErrorCode |
|---|---|---|
| 조건에 맞는 행사 없음 | 404 | `NO_MATCHING_EVENT` |
| `anchorEventId` 행사 없음 | 404 | `EVENT_NOT_FOUND` |
| 여행일이 행사 기간 밖 / 종료된 행사 | 400 | `EVENT_NOT_AVAILABLE_ON_DATE` |
| 같은 사용자가 생성 요청 처리 중 | 409 | `PLAN_GENERATING` |
| 필수값 누락·형식 오류 | 400 | `INVALID_INPUT` |

---

### API-PLAN-002 · 최근 초안 복원

`GET /api/plans/drafts/latest` → `200`, `data: PlanDetail`

- `/trips/draft` 진입 시 호출. 로그인 후 복귀했을 때도 같은 초안이 보인다 (UX-005, SEC-003, TRIP-010 예외).
- 초안이 없으면 `404 PLAN_NOT_FOUND` → 프론트는 "조건을 입력해 일정을 만들어 보세요" 빈 상태.

---

### API-PLAN-003 · 일정 상세

`GET /api/plans/{planId}` → `200`, `data: PlanDetail`

| 상황 | 코드 | ErrorCode |
|---|---|---|
| 없는 일정 | 404 | `PLAN_NOT_FOUND` |
| 남의 일정 | 403 | `FORBIDDEN` |

---

### API-PLAN-004 · 저장 / 수정 저장

`PUT /api/plans/{planId}` → `200`, `data: PlanDetail`

- 초안(`saved=false`)에 호출하면 **최초 저장** (TRIP-010) → `save_yn = TRUE`
- 저장 일정에 호출하면 **수정 저장** (TRIP-011)
- 항목은 **화면에 보이는 순서대로 전체**를 보낸다. 서버는 기존 항목을 모두 지우고 다시 넣는다.

```json
{
  "title": "고궁의 밤을 기다리는 하루",
  "tripDate": "2026-09-20",
  "anchorEventId": "EV-2026-0918-001",
  "items": [
    { "type": "PLACE", "contentId": "KK-1234567", "startTime": "12:30", "durationMin": 60, "aiReason": "행사장에서 약 450m · 점심 시간대 영업" },
    { "type": "PLACE", "contentId": "KK-7654321", "startTime": "14:00", "durationMin": 60, "aiReason": null },
    { "type": "EVENT", "contentId": "EV-2026-0918-001", "startTime": "18:00", "durationMin": 180, "aiReason": "…" }
  ]
}
```

**검증 규칙** (하나라도 어기면 저장 안 됨)

| # | 규칙 | 근거 | 위반 시 |
|---|---|---|---|
| V1 | 제목 공백 제외 1~100자 | TRIP-003 | 400 `INVALID_INPUT` |
| V2 | 항목 1개 이상 | TRIP-001 | 400 `INVALID_INPUT` |
| V3 | `anchorEventId`는 items 안의 EVENT 항목이어야 함 (기준 행사 삭제 시 다른 행사를 기준으로 지정) | TRIP-007 | 400 `PLAN_ANCHOR_REQUIRED` |
| V4 | EVENT 항목 최대 2개 | TRIP-006, EVENT-004 | 400 `PLAN_EVENT_LIMIT` |
| V5 | 같은 행사·같은 장소 중복 불가 | TRIP-006 | 409 `PLAN_ITEM_DUPLICATED` |
| V6 | EVENT는 존재·표출 중이고 `tripDate`가 행사 기간 안 | EVENT-003 | 400 `EVENT_NOT_AVAILABLE_ON_DATE` |
| V7 | PLACE는 `place`에 존재·표출 중 (= 음식점, 관광지 불가) | AI-006, TRIP-005 | 404 `PLACE_NOT_FOUND` |
| V8 | 시간 고정 행사는 행사 시작 시각을 바꿀 수 없음 | TRIP-004 | 400 `PLAN_TIME_CONFLICT` |
| V9 | 시작 시각 오름차순, 앞 항목 종료 ≤ 다음 항목 시작 (겹침 금지) | TRIP-004 | 400 `PLAN_TIME_CONFLICT` |
| V10 | 모든 항목이 `visitStartTime`~`visitEndTime` 안 (지정된 경우) | TRIP-004 | 400 `PLAN_TIME_CONFLICT` |
| V11 | `durationMin` > 0, `aiReason` 100자 이하 | DDL CHECK | 400 `INVALID_INPUT` |
| — | 음식점 영업시간 밖 방문 / 영업시간 미확인 | FOOD-002 | 저장은 되고 `warnings`로 안내 |

`PLAN_TIME_CONFLICT`의 `message`에는 어느 항목이 왜 충돌했는지 사용자 문장으로 넣는다.
예: `"18:00 고궁의 밤 행사와 겹칩니다. 방문 종료 시간을 앞당겨 주세요."`

---

### API-PLAN-005 · 다시 추천 / 조건 수정

`POST /api/plans/{planId}/regenerate` → `200`, `data: PlanDetail` (같은 `planId`에 항목만 교체)

**초안에서만** 사용 (AI-009·010의 화면 범위가 "저장 전 초안").

```json
{ "conditions": null }
```

- `conditions: null` → **다시 추천** (AI-010): 같은 조건, 현재와 다른 행사·장소 조합 우선
- `conditions: { …PLAN-001과 같은 필드… }` → **조건 수정** (AI-009)
- **실패하면 기존 초안은 그대로** 두고 에러만 돌려준다 (AI-009·010 예외)

| 상황 | 코드 | ErrorCode |
|---|---|---|
| 저장된 일정에 호출 | 409 | `PLAN_ALREADY_SAVED` |
| 조건이 현재와 같음 (조건 수정) | 409 | `PLAN_SAME_CONDITIONS` |
| 다른 조합 후보 없음 | 404 | `NO_MATCHING_EVENT` |
| 처리 중 중복 요청 | 409 | `PLAN_GENERATING` |

---

### API-PLACE-001 · 맛집 후보

`GET /api/places?eventId={eventContentId}&keyword=&time=12:30&durationMin=60&page=0&size=10` → `200`

장소 변경(TRIP-005)·장소 추가(TRIP-006) 팝업에서 사용. `place` 테이블(음식점만)에서 기준 행사 좌표로부터 가까운 순.

```json
{
  "items": [
    {
      "contentId": "KK-1234567",
      "name": "행사장 근처 한식당",
      "category": "음식점",
      "address": "서울 종로구 …",
      "lat": 37.5759,
      "lng": 126.9768,
      "imageUrl": null,
      "distanceM": 450,
      "openTime": "11:00",
      "closeTime": "21:00",
      "openAtRequestedTime": true
    }
  ],
  "page": 0,
  "hasNext": true
}
```

- `openAtRequestedTime`: `time`~`time+durationMin`에 영업 중이면 `true`, 영업시간 미확인이면 `null`
- 거리는 직선거리(하버사인) — 실제 경로·이동시간은 제공하지 않음 (통합 검토 결정)
- 결과 없음 = `items: []` (에러 아님). 외부 API·DB 오류는 500으로 구분 (UX-003)

---

### API-PLAN-010 / 011 · 내 여행 목록 · 삭제 (2팀 담당 확정)

2팀(내 여행 화면)이 구현합니다. 같은 테이블을 쓰므로 아래 모양으로 맞춥니다.

- 대상: **`save_yn = TRUE`인 일정만** (초안은 목록에 나오지 않음)
- 정렬: `trip_date` 오름차순 (다가오는 여행 먼저)
- 응답 항목은 카드용 요약(`PlanSummary`) — 상세(`PlanDetail`)가 아님
- 카드를 누르면 `/my-trips/{planId}`로 이동 → 상세·편집 화면(1팀)이 PLAN-003으로 불러옴

```json
// GET /api/plans
{
  "plans": [
    { "planId": 12, "title": "고궁의 밤을 기다리는 하루", "tripDate": "2026-09-20",
      "anchorEventName": "고궁의 밤, 달빛 산책", "imageUrl": "https://...", "itemCount": 3, "dDay": 0 }
  ]
}
```

`DELETE /api/plans/{planId}` → `200`. `trip_item`·`trip_plan_interest`는 FK `ON DELETE CASCADE`로 함께 삭제.
없는 일정 404 `PLAN_NOT_FOUND`, 남의 일정 403 `FORBIDDEN` (삭제된 행 수가 0이면 조용히 성공 처리하지 않음).

---

## 5. 추가할 ErrorCode (구현 PR에서 `ErrorCode.java`에 반영)

이미 있는 것: `PLAN_NOT_FOUND`(404), `FORBIDDEN`(403), `EVENT_NOT_FOUND`(404), `INVALID_INPUT`(400)

| ErrorCode | HTTP | message |
|---|---|---|
| `NO_MATCHING_EVENT` | 404 | 조건에 맞는 문화행사를 찾지 못했어요. 날짜나 관심분야를 바꿔 보세요. |
| `EVENT_NOT_AVAILABLE_ON_DATE` | 400 | 선택한 날짜에 진행하지 않는 행사입니다. |
| `PLACE_NOT_FOUND` | 404 | 존재하지 않는 장소입니다. |
| `PLAN_ANCHOR_REQUIRED` | 400 | 기준 문화행사가 필요합니다. 다시 추천받거나 다른 행사를 선택해 주세요. |
| `PLAN_EVENT_LIMIT` | 400 | 문화행사는 일정당 최대 2개까지 추가할 수 있습니다. |
| `PLAN_ITEM_DUPLICATED` | 409 | 이미 일정에 있는 장소입니다. |
| `PLAN_TIME_CONFLICT` | 400 | (상황별 문장 — 위 V8~V10 참고) |
| `PLAN_ALREADY_SAVED` | 409 | 저장한 일정은 다시 추천할 수 없습니다. |
| `PLAN_SAME_CONDITIONS` | 409 | 조건이 바뀌지 않아 기존 일정을 유지합니다. |
| `PLAN_GENERATING` | 409 | 일정을 만드는 중입니다. 잠시만 기다려 주세요. |

---

## 6. 미결정 사항 (점검 회의 26·27일)

| # | 결정할 것 | 제안 |
|---|---|---|
| 1 | 사용자당 초안 개수 | **1개** — 새 초안을 만들면 이전 미저장 초안 삭제. 쌓이는 초안 정리 문제가 사라짐 |
| 2 | ~~내 여행 목록·삭제(PLAN-010·011) 담당~~ | **확정: 2팀 구현** (강산님 확인). `PlanMapper`·`PlanController`는 공유 — 먼저 머지되는 쪽 위에 이어서 추가 |
| 3 | 맛집 후보 데이터 출처 | `place` 테이블(배치 적재) 우선. 비어 있으면 카카오 로컬 API로 채우는 배치는 누가? (DATA-002) |
| 4 | 남의 일정 응답 | 403 `FORBIDDEN` (ErrorCode 주석 기준). 존재 여부를 숨기려면 404로 통일 |
| 5 | AI 모델·API 키 | 결정 전까지 `useAi=false`(규칙 기반)로 개발 진행 |
| 6 | 식사 시간대·기본 방문시간·검색 반경 | 점심 11:30~13:30 / 저녁 17:00~19:00 / 10:00~21:00 / 반경 1.5km |
| 7 | 컬럼명 변경 예정 (`transport_md` 등) | API 필드명(`transportMode`)은 그대로 두고 Mapper만 수정 |

---

## 7. 구현 순서 (1팀)

| 단계 | 내용 | 브랜치 |
|---|---|---|
| 1 | Plan/Item 엔티티·Mapper, PLAN-003·004 (고정 데이터 초안으로 시작), ErrorCode 추가 | `feat/{이슈}-plan-crud` |
| 2 | 나의 일정 화면 골격 — 이 문서 JSON으로 mock, 타임라인 카드·제목 수정·저장 | `feat/{이슈}-plan-editor-ui` |
| 3 | 규칙 기반 초안 생성 PLAN-001·002 + PLACE-001 | `feat/{이슈}-plan-draft-rule` |
| 4 | 편집 팝업 (시간 변경·장소 변경/추가·삭제) + 이탈 방지 | `feat/{이슈}-plan-edit` |
| 5 | 카카오맵 마커·순서선·목록 연동 | `feat/{이슈}-plan-kakao-map` |
| 6 | AI 제목·추천 이유, PLAN-005 다시 추천·조건 수정, 생성 중 팝업 | `feat/{이슈}-plan-ai` |
