import axios from 'axios'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const USE_MOCK_API =
  import.meta.env.VITE_USE_MOCK_API !== 'false'

const wait = (milliseconds) =>
  new Promise((resolve) => {
    setTimeout(resolve, milliseconds)
  })

const mockSignup = async ({ email }) => {
  // 실제 서버 통신처럼 처리 시간이 있는 모습을 시험합니다.
  await wait(800)

  // 이 이메일을 입력하면 중복 이메일 실패 상황을 시험할 수 있습니다.
  if (email.toLowerCase() === 'duplicate@tripai.com') {
    const error = new Error('이미 사용 중인 이메일입니다.')
    error.status = 409
    throw error
  }

  return {
    success: true,
    data: {
      memberId: 1,
      email,
    },
    message: null,
  }
}

export const signup = async ({ email, password }) => {
  if (USE_MOCK_API) {
    return mockSignup({ email, password })
  }

  try {
    const response = await axios.post(
      `${API_BASE_URL}/api/auth/signup`,
      {
        email,
        password,
      },
    )

    return response.data
  } catch (error) {
    const signupError = new Error(
      error.response?.data?.message ||
        '회원가입 처리 중 오류가 발생했습니다.',
    )

    signupError.status = error.response?.status
    throw signupError
  }
}