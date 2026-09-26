import api from './api.js'
import { getErrorMessage } from './authService.js'
import { mockGetMonthlyEvents, mockGetHomeEventPreviews, mockHasMatchingEvents } from './mock/eventMock.js'

const USE_MOCK_API = import.meta.env.VITE_USE_EVENT_MOCK_API
  ? import.meta.env.VITE_USE_EVENT_MOCK_API !== 'false'
  : import.meta.env.VITE_USE_MOCK_API !== 'false'

// API-EVENT-003: GET /api/events/months/{YYYY-MM}
export async function getMonthlyEvents(month) {
  if (USE_MOCK_API) return mockGetMonthlyEvents(month)

  try {
    const { data } = await api.get(`/api/events/months/${month}`)
    if (data?.success === false) {
      throw new Error(data.message ?? '행사 목록을 불러오지 못했습니다.')
    }

    const result = data?.data ?? data
    return {
      month: result.month ?? month,
      items: result.items ?? [],
      totalCount: result.totalCount ?? result.items?.length ?? 0,
    }
  } catch (error) {
    throw new Error(error.response ? getErrorMessage(error) : error.message || '서버에 연결할 수 없습니다.')
  }
}

// 메인 미리보기: 전체/전시/공연을 각각 최대 10개만 요청한다.
export async function getHomeEventPreviews(month) {
  if (USE_MOCK_API) return mockGetHomeEventPreviews(month)

  const load = async (category) => {
    const params = { month, page: 0, size: 10 }
    if (category) params.category = category
    try {
      const { data } = await api.get('/api/events', { params })
      if (data?.success === false) throw new Error(data.message ?? '행사 목록을 불러오지 못했습니다.')
      const result = data?.data ?? data
      const items = result?.items ?? result?.content ?? []
      return items.slice(0, 10)
    } catch (error) {
      throw new Error(error.response ? getErrorMessage(error) : error.message || '서버에 연결할 수 없습니다.')
    }
  }

  const [all, exhibition, performance] = await Promise.all([
    load(null), load('전시'), load('공연'),
  ])
  return { all, exhibition, performance }
}

// 검색 화면으로 이동하기 전에 첫 결과가 있는지만 확인한다.
export async function hasMatchingEvents(params) {
  if (USE_MOCK_API) return mockHasMatchingEvents(params)

  const query = new URLSearchParams(params)
  query.set('page', '0')
  query.set('size', '1')
  try {
    const { data } = await api.get(`/api/events?${query}`)
    if (data?.success === false) throw new Error(data.message ?? '행사를 검색하지 못했습니다.')
    const result = data?.data ?? data
    const items = result?.items ?? result?.content ?? []
    return (result?.totalCount ?? result?.totalElements ?? items.length) > 0
  } catch (error) {
    throw new Error(error.response ? getErrorMessage(error) : error.message || '서버에 연결할 수 없습니다.')
  }
}
