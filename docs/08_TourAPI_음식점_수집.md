# TourAPI 음식점 수집

서울 음식점은 사용자 요청 때마다 TourAPI를 부르지 않고, 서버가 수집해 `place` 테이블에 저장한 뒤 추천 조회에 사용한다.

## 사전 준비

1. 팀 DDL을 적용한다.
2. 음식 분류 마이그레이션을 한 번 적용한다.

```bash
mysql -u root -p tripai < database/schema/02_place_food_category_v3.2.2.sql
```

3. `backend/.env`에 발급받은 TourAPI 서비스 키를 넣는다. 키는 GitHub에 올리지 않는다.

```dotenv
TOUR_API_SERVICE_KEY=발급받은_서비스키
```

## 한 페이지 수집

아래 명령은 기존 백엔드와 포트 충돌 없이 임시 포트에서 시작해, 지정한 한 페이지를 수집한 뒤 종료한다.

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=tour-sync --tour-api.sync.page-no=1 --tour-api.sync.num-of-rows=100'
```

실행 로그에서 요청 수·저장 수·건너뜀 수·누적 저장 수·추천 가능 수를 확인한다.

## 서울 전체 수집

TourAPI 조회 당시 서울 음식점은 961건이었다. 한 페이지를 100건으로 실행하면 1~10페이지가 필요하다.

각 음식점은 목록 1회와 상세 1회가 필요하므로, 전체 수집은 약 971회 호출한다. 개발키 일일 한도 1,000회에 가깝기 때문에 실패·재시도 여유를 고려해 페이지별로 실행한다.

```bash
for page in {1..10}; do
  ./gradlew bootRun --args="--spring.profiles.active=tour-sync --tour-api.sync.page-no=$page --tour-api.sync.num-of-rows=100"
done
```

## 추천 가능 기준

수집 데이터는 원본 보존을 위해 바로 삭제하지 않는다. 추천 조회에서 아래 조건을 모두 만족하는 항목만 후보로 쓴다.

- `display_yn = true`
- 음식 분류가 한식·양식·일식·중식 중 하나
- 좌표가 있음
- 영업 시작·종료 시간이 있음

TourAPI는 폐업 여부를 확정하는 상태값을 제공하지 않으므로, 폐업을 추정해 삭제하지 않는다. 브레이크타임은 상세 영업시간 원문을 보존하고 이후 파싱 규칙을 보완한다.
