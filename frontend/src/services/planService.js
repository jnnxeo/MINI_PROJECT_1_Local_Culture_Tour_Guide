import api from './api.js'
import { getErrorMessage } from './authService.js'
import {
  MOCK_DRAFT_ID,
  mockAddItem,
  mockGetDraft,
  mockRegenerate,
  mockRemoveItem,
  mockReplaceItems,
  mockSaveDraft,
  mockSearchRestaurants,
  mockUpdateConditions,
  mockUpdateTitle,
  resetMockPlans,
} from './mock/planMock.js'

/*
 * 나의 일정(저장 전 초안) API
 * 기준: 08_TripAI_요구사항정의서 「API 명세」 API-PLAN-001~009, API-PLACE-001
 * 저장 일정(API-PLAN-010~013)은 내 여행 화면 담당 범위라 여기 두지 않는다.
 */
const USE_MOCK_API = import.meta.env.VITE_USE_PLAN_MOCK_API
  ? import.meta.env.VITE_USE_PLAN_MOCK_API !== 'false'
  : import.meta.env.VITE_USE_MOCK_API !== 'false'

const DRAFT_ID_KEY = 'tripai.draftId'

if (USE_MOCK_API && import.meta.env.DEV) {
  window.tripaiResetMockPlans = resetMockPlans
}

