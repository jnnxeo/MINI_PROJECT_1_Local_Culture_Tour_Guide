import api from './api.js'

const ACCESS_TOKEN_KEY = 'tripai.accessToken'
const ACCESS_TOKEN_EXPIRES_AT_KEY = 'tripai.accessTokenExpiresAt'
const USE_MOCK_API = import.meta.env.VITE_USE_MOCK_API !== 'false'

const wait = (milliseconds) => new Promise((resolve) => {
  setTimeout(resolve, milliseconds)
})

function notifyAuthExpired() {
  window.dispatchEvent(new Event('tripai:auth-expired'))
}

async function mockSignup({ email }) {
  await wait(800)

  if (email.toLowerCase() === 'duplicate@tripai.com') {
    const error = new Error('이미 사용 중인 이메일입니다.')
    error.status = 409
    throw error
  }

  return {
    success: true,
    data: {
      userId: 1,
      email,
    },
    message: null,
  }
}

export async function signup({ email, password }) {
  if (USE_MOCK_API) {
    return mockSignup({ email, password })
  }

  try {
    const { data } = await api.post(
      '/api/auth/signup',
      { email, password },
      { skipAuthSessionHandling: true },
    )

    if (!data.success) {
      throw new Error(data.message ?? '회원가입 처리 중 오류가 발생했습니다.')
    }

    return data
  } catch (error) {
    const signupError = new Error(getErrorMessage(error))
    signupError.status = error.response?.status ?? error.status
    throw signupError
  }
}

export async function requestLogin({ email, password }) {
  const { data } = await api.post(
    '/api/auth/login',
    { email, password },
    { skipAuthSessionHandling: true },
  )

  if (!data.success) {
    throw new Error(data.message ?? '로그인 요청 중 오류가 발생했습니다.')
  }

  return data.data
}

export async function requestLogout() {
  await api.post('/api/auth/logout', null, { skipAuthSessionHandling: true })
}

export function saveAuth({ accessToken, expiresIn }) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(
    ACCESS_TOKEN_EXPIRES_AT_KEY,
    String(Date.now() + expiresIn * 1000),
  )
}

export function getAccessToken() {
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY)
  const expiresAt = Number(localStorage.getItem(ACCESS_TOKEN_EXPIRES_AT_KEY))

  if (!accessToken || !expiresAt || Date.now() >= expiresAt) {
    if (accessToken || expiresAt) {
      clearAuth()
      notifyAuthExpired()
    }
    return null
  }

  return accessToken
}

export function getAuthExpirationDelay() {
  const expiresAt = Number(localStorage.getItem(ACCESS_TOKEN_EXPIRES_AT_KEY))
  return Number.isFinite(expiresAt) ? Math.max(0, expiresAt - Date.now()) : 0
}

export function clearAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY)
}

export function getErrorMessage(error) {
  if (error.response?.data?.message) {
    return error.response.data.message
  }

  if (error.response) {
    return '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }

  return '서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.'
}
