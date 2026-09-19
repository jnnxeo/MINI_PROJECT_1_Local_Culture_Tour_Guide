# dto — 화면과 주고받는 데이터 형태

DTO(Data Transfer Object) = **필요한 값만 담아서 전달하는 상자**입니다.

## 왜 필요한가요?

Entity를 그대로 주고받으면 문제가 생깁니다.

```
❌ Member 엔티티를 그대로 응답  →  password 해시까지 화면에 노출됨
✅ MemberResponse DTO 로 변환   →  nickname, email 만 전달
```

## 파일명 규칙

| 방향 | 접미사 | 예시 |
|---|---|---|
| 요청(받는 것) | `Request` | `LoginRequest.java`, `PlanCreateRequest.java` |
| 응답(주는 것) | `Response` | `LoginResponse.java`, `PlanDetailResponse.java` |

## 예시

```java
// 요청: 로그인할 때 받는 값
public record LoginRequest(String email, String password) {}

// 응답: 로그인 성공 시 돌려주는 값 (비밀번호는 절대 포함 X)
public record LoginResponse(String accessToken, String nickname) {}
```

> JSON 키 이름은 `camelCase`로 통일합니다. → [`docs/05_API_명세_작성규칙.md`](../../../../../../../../../docs/05_API_명세_작성규칙.md)
