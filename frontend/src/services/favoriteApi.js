import axios from "axios";

// TODO(연동): 나중에 프론트 .env의 VITE_API_BASE_URL로 교체
const BASE_URL = "http://localhost:8080";

// ===== API 호출 로직 (유지) =====
export function addFavorite(eventContentId) {
    return axios.post(`${BASE_URL}/api/favorites/events/${eventContentId}`);
}

export function removeFavorite(eventContentId) {
    return axios.delete(`${BASE_URL}/api/favorites/events/${eventContentId}`);
}
// ==============================