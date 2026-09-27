import React, { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext.jsx'
import { getErrorMessage, requestLogin } from '../services/authService.js'
import AuthHeader from '../components/AuthHeader.jsx'
import hanokImage from '../assets/mock/hanok.jpg'
import '../styles/signup.css'
import '../styles/login.css'

const loginImages = Object.values(import.meta.glob(
  '../assets/mock/login/*.{jpg,jpeg,png,webp}',
  { eager: true, query: '?url', import: 'default' },
))

function chooseLoginImage() {
  if (loginImages.length === 0) return hanokImage

  const previousValue = sessionStorage.getItem('tripai-login-image-index')
  const previous = Number(previousValue)
  const hasPrevious = previousValue !== null && Number.isInteger(previous) && previous >= 0 && previous < loginImages.length && loginImages.length > 1
  const offset = Math.floor(Math.random() * (loginImages.length - (hasPrevious ? 1 : 0)))
  const index = hasPrevious && offset >= previous ? offset + 1 : offset

  sessionStorage.setItem('tripai-login-image-index', String(index))
  return loginImages[index]
}

const INITIAL_FORM = {
  email: '',
  password: '',
}

function validate(form) {
  const errors = {}

  if (!form.email.trim()) {
    errors.email = '이메일을 입력해 주세요.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = '올바른 이메일 형식을 입력해 주세요.'
  }

  if (!form.password) {
    errors.password = '비밀번호를 입력해 주세요.'
  }

  return errors
}

export default function LoginPage() {
  const navigate = useNavigate()
  const { isAuthenticated, login } = useAuth()
  const [form, setForm] = useState(INITIAL_FORM)
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [loginImage] = useState(chooseLoginImage)

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  const handleChange = (event) => {
    const { name, value } = event.target

    setForm((previous) => ({ ...previous, [name]: value }))
    setErrors((previous) => ({ ...previous, [name]: undefined }))
    setServerError('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    const nextErrors = validate(form)
    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors)
      return
    }

    setIsSubmitting(true)
    setServerError('')

    try {
      const loginResponse = await requestLogin(form)
      login(loginResponse)
      navigate('/', { replace: true })
    } catch (error) {
      setServerError(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="signup-page login-page">
      <AuthHeader />
      <main className="login-content auth-page-panel">
        <div className="login-layout">
          <section className="login-intro" aria-label="TripAI 소개">
            <p className="login-intro-brand">TripAI</p>
            <h1>좋아하는 순간들을<br />하나의 여행으로.</h1>
            <p>행사를 저장하고, 내 취향에 맞는<br />하루 일정을 만들어 보세요.</p>
            {loginImage && <img src={loginImage} alt="서울 문화 공간 풍경" />}
          </section>

          <section className="login-card" aria-labelledby="login-title">
            <h2 id="login-title">다시 만나 반가워요</h2>
            <p className="login-description">이메일로 로그인해 주세요.</p>

            <form className="auth-form" onSubmit={handleSubmit} noValidate>
              <div className="signup-field">
                <label htmlFor="login-email">이메일</label>
                <input
                  id="login-email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  value={form.email}
                  onChange={handleChange}
                  aria-invalid={Boolean(errors.email)}
                  aria-describedby={errors.email ? 'email-error' : undefined}
                  placeholder="이메일을 입력하세요"
                  className={errors.email ? 'input-error' : ''}
                />
                {errors.email && <p className="field-error" id="email-error">{errors.email}</p>}
              </div>

              <div className="signup-field">
                <label htmlFor="login-password">비밀번호</label>
                <input
                  id="login-password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  value={form.password}
                  onChange={handleChange}
                  aria-invalid={Boolean(errors.password)}
                  aria-describedby={errors.password ? 'password-error' : undefined}
                  placeholder="비밀번호를 입력하세요"
                  className={errors.password ? 'input-error' : ''}
                />
                {errors.password && <p className="field-error" id="password-error">{errors.password}</p>}
              </div>

              {serverError && <p className="server-error" role="alert">{serverError}</p>}

              <button className="signup-submit-button" type="submit" disabled={isSubmitting}>
                {isSubmitting ? '로그인 중...' : '로그인'}
              </button>

              <Link to="/signup" className="login-signup-link">
                처음이라면 회원가입
              </Link>
            </form>
          </section>
        </div>
      </main>
    </div>
  )
}
