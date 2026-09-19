# repository — DB에 접근하는 곳

데이터베이스에서 **가져오고 저장하는** 역할만 합니다.

## 하는 일

- JPA를 이용해 DB 조회·저장·삭제
- 필요하면 직접 쿼리 작성 (`@Query`)

## 하지 말아야 할 일

🚫 판단·계산 로직 — **Service의 일**입니다

## 파일명 규칙

```
{엔티티명}Repository.java
```

| 예시 | 대상 테이블 |
|---|---|
| `MemberRepository.java` | `member` |
| `TripPlanRepository.java` | `trip_plan` |
| `PlanItemRepository.java` | `plan_item` |
| `PlaceRepository.java` | `place` |

## 기본 형태

JPA를 쓰면 기본적인 조회·저장 메서드는 **자동으로 만들어집니다.**

```java
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 메서드 이름만 규칙대로 지으면 쿼리가 자동 생성됩니다
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
}
```
