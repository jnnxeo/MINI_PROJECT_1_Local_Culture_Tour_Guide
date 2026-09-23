import axios from "axios";

// TODO(연동): 나중에 프론트 .env의 VITE_API_BASE_URL로 교체
// const BASE_URL = "http://localhost:8080";

// vite 테스트용
const BASE_URL = "";



// ===== API 호출 로직 (유지) =====
// export function addFavorite(eventContentId) {
//     return axios.post(`${BASE_URL}/api/favorites/events/${eventContentId}`);
// }

// export function removeFavorite(eventContentId) {
//     return axios.delete(`${BASE_URL}/api/favorites/events/${eventContentId}`);
// }

// export function getFavorite() {
//     return axios.get(`${BASE_URL}/api/favorites/events`);
// }
// ==============================


// 테스트용
const authConfig = {
    auth: { username: "user", password: "1234" }   // TODO(연동): 로그인 붙으면 JWT 토큰으로 교체
};

export function getFavorite() {
    return axios.get(`${BASE_URL}/api/favorites/events`, authConfig);
}
export function addFavorite(eventContentId) {
    return axios.post(`${BASE_URL}/api/favorites/events/${eventContentId}`, null, authConfig);
}
export function removeFavorite(eventContentId) {
    return axios.delete(`${BASE_URL}/api/favorites/events/${eventContentId}`, authConfig);
}