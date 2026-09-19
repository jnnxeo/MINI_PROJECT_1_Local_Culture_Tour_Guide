# erd — 테이블 관계도

테이블 간 관계를 그림으로 정리한 파일을 둡니다.

| 넣을 것 | 예시 |
|---|---|
| ERD 이미지 | `erd_v1.png` |
| 원본 파일·링크 | `erd_link.md` |

## 추천 도구

- **ERDCloud** (무료, 한국어, 협업 가능) — https://www.erdcloud.com
- **dbdiagram.io** (코드로 그리기) — https://dbdiagram.io

## 우리 프로젝트 관계

```
member  1 ──── N  trip_plan  1 ──── N  plan_item  N ──── 1  place
```

- 회원 한 명이 여러 일정을 가질 수 있음
- 일정 하나에 여러 장소가 들어감
- 같은 장소가 여러 일정에 쓰일 수 있음

> 🔄 테이블 구조가 바뀌면 **ERD도 함께 업데이트**해 주세요.
