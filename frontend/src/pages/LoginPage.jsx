import React, { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext.jsx'
import { getErrorMessage, requestLogin } from '../services/authService.js'

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
  const location = useLocation()
  const { isAuthenticated, login } = useAuth()
  const from = location.state?.from
  const redirectTo = from && from.pathname !== '/login'
    ? `${from.pathname}${from.search ?? ''}${from.hash ?? ''}`
    : '/'
  const [form, setForm] = useState(INITIAL_FORM)
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isAuthenticated) {
    return <Navigate to={redirectTo} replace />
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
      navigate(redirectTo, { replace: true })
    } catch (error) {
      setServerError(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="login-title">
        <div className="auth-brand">TripAI</div>
        <h1 id="login-title">로그인</h1>
        <p className="auth-description">문화행사와 여행 일정을 더 편하게 관리해 보세요.</p>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <label htmlFor="email">이메일</label>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={handleChange}
            aria-invalid={Boolean(errors.email)}
            aria-describedby={errors.email ? 'email-error' : undefined}
            placeholder="example@tripai.com"
            className={errors.email ? 'auth-input input-error' : form.email ? 'auth-input input-filled' : 'auth-input'}
          />
          {errors.email && <p className="field-error" id="email-error">{errors.email}</p>}

          <label htmlFor="password">비밀번호</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={form.password}
            onChange={handleChange}
            aria-invalid={Boolean(errors.password)}
            aria-describedby={errors.password ? 'password-error' : undefined}
            placeholder="비밀번호를 입력해 주세요"
            className={errors.password ? 'auth-input input-error' : form.password ? 'auth-input input-filled' : 'auth-input'}
          />
          {errors.password && <p className="field-error" id="password-error">{errors.password}</p>}

          {serverError && <p className="server-error" role="alert">{serverError}</p>}

          <button className="login-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? '로그인 중...' : '로그인'}
          </button>

          <Link to="/signup" className="auth-signup-link">
            아직 계정이 없으신가요? 회원가입
          </Link>
        </form>
      </section>
    </main>
  )
}
