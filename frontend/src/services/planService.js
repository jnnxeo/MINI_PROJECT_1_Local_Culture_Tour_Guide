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

// 나의 일정 API — 팀 시트의 API-PLAN 명세와 기존 편집 화면 응답을 연결한다.
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

// API 명세의 draftId/visitDate/placeId와 화면의 planId/tripDate/contentId를 연결한다.
function normalizePlan(detail) {
  if (!detail || typeof detail !== 'object') {
    throw new Error('일정 상세 응답 형식이 맞지 않습니다.')
  }
  const items = (detail.items ?? []).map((item) => ({
    ...item,
    contentId: item.contentId ?? item.placeId,
    seq: item.seq ?? item.sequence,
    address: item.address ?? item.addr,
    endTime: item.endTime ?? (item.startTime && item.durationMin
      ? (() => {
        const [hours, minutes] = item.startTime.split(':').map(Number)
        const total = hours * 60 + minutes + item.durationMin
        return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
      })()
      : null),
  }))
  const anchorEventId = detail.anchorEventId
    ?? detail.selectedEvent?.eventId
    ?? items.find((item) => item.type === 'EVENT')?.contentId

  return {
    ...detail,
    planId: detail.planId ?? detail.draftId,
    tripDate: detail.tripDate ?? detail.visitDate,
    anchorEventId,
    visitStartTime: detail.visitStartTime ?? detail.conditions?.startTime,
    visitEndTime: detail.visitEndTime ?? detail.conditions?.endTime,
    saved: detail.saved ?? (detail.draftId == null),
    items,
  }
}

/** API-PLAN-002 최근 초안 복원 */
export function getLatestDraft({ forceApi = false, draftId = null } = {}) {
  if (USE_MOCK_API && !forceApi) {
    return withMock(mockGetLatestDraft)
  }

  return request(() => api.get(draftId
    ? `/api/plans/drafts/${encodeURIComponent(draftId)}`
    : '/api/plans/drafts/latest')).then(normalizePlan)
}

/** API-PLAN-003 일정 상세 */
export function getPlan(planId, { forceApi = false } = {}) {
  if (USE_MOCK_API && !forceApi) {
    return withMock(() => mockGetPlan(planId))
  }

  return request(() => api.get(`/api/plans/${planId}`)).then(normalizePlan)
}

/** 저장 전 초안은 PUT, 저장 일정 수정(API-PLAN-012)은 PATCH로 전체 항목을 보낸다. */
export function savePlan(planId, { title, tripDate, anchorEventId, items }, { forceApi = false, saved = false } = {}) {
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

  if (USE_MOCK_API && !forceApi) {
    return withMock(() => mockSavePlan(planId, body))
  }

  return request(() => saved
    ? api.patch(`/api/plans/${planId}`, body)
    : api.put(`/api/plans/${planId}`, body))
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


/** EVENT-003: 선택한 행사를 기준으로 당일 일정 초안을 만든다. */
export function createEventDraft({
  eventId,
  tripDate,
  headcount,
  transportMode,
  interests,
  useAi,
}) {
  return request(() => api.post('/api/plans/recommend', {
    eventId,
    visitDate: tripDate,
    headcount,
    transportMode,
    interests,
    useAi,
  }))
}

/** EVENT-004: 저장한 일정 목록. 실제 서버 응답은 팀 계약 확정 후 하나로 통일한다. */
export async function getSavedPlans() {
  const all = []
  const pageSize = 20
  for (let page = 0; page < 100; page += 1) {
    const data = await request(() => api.get('/api/plans', { params: { page, size: pageSize } }))
    const plans = Array.isArray(data) ? data : (data.items ?? data.plans)
    if (!Array.isArray(plans)) throw new Error('저장한 일정 목록의 응답 형식이 맞지 않습니다.')
    all.push(...plans)
    if (plans.length < pageSize || data.totalCount != null && all.length >= data.totalCount) return all
  }
  throw new Error('저장한 일정이 많아 목록을 모두 불러오지 못했습니다.')
}
