# Backend (Java · Spring Boot · Gradle)

백엔드 서버 디렉토리입니다. 프론트엔드의 요청을 받아 **DB 조회·외부 API 연동·AI 일정 생성**을 처리하고 JSON으로 응답합니다.

> 📌 **현재 상태**: Spring Boot·Gradle·MyBatis 공통 초기 설정이 완료되었습니다.
> 기능별 API 코드는 각자 `feat/*` 브랜치에서 추가합니다.

---

## 폴더 구조

```
backend/
├── README.md                      ← 지금 이 파일
└── src/
    ├── main/
    │   ├── java/com/tripai/backend/
    │   │   ├── controller/        ← 요청을 받는 입구 (URL 매핑)
    │   │   ├── service/           ← 실제 로직 처리 (핵심)
    │   │   ├── repository/        ← DB 조회·저장
    │   │   ├── domain/
    │   │   │   ├── entity/        ← DB 테이블과 대응되는 클래스
    │   │   │   └── dto/           ← 화면과 주고받는 데이터 형태
    │   │   ├── external/          ← 서울시 문화행사·TourAPI·카카오 연동
    │   │   └── global/
    │   │       ├── config/        ← 설정 (CORS·Security 등)
    │   │       ├── exception/     ← 에러 처리
    │   │       ├── jwt/           ← 로그인 토큰
    │   │       └── response/      ← 공통 응답 형식
    │   └── resources/
    │       ├── README.md          ← ⚠️ 비밀 설정 파일 안내 (필독)
    │       ├── static/            ← 정적 파일
    │       └── templates/         ← (선택) 서버 렌더링 템플릿
    └── test/java/com/tripai/backend/   ← 테스트 코드
```

> 📦 패키지명은 `com.tripai.backend` 입니다. (프로젝트명 TripAI 기준 · 팀 협의로 변경 가능)

---

## 요청이 처리되는 흐름

```
[프론트엔드]
     │  ① HTTP 요청 (예: GET /api/plans/1)
     ▼
 Controller   ← 요청을 받고, 값이 올바른지 1차 확인
     │  ②
     ▼
  Service     ← 실제 로직 (조건 검증, 일정 조합, AI 호출)
     │  ③                    │
     ▼                       ▼
 Repository              External      ← 서울시 API·TourAPI·카카오
     │  ④ SQL                │ 외부 HTTP
     ▼                       ▼
   [ DB ]              [ 외부 서버 ]
```

| 계층 | 하는 일 | 하지 말아야 할 일 |
|---|---|---|
| Controller | 요청 받기, 응답 내보내기 | 복잡한 로직 작성 ❌ |
| Service | 비즈니스 로직 | SQL 직접 작성 ❌ |
| Repository | DB 접근 | 로직 판단 ❌ |
| External | 외부 API 호출 | 화면용 데이터 가공 ❌ |

---

## 공통 초기 설정

공통 초기 설정은 이미 저장소에 포함되어 있습니다. 팀원은 새 Spring Boot 프로젝트를 생성하지 않습니다.

- Java 17 · Spring Boot 3.5 · Gradle Wrapper
- Spring Web · Validation · Spring Security
- MyBatis · MySQL Driver · Lombok
- CORS 설정, 공통 API 응답, 전역 입력 검증 예외 처리

공용 `src/main/resources/application.yml`은 환경 변수 이름과 MyBatis 설정만 관리합니다.
개인 DB 비밀번호와 JWT 비밀 키는 `backend/.env.example`을 참고해 각자 `backend/.env`에 입력합니다.
`.env` 파일은 Git에 올리지 않습니다.

---

## 실행 방법

```bash
cd backend

# 맥 / 리눅스
./gradlew bootRun

# 윈도우
gradlew.bat bootRun
```

서버가 뜨면: http://localhost:8080

### 자주 쓰는 Gradle 명령

| 명령 | 설명 |
|---|---|
| `./gradlew bootRun` | 서버 실행 |
| `./gradlew build` | 빌드 (테스트 포함) |
| `./gradlew test` | 테스트만 실행 |
| `./gradlew clean` | 빌드 결과물 삭제 |

> 💡 Gradle을 따로 설치할 필요 없습니다. `gradlew`가 알아서 내려받습니다.

---

## ⚠️ 커밋 주의사항

| 커밋 **O** | 커밋 **X** |
|---|---|
| `build.gradle`, `settings.gradle` | `build/` (빌드 결과물) |
| `gradlew`, `gradlew.bat` | `.gradle/` (캐시) |
| `gradle/wrapper/` | `application.properties` 🔐 |
| `src/` 아래 소스 코드 | `*.log`, `.idea/` |

> 🔐 `application.properties`에는 **DB 비밀번호와 API 키**가 들어갑니다.
> `.gitignore`에 등록되어 있어 자동으로 제외되지만, 강제로 추가하지 마세요.
> 설정 항목은 [`src/main/resources/README.md`](src/main/resources/README.md) 참고

---

## 담당 모듈 참고

요구사항 정의서 기준 모듈 구분입니다. → [`../docs/04_프로젝트_요구사항_요약.md`](../docs/04_프로젝트_요구사항_요약.md)

| 모듈 | 주요 기능 | 관련 패키지 |
|---|---|---|
| Auth | 회원가입·로그인·로그아웃 | `controller`, `service`, `global/jwt` |
| Search | 행사 검색·필터 | `controller`, `service` |
| Data | 외부 API 수집·중복 제거 | `external`, `service` |
| Plan | 일정 생성·저장·수정 | `controller`, `service`, `repository` |
| Favorite | 즐겨찾기 | `controller`, `service`, `repository` |
| Security | 입력 검증·권한 확인 | `global/config`, `global/exception` |
