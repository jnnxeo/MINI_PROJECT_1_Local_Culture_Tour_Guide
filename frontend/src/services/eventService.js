import api from './api.js'

export async function getEventDetail(eventId) {
  try {
    const response = await api.get(
      `/api/events/${encodeURIComponent(eventId)}`
    )
    return response.data.data
  } catch (error) {
    const eventError = new Error(
      error.response?.data?.message || '행사 정보를 불러오지 못했습니다.'
    )
    eventError.status = error.response?.status
    throw eventError
  }
}