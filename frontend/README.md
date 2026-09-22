# Frontend (React 18 · Vite 5 · JavaScript · Node.js)

사용자가 직접 보는 화면 디렉토리입니다. 백엔드 API를 호출해 받은 데이터를 화면에 그립니다.

> 📌 **현재 상태**: JavaScript 기반 React/Vite 초기 환경이 구성되어 있습니다.
> 팀원은 프로젝트를 다시 생성하지 말고 아래 설치·실행 절차를 따르세요.

---

## 폴더 구조

```
frontend/
├── README.md              ← 지금 이 파일
├── public/                ← 가공 없이 그대로 복사되는 파일 (파비콘 등)
└── src/
    ├── assets/            ← 이미지·폰트
    ├── components/        ← 재사용 UI 조각 (버튼·카드·모달)
    ├── pages/             ← 화면 단위 (로그인·메인·마이페이지)
    ├── services/          ← 백엔드 API 호출 코드
    ├── hooks/             ← 재사용 로직 (커스텀 훅)
    ├── contexts/          ← 전역 상태 (로그인 정보 등)
    ├── types/             ← 타입 정의 (TypeScript 선택 시)
    ├── styles/            ← 공통 CSS
    └── utils/             ← 날짜 포맷 등 도우미 함수
```

### components 와 pages 의 차이

| | components | pages |
|---|---|---|
| 무엇 | **조각** (버튼, 행사 카드, 모달) | **화면 전체** (로그인 화면, 메인 화면) |
| 재사용 | 여러 곳에서 씀 | 보통 1개 URL에 1개 |
| 예시 | `EventCard.jsx`, `Button.jsx` | `LoginPage.jsx`, `MainPage.jsx` |

---

## 프로젝트 초기 설정 및 실행

### 1단계 · 언어 선택 (결정 완료: JavaScript)

| | TypeScript | JavaScript |
|---|---|---|
| 장점 | 오타·타입 실수를 **미리** 잡아줌 | 문법이 단순해 **빨리 시작** 가능 |
| 단점 | 처음엔 어려움 | 실행 중 에러를 만날 확률이 높음 |
| 이번 프로젝트 | 사용하지 않음 | **사용** |

> 팀원이 JavaScript와 npm을 사용하므로 `.jsx` 파일을 작성합니다.

### 2단계 · Vite 프로젝트 (완료)

```bash
cd frontend
npm ci
```

> `npm create vite@latest . -- --template react`는 실행하지 않았습니다. 기존 README를 보존하면서 React/Vite 초기 파일을 추가했습니다. 팀원은 `npm ci`로 잠금 파일에 기록된 버전을 설치합니다.

### 3단계 · 라이브러리 설치 (완료)

`package.json`에 React, Vite, axios(API 호출), react-router-dom(화면 이동)을 등록했고 `npm install`로 `package-lock.json`을 생성했습니다. 새 라이브러리를 추가할 때는 `npm install <패키지>`를 실행합니다.

### 4단계 · 실행 확인

```bash
npm run dev
```

→ http://localhost:5173 에서 화면이 뜨면 성공

### 5단계 · 커밋

```bash
git checkout -b chore/frontend-init
git add frontend/
git commit -m "chore: React(Vite) 프로젝트 초기 설정"
git push origin chore/frontend-init
```

---

## 자주 쓰는 명령

| 명령 | 설명 |
|---|---|
| `npm ci` | `package-lock.json`에 고정된 라이브러리 설치 → `node_modules/` 생성 |
| `npm install <패키지>` | 의존성 추가 시 `package.json`과 `package-lock.json` 수정 |
| `npm run dev` | 개발 서버 실행 (http://localhost:5173) |
| `npm run build` | 배포용 빌드 → `dist/` 생성 |
| `npm run preview` | 빌드 결과 미리보기 |

> 🔄 **새로 clone 받았거나 팀원이 라이브러리를 추가했다면** `npm ci`를 실행하세요. 의존성을 추가한 사람은 두 package 파일을 함께 커밋합니다.

---

## 백엔드 연결 (CORS)

프론트(5173)와 백엔드(8080)는 **포트가 달라서** 그냥 호출하면 브라우저가 막습니다(CORS 에러).
해결 방법은 두 가지입니다.

**① 백엔드에서 CORS 허용** (권장) — 백엔드 담당자가 `global/config/`에 설정 추가

**② Vite 프록시 설정** — `vite.config.js`에 아래 추가

```js
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
```

---

## ⚠️ 커밋 주의사항

| 커밋 **O** | 커밋 **X** |
|---|---|
| `package.json` | `node_modules/` (용량 수백 MB) |
| `package-lock.json` | `dist/` (빌드 결과물) |
| `index.html`, `.env.example` | `.env` 🔐 (API 키) |
| `src/` 아래 소스 코드 | `.vite/` (캐시) |

> 🔐 카카오맵 키 등은 `.env` 파일에 넣고, **절대 커밋하지 않습니다.**
> Vite에서는 `VITE_` 접두사가 붙은 변수만 코드에서 읽을 수 있습니다.
>
> ```
> VITE_API_BASE_URL=http://localhost:8080
> VITE_KAKAO_MAP_KEY=여기에_발급받은_키
> ```

---

## 만들어야 할 화면

| 화면 | URL | 권한 | 관련 문서 |
|---|---|---|---|
| 로그인 | `/login` | 비회원 | SCR-001 |
| 회원가입 | `/signup` | 비회원 | SCR-002 |
| 메인 (조건 입력 + AI 추천 결과 + 지도) | `/` | 회원 | SCR-003 |
| 마이페이지 (저장 일정·즐겨찾기) | `/mypage` | 회원 | SCR-004 |

> 화면 상세는 [`../docs/04_프로젝트_요구사항_요약.md`](../docs/04_프로젝트_요구사항_요약.md) 및 Figma 시안 참고
