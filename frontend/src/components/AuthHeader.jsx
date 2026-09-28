import React from 'react'
import { Link } from 'react-router-dom'

export default function AuthHeader() {
  return (
    <header className="signup-header">
      <Link to="/" className="signup-logo" aria-label="TripAI 메인페이지">
        <span className="signup-logo-mark" aria-hidden="true">↑</span>
        <span>TripAI</span>
      </Link>

      <nav className="signup-navigation" aria-label="주요 메뉴">
        <Link to="/">메인페이지</Link>
        <Link to="/mypage">내 여행</Link>
        <Link to="/login" className="signup-login-button">로그인</Link>
      </nav>
    </header>
  )
}