async function request(call) {
  try {
    const { data } = await call()

    if (data && data.success === false) {
      throw new Error(data.message ?? '요청을 처리하지 못했습니다.')
    }

    return data?.data
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

const draftPath = (draftId) => `/api/plans/drafts/${draftId}`

/**
 * 지금 편집할 초안 ID (UX-005 초안 복원)
 * 추천 생성(API-PLAN-001) 후 /trips/draft로 이동할 때 state 또는 sessionStorage로 넘겨받는다.
 * 목업 모드에서는 개발용 초안을 쓴다.
 */
export function resolveDraftId(stateDraftId) {
  if (stateDraftId) {
    rememberDraftId(stateDraftId)
    return stateDraftId
  }

  try {
    const stored = sessionStorage.getItem(DRAFT_ID_KEY)

    if (stored) {
      return Number(stored)
    }
  } catch {
    // 저장소를 읽을 수 없으면 아래로 진행
  }

  return USE_MOCK_API ? MOCK_DRAFT_ID : null
}

export function rememberDraftId(draftId) {
  try {
    sessionStorage.setItem(DRAFT_ID_KEY, String(draftId))
  } catch {
    // 저장소를 쓸 수 없어도 현재 화면은 동작한다.
  }
}

export function forgetDraftId() {
  try {
    sessionStorage.removeItem(DRAFT_ID_KEY)
  } catch {
    // 무시
  }
}

/** API-PLAN-001: 행사 상세에서도 나의 일정 팀의 추천 API 계약을 사용한다. */
export async function recommendDraft({
  eventId, visitDate, startTime, endTime, companion, foodPreference, mealType, transportMode, headcount,
}) {
  const body = { eventId, visitDate, startTime, endTime, companion, foodPreference, mealType, transportMode, headcount }

  const draft = USE_MOCK_API
    ? await withMock(() => {
      resetMockPlans()
      return mockGetDraft(MOCK_DRAFT_ID)
    })
    : await request(() => api.post('/api/plans/recommend', body))

  if (draft?.draftId == null) throw new Error('일정 초안 ID를 받지 못했습니다.')
  rememberDraftId(draft.draftId)
  return draft
}

/** API-PLAN-002 저장 전 일정 초안 조회 */
export function getDraft(draftId) {
  if (USE_MOCK_API) {
    return withMock(() => mockGetDraft(draftId))
  }

  return request(() => api.get(draftPath(draftId)))
}

/** API-PLAN-003 일정 추천 조건 수정 — Body: {visitDate,startTime,endTime,companion,foodPreference,transportMode} */
export function updateConditions(draftId, { visitDate, startTime, endTime, companion, foodPreference, transportMode }) {
  const body = { visitDate, startTime, endTime, companion, foodPreference, transportMode }

  if (USE_MOCK_API) {
    return withMock(() => mockUpdateConditions(draftId, body))
  }

  return request(() => api.patch(`${draftPath(draftId)}/conditions`, body))
}

/** API-PLAN-004 (수정한) 조건으로 일정 다시 추천 */
export function regenerateDraft(draftId) {
  if (USE_MOCK_API) {
    return withMock(() => mockRegenerate(draftId))
  }

  return request(() => api.post(`${draftPath(draftId)}/regenerate`))
}

/** API-PLAN-005 저장 전 일정 제목 변경 */
export function updateDraftTitle(draftId, title) {
  if (USE_MOCK_API) {
    return withMock(() => mockUpdateTitle(draftId, title))
  }

  return request(() => api.patch(`${draftPath(draftId)}/title`, { title }))
}

/** API-PLAN-006 일정 항목·시간·방문 순서 일괄 수정 — Body: {items:[{itemId,type,placeId,startTime,durationMin,sequence}]} */
export function replaceDraftItems(draftId, items) {
  const body = {
    items: items.map(({ itemId, type, placeId, startTime, durationMin, sequence }) => ({
      itemId,
      type,
      placeId,
      startTime,
      durationMin,
      sequence,
    })),
  }

  if (USE_MOCK_API) {
    return withMock(() => mockReplaceItems(draftId, body.items))
  }

  return request(() => api.put(`${draftPath(draftId)}/items`, body))
}

/** API-PLAN-007 맛집 일정에 추가 — Body: {placeId,type,startTime,durationMin,sequence} */
export function addDraftItem(draftId, { placeId, type, startTime, durationMin, sequence }) {
  const body = { placeId, type, startTime, durationMin, sequence }

  if (USE_MOCK_API) {
    return withMock(() => mockAddItem(draftId, body))
  }

  return request(() => api.post(`${draftPath(draftId)}/items`, body))
}

/** API-PLAN-008 일정 항목 삭제 */
export function removeDraftItem(draftId, itemId) {
  if (USE_MOCK_API) {
    return withMock(() => mockRemoveItem(draftId, itemId))
  }

  return request(() => api.delete(`${draftPath(draftId)}/items/${itemId}`))
}

/** API-PLAN-009 일정 초안을 저장 일정으로 확정 — Body: {draftId} → {planId,title,visitDate,dDay} */
export function saveDraftAsPlan(draftId) {
  if (USE_MOCK_API) {
    return withMock(() => mockSaveDraft(draftId))
  }

  return request(() => api.post('/api/plans', { draftId }))
}

/** API-PLACE-001 행사 주변 맛집 후보 조회 — Query: eventId, radius, category, page, size */
export function searchRestaurants({ eventId, radius, category, page, size }) {
  if (USE_MOCK_API) {
    return withMock(() => mockSearchRestaurants({ eventId }))
  }

  return request(() => api.get('/api/places/restaurants', {
    params: { eventId, radius, category, page, size },
  }))
}

/** EVENT-003: 저장 일정 선택 팝업용 목록. 목록 API는 내 여행 담당 구현과 연동한다. */
export async function getSavedPlans() {
  const all = []
  const pageSize = 20
  for (let page = 0; page < 100; page += 1) {
    const data = await request(() => api.get('/api/plans', { params: { page, size: pageSize } }))
    const plans = Array.isArray(data) ? data : (data?.items ?? data?.plans)
    if (!Array.isArray(plans)) throw new Error('저장한 일정 목록의 응답 형식이 맞지 않습니다.')
    all.push(...plans.map((plan) => ({
      ...plan,
      planId: plan.planId ?? plan.tripPlanId,
      tripDate: plan.tripDate ?? plan.visitDate,
    })))
    if (plans.length < pageSize || data?.totalCount != null && all.length >= data.totalCount) return all
  }
  throw new Error('저장한 일정이 많아 목록을 모두 불러오지 못했습니다.')
}

/** EVENT-003: 저장 일정의 행사 중복·날짜 충돌을 확인할 상세 조회. */
export async function getPlan(planId) {
  const data = await request(() => api.get(`/api/plans/${encodeURIComponent(planId)}`))
  if (!data || typeof data !== 'object') throw new Error('일정 상세 응답 형식이 맞지 않습니다.')
  return {
    ...data,
    planId: data.planId ?? data.tripPlanId,
    tripDate: data.tripDate ?? data.visitDate,
    items: (data.items ?? []).map((item) => ({
      ...item,
      contentId: item.contentId ?? item.eventId ?? item.placeId,
      seq: item.seq ?? item.sequence,
    })),
  }
}

/** EVENT-003: 저장 일정에 행사 추가. 서버가 소유권·중복·개수·시간 충돌을 다시 검사한다. */
export function addEventToSavedPlan(planId, eventId, startTime) {
  return request(() => api.post(`/api/plans/${encodeURIComponent(planId)}/events`, {
    eventId,
    startTime,
  }))
}

export function updateSavedEventTime(planId, itemId, startTime) {
  return request(() => api.patch(
    `/api/plans/${encodeURIComponent(planId)}/events/${encodeURIComponent(itemId)}`,
    { startTime },
  ))
}
