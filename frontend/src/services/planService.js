import api from './api.js'
import { getErrorMessage } from './authService.js'
import {
  mockGetLatestDraft,
  mockGetPlan,
  mockRegeneratePlan,
  mockSavePlan,
  mockSearchPlaces,
  resetMockPlans,
} from './mock/planMock.js'

// 나의 일정 API — 계약: docs/07_나의일정_API_계약.md
const USE_MOCK_API = import.meta.env.VITE_USE_PLAN_MOCK_API
  ? import.meta.env.VITE_USE_PLAN_MOCK_API !== 'false'
  : import.meta.env.VITE_USE_MOCK_API !== 'false'

if (USE_MOCK_API && import.meta.env.DEV) {
  window.tripaiResetMockPlans = resetMockPlans
}

async function request(call) {
  try {
    const { data } = await call()

    if (!data.success) {
      throw new Error(data.message ?? '요청을 처리하지 못했습니다.')
    }

    return data.data
  } catch (error) {
    const planError = new Error(error.response ? getErrorMessage(error) : error.message)
    planError.status = error.response?.status ?? error.status
    throw planError
  }
}

async function withMock(mockCall) {
  try {
    return await mockCall()
  } catch (error) {
    const planError = new Error(error.message)
    planError.status = error.status
    throw planError
  }
}

/** API-PLAN-002 최근 초안 복원 */
export function getLatestDraft() {
  if (USE_MOCK_API) {
    return withMock(mockGetLatestDraft)
  }

  return request(() => api.get('/api/plans/drafts/latest'))
}

/** API-PLAN-003 일정 상세 */
export function getPlan(planId) {
  if (USE_MOCK_API) {
    return withMock(() => mockGetPlan(planId))
  }

  return request(() => api.get(`/api/plans/${planId}`))
}

/** API-PLAN-004 저장 / 수정 저장 — 화면 순서대로 항목 전체를 보낸다. */
export function savePlan(planId, { title, tripDate, anchorEventId, items }) {
  const body = {
    title: title.trim(),
    tripDate,
    anchorEventId,
    items: items.map(({ type, contentId, startTime, durationMin, aiReason }) => ({
      type,
      contentId,
      startTime,
      durationMin,
      aiReason: aiReason ?? null,
    })),
  }

  if (USE_MOCK_API) {
    return withMock(() => mockSavePlan(planId, body))
  }

  return request(() => api.put(`/api/plans/${planId}`, body))
}

/** API-PLAN-005 다시 추천(conditions = null) / 조건 수정 */
export function regeneratePlan(planId, conditions = null) {
  if (USE_MOCK_API) {
    return withMock(() => mockRegeneratePlan(planId, conditions))
  }

  return request(() => api.post(`/api/plans/${planId}/regenerate`, { conditions }))
}

/** API-PLACE-001 맛집 후보 */
export function searchPlaces({ eventId, keyword = '', time, durationMin }) {
  if (USE_MOCK_API) {
    return withMock(() => mockSearchPlaces({ eventId, keyword, time, durationMin }))
  }

  return request(() => api.get('/api/places', {
    params: { eventId, keyword: keyword || undefined, time, durationMin },
  }))
}
