import api from './api.js'
import { getErrorMessage } from './authService.js'
import { mockSearchEvents } from './mock/eventMock.js'

const USE_MOCK_API = import.meta.env.VITE_USE_EVENT_MOCK_API !== undefined
  ? import.meta.env.VITE_USE_EVENT_MOCK_API === 'true'
  : import.meta.env.VITE_USE_MOCK_API === 'true'

export async function getEventSearchResults(params) {
  if (USE_MOCK_API) return mockSearchEvents(params)
  try {
    const { data } = await api.get('/api/events', { params })
    if (data?.success === false) throw new Error(data.message || '행사를 찾지 못했습니다.')
    return data.data
  } catch (error) {
    throw new Error(error.response ? getErrorMessage(error) : error.message || '서버에 연결할 수 없습니다.')
  }
}
