# 05. API 명세 작성 규칙

> API = **프론트엔드와 백엔드가 데이터를 주고받는 약속**입니다.
> 이 약속을 먼저 정해야 두 팀이 **동시에** 개발할 수 있습니다.
> (프론트는 아직 서버가 없어도 "이런 모양으로 올 것"을 알면 화면을 만들 수 있습니다)

---

## 1. 기본 규칙

### 주소(URL) 형식

```
/api/{자원}/{식별자}
```

- 모두 **소문자**, 단어 구분은 **하이픈(-)**
- 자원 이름은 **복수형** (`/api/plans`, `/api/places`)
- 동사는 URL이 아닌 **HTTP 메서드**로 표현

| 하고 싶은 일 | 메서드 | URL 예시 |
|---|---|---|
| 목록 조회 | `GET` | `/api/plans` |
| 단건 조회 | `GET` | `/api/plans/1` |
| 생성 | `POST` | `/api/plans` |
| 전체 수정 | `PUT` | `/api/plans/1` |
| 일부 수정 | `PATCH` | `/api/plans/1/title` |
| 삭제 | `DELETE` | `/api/plans/1` |

### ❌ 이렇게 쓰지 마세요

```
/api/getPlanList        ← 동사가 URL에 들어감
/api/Plan/Delete/1      ← 대문자 + 동사
/api/plan_list          ← 언더바
```

---

## 2. 응답 형식 통일

**모든 응답은 같은 껍데기**를 사용합니다. 프론트에서 처리하기 편해집니다.

### 성공

```json
{
  "success": true,
  "data": {
    "planId": 1,
    "title": "고궁의 밤을 기다리는 하루"
  },
  "message": null
}
```

### 실패

```json
{
  "success": false,
  "data": null,
  "message": "이미 사용 중인 이메일입니다."
}
```

> `message`는 **사용자에게 그대로 보여줄 수 있는 한국어 문장**으로 씁니다.
> (`NullPointerException` 같은 개발자용 메시지를 그대로 내보내면 안 됩니다)

---

## 3. HTTP 상태 코드

| 코드 | 의미 | 언제 |
|---|---|---|
| `200` | 성공 | 조회·수정·삭제 성공 |
| `201` | 생성됨 | 회원가입, 일정 저장 성공 |
| `400` | 잘못된 요청 | 필수값 누락, 종료일 < 시작일 |
| `401` | 인증 안 됨 | 로그인 필요 / 토큰 만료 |
| `403` | 권한 없음 | **남의 일정**을 수정하려 할 때 |
| `404` | 없음 | 존재하지 않는 일정 ID |
| `409` | 충돌 | 이메일 중복, 즐겨찾기 중복 등록 |
| `500` | 서버 오류 | 예상 못 한 에러 (외부 API 실패 포함) |

> 🔐 `401`과 `403`은 다릅니다.
> `401` = **당신이 누구인지 모르겠다**(로그인 안 함) / `403` = **누군지는 알지만 권한이 없다**

---

## 4. 명세 작성 양식

새 API를 만들기 전에 **엑셀 `API 명세` 시트**에 아래 형식으로 먼저 적습니다.

### 예시

| 항목 | 내용 |
|---|---|
| 기능명 | 로그인 |
| 요구사항 ID | AUTH-02 |
| 메서드 | `POST` |
| URL | `/api/auth/login` |
| 인증 필요 | ❌ |

**요청 (Request Body)**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "nickname": "준서"
  },
  "message": null
}
```

**에러 케이스**

| 상황 | 코드 | message |
|---|---|---|
| 이메일 형식 오류 | 400 | "올바른 이메일 형식을 입력해주세요." |
| 계정 없음/비밀번호 불일치 | 401 | "이메일 또는 비밀번호가 올바르지 않습니다." |

---

## 5. 이름 짓기 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| JSON 키 | `camelCase` | `startDate`, `planId`, `contentId` |
| DB 컬럼 | `snake_case` | `start_date`, `plan_id`, `content_id` |
| URL | `kebab-case` | `/api/trip-plans` |

> 💡 DB는 `snake_case`, API(JSON)는 `camelCase`입니다. 헷갈리지 마세요.
> Spring Boot에서 자동 변환 설정을 걸어두면 편합니다.

### 날짜·시간 형식

| 종류 | 형식 | 예시 |
|---|---|---|
| 날짜 | `YYYY-MM-DD` | `2026-09-20` |
| 날짜+시간 | `YYYY-MM-DDTHH:mm:ss` | `2026-09-20T18:00:00` |
| 시간만 | `HH:mm` | `18:00` |

---

## 6. 우리 프로젝트 주요 API (초안)

> 확정본은 엑셀 `API 명세` 시트를 기준으로 합니다.

| 기능 | 메서드 | URL | 인증 |
|---|---|---|---|
| 회원가입 | POST | `/api/auth/signup` | ❌ |
| 로그인 | POST | `/api/auth/login` | ❌ |
| 로그아웃 | POST | `/api/auth/logout` | ⭕ |
| 문화행사 검색 | GET | `/api/events?startDate=&endDate=&gu=&category=` | ❌ |
| 행사 상세 | GET | `/api/events/{contentId}` | ❌ |
| AI 일정 생성 | POST | `/api/plans/generate` | ⭕ |
| 일정 저장 | POST | `/api/plans` | ⭕ |
| 내 일정 목록 | GET | `/api/plans` | ⭕ |
| 일정 상세 | GET | `/api/plans/{planId}` | ⭕ |
| 일정 수정 | PATCH | `/api/plans/{planId}` | ⭕ |
| 일정 삭제 | DELETE | `/api/plans/{planId}` | ⭕ |
| 즐겨찾기 토글 | POST | `/api/favorites/{contentId}` | ⭕ |
| 즐겨찾기 목록 | GET | `/api/favorites` | ⭕ |

---

## 7. 개발 순서 권장

```
1. 엑셀 API 명세에 요청/응답 형태 먼저 확정
        ↓
2. 프론트는 임시 데이터(mock)로 화면 개발
   백엔드는 실제 API 개발
        ↓
3. 완성되면 연결해서 테스트
```

> 이렇게 하면 **백엔드를 기다리느라 프론트가 멈추는 일**이 없습니다.
