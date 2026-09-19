# entity — DB 테이블과 1:1로 대응되는 클래스

`@Entity`가 붙은 클래스입니다. **테이블 한 개 = 클래스 한 개**로 생각하면 됩니다.

| 클래스 | 테이블 |
|---|---|
| `Member.java` | `member` |
| `TripPlan.java` | `trip_plan` |
| `PlanItem.java` | `plan_item` |
| `Place.java` | `place` |

## 이름 규칙 주의

| 구분 | 표기 | 예시 |
|---|---|---|
| DB 컬럼 | `snake_case` | `start_date` |
| Java 필드 | `camelCase` | `startDate` |

> Spring Boot 설정에서 자동 변환되도록 해두면 편합니다.

## 주의

⚠️ **Entity를 그대로 API 응답으로 내보내지 마세요.**
비밀번호 같은 민감한 필드가 노출될 수 있습니다. → `dto/` 를 사용하세요.

> 테이블 상세 정의는 [`database/README.md`](../../../../../../../../../database/README.md) 참고
