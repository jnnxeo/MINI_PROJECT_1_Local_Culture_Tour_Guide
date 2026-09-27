import { mockSearchEvents } from './mock/eventMock.js'

// #53 병합 후 이 함수 내부의 조회만 GET /api/events로 교체한다.
// 화면은 API-EVENT-001과 같은 items, page, totalCount 응답 형태를 사용한다.
export async function getEventSearchResults(params) {
  return mockSearchEvents(params)
}
