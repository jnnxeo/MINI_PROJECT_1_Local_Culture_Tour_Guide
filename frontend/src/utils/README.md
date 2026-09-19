# utils — 도우미 함수

**화면과 상관없는 단순 계산·변환 함수**를 둡니다.

| 파일 예시 | 하는 일 |
|---|---|
| `formatDate.js` | `2026-09-20` → `9월 20일 (금)` |
| `calcDistance.js` | 두 좌표 사이 직선거리 계산 |
| `storage.js` | localStorage 저장·조회 |

## 예시

```js
export function formatDate(dateStr) {
  const date = new Date(dateStr);
  const days = ['일','월','화','수','목','금','토'];
  return `${date.getMonth() + 1}월 ${date.getDate()}일 (${days[date.getDay()]})`;
}
```

> ⚠️ 좌표로 계산한 거리는 **직선거리**입니다.
> 실제 도로 이동시간처럼 표시하면 안 됩니다. (요구사항 PLAN-02)
