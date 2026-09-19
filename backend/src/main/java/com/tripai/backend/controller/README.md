# controller — 요청을 받는 입구

프론트엔드가 보낸 HTTP 요청을 **가장 먼저 받는** 곳입니다. URL과 메서드를 연결합니다.

## 하는 일

- 어떤 URL로 들어온 요청인지 매칭 (`@GetMapping`, `@PostMapping`)
- 요청 값이 올바른지 **1차 검증** (`@Valid`)
- Service를 호출하고, 결과를 JSON으로 응답

## 하지 말아야 할 일

🚫 복잡한 로직 작성 — **Service로 보내세요**
🚫 SQL 직접 작성 — Repository의 일입니다

## 파일명 규칙

```
{기능}Controller.java
```

| 예시 | 담당 URL |
|---|---|
| `AuthController.java` | `/api/auth/**` (회원가입·로그인) |
| `EventController.java` | `/api/events/**` (문화행사 검색) |
| `PlanController.java` | `/api/plans/**` (일정 생성·저장) |
| `FavoriteController.java` | `/api/favorites/**` (즐겨찾기) |

> API 주소·응답 형식 규칙은 [`docs/05_API_명세_작성규칙.md`](../../../../../../../../docs/05_API_명세_작성규칙.md) 참고
