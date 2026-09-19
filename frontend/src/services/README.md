# services — 백엔드 API 호출

서버와 통신하는 코드를 **한곳에 모아둡니다.**

## 왜 모으나요?

컴포넌트 안에서 직접 `fetch`를 하면, 주소가 바뀔 때 수십 군데를 고쳐야 합니다.
여기에 모아두면 **한 파일만 고치면 됩니다.**

## 파일 예시

| 파일 | 담당 |
|---|---|
| `api.js` | 공통 설정 (기본 주소, 토큰 자동 첨부) |
| `authApi.js` | 회원가입·로그인 |
| `eventApi.js` | 문화행사 검색·상세 |
| `planApi.js` | 일정 생성·저장·수정 |
| `favoriteApi.js` | 즐겨찾기 |

## 기본 형태 (axios)

```js
// api.js — 공통 설정
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,  // .env 에서 읽음
});

// 요청마다 로그인 토큰 자동 첨부
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default api;
```

> ⚠️ **서버 주소를 코드에 직접 쓰지 마세요.** `.env` 파일의 `VITE_API_BASE_URL`을 사용합니다.
