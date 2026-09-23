import axios from 'axios'

const ACCESS_TOKEN_KEY = 'tripai.accessToken'
const ACCESS_TOKEN_EXPIRES_AT_KEY = 'tripai.accessTokenExpiresAt'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

function clearExpiredAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY)
  window.dispatchEvent(new Event('tripai:auth-expired'))
}

api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY)
  const expiresAt = Number(localStorage.getItem(ACCESS_TOKEN_EXPIRES_AT_KEY))

  if (accessToken && expiresAt && Date.now() < expiresAt) {
    config.headers.Authorization = `Bearer ${accessToken}`
  } else if (accessToken || expiresAt) {
    clearExpiredAuth()
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response?.status === 401
      && !error.config?.skipAuthSessionHandling
    ) {
      clearExpiredAuth()
    }

    return Promise.reject(error)
  },
)

export default api
