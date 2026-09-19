# external — 외부 API 연동

우리 서버가 **다른 회사·기관의 API를 호출**하는 코드를 모읍니다.

## 연동 대상

| 대상 | 용도 | 요구사항 ID |
|---|---|---|
| 서울 열린데이터광장 (문화행사) | 날짜·자치구·분야별 행사 조회 | DATA-01 |
| TourAPI (한국관광공사) | 관광지·문화시설 정보 | DATA-02 |
| 카카오 Local API | 주변 음식점·숙박 검색 | DATA-02 |
| AI API | 일정 제목·추천 이유 생성 | AI-01 |

## 파일명 규칙

```
{대상}Client.java
```

예: `SeoulEventClient.java`, `TourApiClient.java`, `KakaoLocalClient.java`

## ⚠️ 반드시 지킬 것

1. **API 키는 코드에 직접 쓰지 마세요.** `application.properties`에서 읽어옵니다.

   ```java
   @Value("${seoul.api.key}")
   private String apiKey;     // ✅ 설정 파일에서 주입
   ```

2. **외부 API가 실패해도 서비스 전체가 멈추면 안 됩니다.** (요구사항 UX-02)
   실패 시 안내 메시지를 반환하고, 가능한 부분은 살려서 보여줍니다.

3. **실제 응답 필드·호출 제한을 연동 전에 꼭 확인**하세요. 문서와 다를 수 있습니다.

4. 받아온 데이터는 **공통 Place 형태로 변환**해서 사용합니다 (DATA-03).
