import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../services/authService'
import '../styles/signup.css'

const initialForm = {
  email: '',
  password: '',
  passwordConfirm: '',
  agreedToTerms: false,
}

const initialErrors = {
  email: '',
  password: '',
  passwordConfirm: '',
  agreedToTerms: '',
  submit: '',
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/

export default function SignupPage() {
  const navigate = useNavigate()

  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState(initialErrors)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isSuccessModalOpen, setIsSuccessModalOpen] = useState(false)

  const validateForm = () => {
    const nextErrors = {
      email: '',
      password: '',
      passwordConfirm: '',
      agreedToTerms: '',
      submit: '',
    }

    if (!form.email.trim()) {
      nextErrors.email = '이메일을 입력해주세요.'
    } else if (!emailPattern.test(form.email.trim())) {
      nextErrors.email = '올바른 이메일 형식을 입력해주세요.'
    }

    if (!form.password) {
      nextErrors.password = '비밀번호를 입력해주세요.'
    } else if (!passwordPattern.test(form.password)) {
      nextErrors.password =
        '비밀번호는 영문과 숫자를 포함해 8자 이상 입력해주세요.'
    }

    if (!form.passwordConfirm) {
      nextErrors.passwordConfirm = '비밀번호를 다시 입력해주세요.'
    } else if (form.password !== form.passwordConfirm) {
      nextErrors.passwordConfirm = '비밀번호가 일치하지 않습니다.'
    }

    if (!form.agreedToTerms) {
      nextErrors.agreedToTerms = '필수 약관에 동의해주세요.'
    }

    setErrors(nextErrors)

    return !Object.values(nextErrors).some(Boolean)
  }

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target

    setForm((previousForm) => ({
      ...previousForm,
      [name]: type === 'checkbox' ? checked : value,
    }))

    setErrors((previousErrors) => ({
      ...previousErrors,
      [name]: '',
      submit: '',
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!validateForm() || isSubmitting) {
      return
    }

    setIsSubmitting(true)

    try {
      await signup({
        email: form.email.trim(),
        password: form.password,
      })

      setIsSuccessModalOpen(true)
    } catch (error) {
      if (error.status === 409) {
        setErrors((previousErrors) => ({
          ...previousErrors,
          email: error.message,
        }))
      } else {
        setErrors((previousErrors) => ({
          ...previousErrors,
          submit: error.message,
        }))
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const moveToLogin = () => {
    setIsSuccessModalOpen(false)
    navigate('/login')
  }

  return (
    <div className="signup-page">
      <header className="signup-header">
        <Link to="/" className="signup-logo">
          <span className="signup-logo-mark" aria-hidden="true">
            ↑
          </span>
          <span>TripAI</span>
        </Link>

        <nav className="signup-navigation" aria-label="주요 메뉴">
          <Link to="/">메인페이지</Link>
          <Link to="/my-trips">내 여행</Link>
          <Link to="/login" className="signup-login-button">
            로그인
          </Link>
        </nav>
      </header>

      <section className="signup-hero">
        <p className="signup-eyebrow">JOIN TRIPAI</p>
        <h1>나만의 여행을 시작하세요</h1>
        <p>계정을 만들면 내 여행과 일정을 저장할 수 있어요.</p>
      </section>

      <main className="signup-content">
        <form
          className="signup-form"
          onSubmit={handleSubmit}
          noValidate
        >
          <div className="signup-field">
            <label htmlFor="email">이메일</label>

            <input
              id="email"
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              placeholder="이메일을 입력하세요"
              autoComplete="email"
              aria-invalid={Boolean(errors.email)}
              aria-describedby={
                errors.email ? 'email-error' : undefined
              }
              className={errors.email ? 'input-error' : ''}
            />

            {errors.email && (
              <p id="email-error" className="signup-error">
                {errors.email}
              </p>
            )}
          </div>

          <div className="signup-field">
            <label htmlFor="password">비밀번호</label>

            <input
              id="password"
              name="password"
              type="password"
              value={form.password}
              onChange={handleChange}
              placeholder="영문과 숫자를 포함해 8자 이상 입력하세요"
              autoComplete="new-password"
              aria-invalid={Boolean(errors.password)}
              aria-describedby={
                errors.password ? 'password-error' : undefined
              }
              className={errors.password ? 'input-error' : ''}
            />

            {errors.password && (
              <p id="password-error" className="signup-error">
                {errors.password}
              </p>
            )}
          </div>

          <div className="signup-field">
            <label htmlFor="passwordConfirm">비밀번호 확인</label>

            <input
              id="passwordConfirm"
              name="passwordConfirm"
              type="password"
              value={form.passwordConfirm}
              onChange={handleChange}
              placeholder="비밀번호를 다시 입력하세요"
              autoComplete="new-password"
              aria-invalid={Boolean(errors.passwordConfirm)}
              aria-describedby={
                errors.passwordConfirm
                  ? 'password-confirm-error'
                  : undefined
              }
              className={
                errors.passwordConfirm ? 'input-error' : ''
              }
            />

            {errors.passwordConfirm && (
              <p
                id="password-confirm-error"
                className="signup-error"
              >
                {errors.passwordConfirm}
              </p>
            )}
          </div>

          <div className="signup-terms-wrapper">
            <label className="signup-terms">
              <input
                name="agreedToTerms"
                type="checkbox"
                checked={form.agreedToTerms}
                onChange={handleChange}
              />

              <span>필수 약관에 동의합니다</span>
            </label>

            {errors.agreedToTerms && (
              <p className="signup-error signup-terms-error">
                {errors.agreedToTerms}
              </p>
            )}
          </div>

          {errors.submit && (
            <p className="signup-submit-error" role="alert">
              {errors.submit}
            </p>
          )}

          <button
            type="submit"
            className="signup-submit-button"
            disabled={isSubmitting}
          >
            {isSubmitting ? '가입 처리 중...' : '회원가입'}
          </button>

          <Link to="/login" className="signup-login-link">
            이미 계정이 있으신가요? 로그인
          </Link>
        </form>
      </main>

      {isSuccessModalOpen && (
        <div
          className="signup-modal-backdrop"
          role="presentation"
          onMouseDown={moveToLogin}
        >
          <section
            className="signup-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="signup-modal-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <button
              type="button"
              className="signup-modal-close"
              onClick={moveToLogin}
              aria-label="팝업 닫기"
            >
              ×
            </button>

            <h2 id="signup-modal-title">가입을 환영해요</h2>
            <p>이제 나만의 여행을 저장할 수 있어요.</p>

            <button
              type="button"
              className="signup-modal-action"
              onClick={moveToLogin}
            >
              로그인하러 가기
            </button>
          </section>
        </div>
      )}
    </div>
  )
}