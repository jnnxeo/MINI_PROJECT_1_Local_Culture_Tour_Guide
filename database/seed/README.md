# seed — 테스트용 초기 데이터

개발·테스트할 때 쓸 **샘플 데이터** INSERT 문을 둡니다.

## 왜 필요한가요?

DB가 비어 있으면 화면을 만들어도 아무것도 안 보입니다.
샘플 데이터를 넣어두면 **팀원 모두 같은 화면**을 보며 개발할 수 있습니다.

## 파일명 예시

```
01_sample_member.sql
02_sample_place.sql
03_sample_trip_plan.sql
```

## ⚠️ 반드시 지킬 것

🚫 **실제 사람의 개인정보를 넣지 마세요.** 가짜 데이터만 사용합니다.

```sql
-- ✅ 좋은 예
INSERT INTO member (email, password, nickname)
VALUES ('test1@example.com', '$2a$10$해시값', '테스트유저1');

-- ❌ 나쁜 예 — 실제 이메일·비밀번호
INSERT INTO member (email, password, nickname)
VALUES ('hongjunseo@naver.com', 'mypassword123', '홍준서');
```

> 비밀번호 칸에도 **평문이 아닌 해시값**을 넣으세요. 로그인 테스트가 정상 동작합니다.
