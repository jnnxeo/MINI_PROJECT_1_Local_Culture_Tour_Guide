# src — 프론트엔드 소스 루트

React 코드가 전부 이 아래에 들어갑니다.

| 폴더 | 역할 | 예시 파일 |
|---|---|---|
| `pages/` | 화면 단위 | `LoginPage`, `MainPage`, `MyPage` |
| `components/` | 재사용 UI 조각 | `EventCard`, `Button`, `Modal` |
| `services/` | 백엔드 API 호출 | `authApi`, `planApi` |
| `hooks/` | 재사용 로직 | `useAuth`, `useDebounce` |
| `contexts/` | 전역 상태 | `AuthContext` |
| `types/` | 타입 정의 (TS 선택 시) | `api.ts` |
| `styles/` | 공통 CSS | `globals.css` |
| `utils/` | 도우미 함수 | `formatDate` |
| `assets/` | 이미지·폰트 | `logo.png` |

## 파일이 어디 들어가야 할지 헷갈릴 때

```
이 코드가 화면 하나를 통째로 그리나?      → pages/
여러 화면에서 반복해서 쓰나?              → components/
백엔드를 호출하나?                        → services/
화면 없이 로직만 있나? (use~ 로 시작)     → hooks/
여러 화면이 같은 값을 공유해야 하나?      → contexts/
날짜 변환 같은 단순 계산인가?             → utils/
```
