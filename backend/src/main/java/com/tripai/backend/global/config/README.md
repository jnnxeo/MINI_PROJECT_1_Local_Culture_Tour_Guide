# config — 설정 클래스

프로젝트 전체에 적용되는 설정을 담습니다.

| 파일 예시 | 역할 |
|---|---|
| `SecurityConfig.java` | 어떤 URL에 로그인이 필요한지 설정 |
| `CorsConfig.java` | 프론트(5173)에서의 요청 허용 |
| `WebConfig.java` | 기타 웹 설정 |

## CORS가 필요한 이유

프론트는 `localhost:5173`, 백엔드는 `localhost:8080` 으로 **포트가 다릅니다.**
브라우저는 보안상 이런 요청을 기본적으로 막기 때문에, 백엔드에서 "이 주소는 허용한다"고 알려줘야 합니다.

```java
// 개발 중에는 프론트 주소를 허용
registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
```
