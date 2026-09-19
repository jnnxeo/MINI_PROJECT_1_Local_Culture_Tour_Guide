# resources — 설정 파일과 정적 자원

## ⚠️ 가장 중요: application.properties

이 폴더에 `application.properties` 파일을 **각자 직접 만들어** 사용합니다.
**이 파일은 `.gitignore`에 등록되어 커밋되지 않습니다.** (DB 비밀번호·API 키가 들어가기 때문)

### 왜 커밋하면 안 되나요?

GitHub는 공개 저장소입니다. 비밀번호나 API 키가 올라가면 **누구나 볼 수 있고**,
한 번 커밋되면 파일을 지워도 **기록에는 계속 남습니다.**

### 만들어야 할 설정 항목

아래 항목들을 채워서 `application.properties`를 직접 작성하세요.
**실제 값은 팀 카톡·노션 등 저장소 밖에서 공유**합니다.

```properties
# --- 서버 ---
server.port=8080

# --- 데이터베이스 ---
spring.datasource.url=jdbc:mysql://localhost:3306/tripai?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
spring.datasource.username=(본인 DB 계정)
spring.datasource.password=(본인 DB 비밀번호)
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# --- JPA ---
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# --- JWT ---
jwt.secret=(충분히 긴 임의의 문자열)
jwt.expiration=3600000

# --- 외부 API 키 ---
seoul.api.key=(서울 열린데이터광장 키)
tour.api.key=(TourAPI 키)
kakao.api.key=(카카오 REST API 키)
```

> 💡 `ddl-auto` 값 주의
> - `update` : 테이블이 없으면 만들고, 있으면 유지 (개발 중 권장)
> - `create` : **매번 테이블을 지우고 다시 만듦** — 데이터가 전부 사라집니다 ⚠️

## 폴더 설명

| 폴더 | 용도 |
|---|---|
| `static/` | 이미지 등 그대로 제공되는 파일 |
| `templates/` | (사용 시) 서버에서 HTML을 그릴 때 |

> 우리 프로젝트는 화면을 React가 담당하므로 `templates/`는 쓰지 않을 가능성이 높습니다.
