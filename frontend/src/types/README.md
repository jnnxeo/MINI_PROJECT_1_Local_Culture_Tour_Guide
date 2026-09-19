# types — 타입 정의 (TypeScript 선택 시)

API 응답이나 데이터의 **모양을 미리 정의**해 두는 곳입니다.

> ⚠️ **JavaScript로 진행하기로 했다면 이 폴더는 사용하지 않습니다.** 폴더째 삭제해도 됩니다.

## 왜 쓰나요?

```ts
// 타입이 있으면 오타를 에디터가 미리 잡아줍니다
plan.titel   // ❌ 빨간 줄로 경고
plan.title   // ✅
```

## 예시

```ts
// api.ts
export interface Event {
  contentId: string;
  title: string;
  startDate: string;   // "2026-09-20"
  endDate: string;
  addr: string;
  mapx: number;
  mapy: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string | null;
}
```

> 백엔드 응답 형식과 **반드시 일치**해야 합니다. → [`docs/05_API_명세_작성규칙.md`](../../../docs/05_API_명세_작성규칙.md)
