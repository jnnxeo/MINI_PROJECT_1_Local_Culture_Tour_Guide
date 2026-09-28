import api from './api.js'

// 공통 api 클라이언트가 로그인 JWT를 Authorization 헤더에 넣는다.
export function getFavorite({ page = 0, size = 20 } = {}) {
  return api.get('/api/favorites/events', { params: { page, size } })
}

export function addFavorite(eventContentId) {
  return api.post(`/api/favorites/events/${encodeURIComponent(eventContentId)}`)
}

export function removeFavorite(eventContentId) {
  return api.delete(`/api/favorites/events/${encodeURIComponent(eventContentId)}`)
}
