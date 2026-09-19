# response — 공통 응답 형식

모든 API가 **같은 모양으로** 응답하도록 껍데기를 정의합니다.

## 왜 통일하나요?

프론트에서 응답마다 다른 구조를 처리하면 코드가 복잡해집니다. 형식이 같으면 한 번만 처리하면 됩니다.

## 형식

```json
{
  "success": true,
  "data": { "planId": 1 },
  "message": null
}
```

```json
{
  "success": false,
  "data": null,
  "message": "이미 사용 중인 이메일입니다."
}
```

## 파일 예시

| 파일 | 역할 |
|---|---|
| `ApiResponse.java` | 위 형식을 담는 공통 클래스 |

> `message`는 **사용자에게 그대로 보여줄 한국어 문장**으로 작성합니다.
