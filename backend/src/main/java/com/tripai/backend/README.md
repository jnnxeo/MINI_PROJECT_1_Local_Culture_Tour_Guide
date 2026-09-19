# com.tripai.backend — 백엔드 소스 루트

모든 Java 코드가 이 아래에 들어갑니다. **역할별로 폴더가 나뉘어 있으니** 맞는 위치에 파일을 만들어 주세요.

| 폴더 | 역할 | 파일명 예시 |
|---|---|---|
| `controller/` | 프론트 요청을 받는 입구 | `AuthController.java` |
| `service/` | 실제 로직 처리 | `PlanService.java` |
| `repository/` | DB 조회·저장 | `MemberRepository.java` |
| `domain/entity/` | DB 테이블과 대응 | `Member.java` |
| `domain/dto/` | 화면과 주고받는 데이터 | `LoginRequest.java` |
| `external/` | 외부 API 연동 | `SeoulEventClient.java` |
| `global/` | 공통 설정·에러·토큰 | `SecurityConfig.java` |

> 이 폴더에는 실행 진입점인 `BackendApplication.java` 도 함께 위치합니다.
> (Spring Initializr로 프로젝트를 생성하면 자동으로 만들어집니다)

## 왜 폴더를 나누나요?

한 파일에 전부 넣으면 나중에 찾기도, 고치기도 어렵습니다.
**"요청 받기 / 로직 / DB 접근"을 분리**해 두면 문제가 생겼을 때 어디를 봐야 할지 바로 알 수 있습니다.
