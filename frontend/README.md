# Frontend (React · Vite · Node.js)

사용자가 직접 보는 화면 디렉토리입니다. 백엔드 API를 호출해 받은 데이터를 화면에 그립니다.

> 📌 **현재 상태**: 폴더 구조와 설명 문서만 있고 **실제 코드는 없습니다.**
> 프론트엔드 담당자가 아래 "프로젝트 최초 생성"을 **한 번만** 수행하면 됩니다.

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

## 프로젝트 최초 생성 (담당자 1인만, 1회)

### 1단계 · 언어 선택 (팀 결정 필요)

| | TypeScript | JavaScript |
|---|---|---|
| 장점 | 오타·타입 실수를 **미리** 잡아줌 | 문법이 단순해 **빨리 시작** 가능 |
| 단점 | 처음엔 어려움 | 실행 중 에러를 만날 확률이 높음 |
| 참고 | 이전 프로젝트(ScholarBee)에서 사용 | - |

> 🙋 **아직 정해지지 않았습니다.** 팀에서 정한 뒤 아래 명령 중 하나를 고르세요.
> 정하고 나면 이 문서의 이 부분을 수정해 주세요.

### 2단계 · Vite 프로젝트 생성

```bash
cd frontend

# TypeScript 로 할 경우
npm create vite@latest . -- --template react-ts

# JavaScript 로 할 경우
npm create vite@latest . -- --template react
```

> `.` 은 "현재 폴더에 생성"이라는 뜻입니다.
> 기존 파일이 있다고 경고가 나오면 **기존 파일 유지(Ignore files and continue)** 를 선택하세요.
> ⚠️ 이미 있는 `README.md` 파일들을 덮어쓰지 않도록 주의하세요.

### 3단계 · 라이브러리 설치

```bash
npm install                 # 기본 설치
npm install axios           # API 호출용
npm install react-router-dom  # 화면 이동(라우팅)용
```

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
| `npm install` | `package.json`의 라이브러리 설치 → `node_modules/` 생성 |
| `npm run dev` | 개발 서버 실행 (http://localhost:5173) |
| `npm run build` | 배포용 빌드 → `dist/` 생성 |
| `npm run preview` | 빌드 결과 미리보기 |

> 🔄 **새로 clone 받았거나 팀원이 라이브러리를 추가했다면** `npm install`을 먼저 실행하세요.

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
| `vite.config.js`, `index.html` | `.env` 🔐 (API 키) |
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
