# MINI_PROJECT_1 · 지역 문화 행사 기반 투어 가이드 서비스

공공데이터와 AI를 활용해, **서울 문화행사를 기준으로 하루 여행 코스를 추천**하는 웹 서비스입니다.

- 선택 주제: 공공데이터와 AI를 활용한 지역 생활정보 추천 서비스
- 프로젝트 방향: 문화행사를 여행의 기준점으로 삼아 주변 맛집·관광지·숙박을 묶어 일정으로 제안
- 현재 단계: **개발 시작 전 초기 세팅 완료** (코드 없음, 폴더 구조와 문서만 존재)

---

## 목차

1. [이 저장소는 지금 어떤 상태인가요?](#이-저장소는-지금-어떤-상태인가요)
2. [기술 스택](#기술-스택)
3. [Gradle과 Node.js는 꼭 필요한가요?](#gradle과-nodejs는-꼭-필요한가요)
4. [전체 디렉토리 구조](#전체-디렉토리-구조)
5. [처음 참여하는 팀원이 해야 할 일](#처음-참여하는-팀원이-해야-할-일)
6. [절대 커밋하면 안 되는 것들](#절대-커밋하면-안-되는-것들)
7. [문서 목록](#문서-목록)

---

## 이 저장소는 지금 어떤 상태인가요?

**아직 코드가 한 줄도 없습니다.** 지금 있는 것은 두 가지뿐입니다.

1. **폴더 구조** — 앞으로 어떤 파일을 어디에 넣을지 미리 정해둔 빈 상자들
2. **설명 문서(.md 파일)** — 각 상자에 무엇을 넣어야 하는지 적어둔 안내문

모든 폴더 안에는 `README.md` 한 개씩만 들어 있습니다. 그 폴더가 **무슨 역할인지, 어떤 파일을 넣어야 하는지** 적혀 있으니, 작업을 시작하기 전에 해당 폴더의 `README.md`를 먼저 읽어 주세요.

> 💡 **왜 빈 폴더에 README.md가 들어있나요?**
> Git은 **완전히 빈 폴더는 저장하지 못합니다.** 파일이 최소 1개는 있어야 폴더가 유지됩니다.
> 그래서 각 폴더에 설명용 `README.md`를 넣어 폴더 구조를 유지하면서 동시에 설명서 역할도 하게 했습니다.

---

## 기술 스택

| 구분 | 기술 | 버전(권장) | 설명 |
|---|---|---|---|
| 언어 (백엔드) | Java | 17 | Spring Boot 3.x가 요구하는 최소 버전 |
| 프레임워크 | Spring Boot | 3.x | 백엔드 서버(REST API) |
| 빌드 도구 | **Gradle** | Wrapper 사용 | 백엔드 빌드·의존성 관리 |
| 언어 (프론트) | JavaScript / TypeScript | - | React 작성용 |
| 프레임워크 | React | 18.x | 화면(UI) |
| 개발 서버/번들러 | Vite | 5.x | React 개발 서버 + 빌드 |
| 실행 환경 | **Node.js** | 20 LTS | React 개발·빌드에 필요 |
| 데이터베이스 | MySQL / MariaDB | 8.x / 10.x | 회원·일정·장소 데이터 저장 |

> MySQL과 MariaDB는 **거의 호환**됩니다. 둘 중 하나로 팀 내에서 통일하는 것을 권장하며,
> 접속 설정만 바꾸면 되도록 코드에서는 표준 SQL 위주로 작성합니다.

---

## Gradle과 Node.js는 꼭 필요한가요?

### ✅ Gradle — 필요합니다 (단, 따로 설치하지 않아도 됩니다)

Gradle은 **백엔드(Java/Spring Boot)를 빌드하고 라이브러리를 자동으로 내려받아 주는 도구**입니다.
Spring Boot 프로젝트는 Gradle(또는 Maven) 없이는 실행할 수 없습니다. 우리는 **Gradle**을 씁니다.

**하지만 팀원 각자가 Gradle을 설치할 필요는 없습니다.** Spring Boot 프로젝트를 만들면
`gradlew`(맥/리눅스), `gradlew.bat`(윈도우)라는 **Gradle Wrapper** 파일이 함께 생성되는데,
이 파일이 "프로젝트에 맞는 Gradle 버전"을 알아서 내려받아 실행해 줍니다.

```bash
# Gradle을 설치하지 않아도 이렇게 실행됩니다
./gradlew bootRun      # 맥/리눅스
gradlew.bat bootRun    # 윈도우
```

> ⚠️ 그래서 `gradlew`, `gradlew.bat`, `gradle/wrapper/` 폴더는 **반드시 커밋해야 합니다.**
> 반대로 빌드 결과물인 `build/`, `.gradle/` 폴더는 **커밋하면 안 됩니다.** (`.gitignore`에 이미 설정됨)

### ✅ Node.js — 필요합니다 (각자 로컬에 설치)

Node.js는 **React를 개발·빌드할 때 쓰는 실행 환경**입니다.
React 코드는 브라우저가 바로 이해할 수 없어서, Vite가 변환해 주는데 그 Vite가 Node.js 위에서 돌아갑니다.

```bash
npm install      # package.json에 적힌 라이브러리를 node_modules/ 폴더로 내려받음
npm run dev      # 개발 서버 실행 (보통 http://localhost:5173)
npm run build    # 배포용 파일 생성 (dist/ 폴더)
```

> ⚠️ `node_modules/` 폴더는 **용량이 수백 MB**라 절대 커밋하지 않습니다. (`.gitignore`에 이미 설정됨)
> 대신 `package.json`과 `package-lock.json`을 커밋하면, 다른 팀원이 `npm install` 한 번으로 똑같이 맞출 수 있습니다.

### 정리

| 도구 | 설치 필요? | 커밋할 것 | 커밋하면 안 되는 것 |
|---|---|---|---|
| Gradle | ❌ (Wrapper가 대신함) | `gradlew`, `gradlew.bat`, `gradle/wrapper/`, `build.gradle` | `build/`, `.gradle/` |
| Node.js | ⭕ (직접 설치) | `package.json`, `package-lock.json` | `node_modules/`, `dist/` |

---

## 전체 디렉토리 구조

```
MINI_PROJECT_1_Local_Culture_Tour_Guide/
│
├── README.md                    ← 지금 읽고 있는 파일 (프로젝트 전체 안내)
├── .gitignore                   ← Git이 무시할 파일 목록 (빌드 결과물·비밀번호 등)
├── .gitattributes               ← 윈도우/맥 줄바꿈 차이로 인한 충돌 방지 설정
│
├── .github/                     ← GitHub 협업 도구 설정
│   ├── README.md
│   ├── PULL_REQUEST_TEMPLATE.md ← PR(코드 합치기 요청) 작성 양식
│   └── ISSUE_TEMPLATE/          ← 이슈(할 일·버그) 작성 양식
│       ├── bug_report.md
│       └── feature_request.md
│
├── docs/                        ← 📚 팀 문서 모음 (여기부터 읽으세요)
│   ├── README.md                ← 문서 목차
│   ├── 00_github_기초가이드.md    ← 깃허브가 처음이라면 이것부터!
│   ├── 01_브랜치전략.md           ← 어떤 브랜치를 만들고 언제 합치는지
│   ├── 02_커밋컨벤션.md           ← 커밋 메시지 작성 규칙
│   ├── 03_개발환경설정.md         ← JDK·Node.js·DB 설치 방법
│   ├── 04_프로젝트_요구사항_요약.md ← 우리가 만들 기능 정리
│   ├── 05_API_명세_작성규칙.md     ← 백엔드-프론트 간 데이터 주고받는 규칙
│   └── 06_자주묻는질문_트러블슈팅.md ← 에러가 났을 때 보는 문서
│
├── backend/                     ← ☕ 백엔드 (Java + Spring Boot + Gradle)
│   ├── README.md                ← 백엔드 실행 방법·패키지 설명
│   └── src/
│       ├── main/
│       │   ├── java/com/tripai/backend/
│       │   │   ├── README.md
│       │   │   ├── controller/  ← 프론트의 요청을 받는 입구 (URL 담당)
│       │   │   ├── service/     ← 실제 로직 처리 (핵심 두뇌)
│       │   │   ├── repository/  ← DB에 질의하는 부분
│       │   │   ├── domain/
│       │   │   │   ├── entity/  ← DB 테이블과 1:1로 대응되는 클래스
│       │   │   │   └── dto/     ← 화면과 주고받는 데이터 형태
│       │   │   ├── external/    ← 서울시 문화행사·TourAPI·카카오 등 외부 API 연동
│       │   │   └── global/
│       │   │       ├── config/    ← 설정(CORS·Security 등)
│       │   │       ├── exception/ ← 에러 처리
│       │   │       ├── jwt/       ← 로그인 토큰 처리
│       │   │       └── response/  ← 공통 응답 형식
│       │   └── resources/
│       │       ├── README.md      ← ⚠️ 비밀번호·API 키 관리 방법 (필독)
│       │       ├── static/        ← 이미지 등 정적 파일
│       │       └── templates/     ← (사용 시) 서버 렌더링 템플릿
│       └── test/java/com/tripai/backend/  ← 테스트 코드
│
├── frontend/                    ← ⚛️ 프론트엔드 (React + Vite + Node.js)
│   ├── README.md                ← 프론트 실행 방법·폴더 설명
│   ├── public/                  ← 그대로 복사되는 파일(파비콘 등)
│   └── src/
│       ├── README.md
│       ├── assets/              ← 이미지·폰트
│       ├── components/          ← 재사용 UI 조각 (버튼·카드 등)
│       ├── pages/               ← 화면 단위 (로그인·메인·마이페이지)
│       ├── services/            ← 백엔드 API 호출 코드
│       ├── hooks/               ← 재사용 로직 (커스텀 훅)
│       ├── contexts/            ← 전역 상태 (로그인 정보 등)
│       ├── types/               ← TypeScript 타입 정의
│       ├── styles/              ← 공통 CSS
│       └── utils/               ← 잡다한 도우미 함수
│
└── database/                    ← 🗄️ 데이터베이스 (MySQL / MariaDB)
    ├── README.md                ← 테이블 구조 설명
    ├── erd/                     ← ERD 이미지·다이어그램
    ├── schema/                  ← 테이블 생성 SQL (CREATE TABLE)
    └── seed/                    ← 테스트용 초기 데이터 SQL (INSERT)
```

> 위 구조에서 **`build/`, `node_modules/`, `dist/` 같은 폴더는 보이지 않습니다.**
> 그 폴더들은 각자 컴퓨터에서 빌드할 때 자동으로 생기고, GitHub에는 올리지 않기 때문입니다.

---

## 처음 참여하는 팀원이 해야 할 일

### 1단계 · 저장소 내려받기

```bash
git clone https://github.com/jnnxeo/MINI_PROJECT_1_Local_Culture_Tour_Guide.git
cd MINI_PROJECT_1_Local_Culture_Tour_Guide
```

### 2단계 · 문서 읽기 (순서대로)

1. [`docs/00_github_기초가이드.md`](docs/00_github_기초가이드.md) — 깃허브가 처음이라면 필독
2. [`docs/03_개발환경설정.md`](docs/03_개발환경설정.md) — JDK·Node.js·DB 설치
3. [`docs/01_브랜치전략.md`](docs/01_브랜치전략.md) + [`docs/02_커밋컨벤션.md`](docs/02_커밋컨벤션.md) — 작업 규칙

### 3단계 · 내 작업 브랜치 만들기

```bash
git checkout develop        # 개발 기준 브랜치로 이동
git pull origin develop     # 최신 상태로 맞추기
git checkout -b feat/기능이름  # 내 작업 브랜치 생성
```

> ⚠️ **`main` 브랜치에서 직접 작업하지 마세요.** 자세한 내용은 [`docs/01_브랜치전략.md`](docs/01_브랜치전략.md)

### 4단계 · 각자 맡은 영역 초기화

폴더 구조만 있고 실제 프로젝트 파일(빌드 설정)은 아직 없습니다.
백엔드/프론트 담당자가 아래 문서를 보고 **한 번만** 생성합니다.

- 백엔드: [`backend/README.md`](backend/README.md) 의 "프로젝트 최초 생성" 항목
- 프론트: [`frontend/README.md`](frontend/README.md) 의 "프로젝트 최초 생성" 항목

---

## 절대 커밋하면 안 되는 것들

아래 항목들은 `.gitignore`에 이미 등록되어 있어 **자동으로 제외**되지만, 원리는 알고 있어야 합니다.

| 항목 | 이유 |
|---|---|
| `node_modules/` | 용량이 수백 MB. `package.json`만 있으면 재생성 가능 |
| `build/`, `dist/`, `out/`, `.gradle/` | 빌드하면 자동 생성되는 결과물. 사람마다 달라 충돌만 남 |
| `application.properties`, `application.yml`, `.env` | **DB 비밀번호·API 키**가 들어감. 유출되면 사고 |
| `.idea/`, `.vscode/`, `*.iml` | 각자 쓰는 에디터 설정. 공유하면 서로 방해됨 |
| `.DS_Store`, `Thumbs.db` | 맥/윈도우가 멋대로 만드는 시스템 파일 |

> 🔐 **API 키·비밀번호를 실수로 커밋했다면** 즉시 팀에 알리고 해당 키를 폐기·재발급해야 합니다.
> 커밋 기록에 한 번 올라가면 파일을 지워도 기록에는 남습니다.

---

## 문서 목록

| 문서 | 내용 |
|---|---|
| [docs/00_github_기초가이드.md](docs/00_github_기초가이드.md) | clone·add·commit·push·PR을 그림처럼 따라 하는 가이드 |
| [docs/01_브랜치전략.md](docs/01_브랜치전략.md) | main / develop / feat 브랜치 규칙 |
| [docs/02_커밋컨벤션.md](docs/02_커밋컨벤션.md) | 커밋 메시지 쓰는 법 |
| [docs/03_개발환경설정.md](docs/03_개발환경설정.md) | JDK 17 · Node.js 20 · MySQL/MariaDB 설치 |
| [docs/04_프로젝트_요구사항_요약.md](docs/04_프로젝트_요구사항_요약.md) | 우리가 만들 기능 목록과 우선순위 |
| [docs/05_API_명세_작성규칙.md](docs/05_API_명세_작성규칙.md) | API 주소·응답 형식 규칙 |
| [docs/06_자주묻는질문_트러블슈팅.md](docs/06_자주묻는질문_트러블슈팅.md) | 자주 나는 에러와 해결법 |

---

## 참고

- 이 저장소의 폴더 구조는 팀의 이전 프로젝트 [ScholarBee](https://github.com/yejun05011/ScholarBee)의 구성을 참고해 정리했습니다.
- 상세 요구사항·기능·화면·DB 정의는 팀 공유 엑셀 문서(`08_TripAI_요구사항정의서.xlsx`)를 기준으로 합니다.
