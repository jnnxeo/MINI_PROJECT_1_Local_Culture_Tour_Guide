import React from 'react'
import { useNavigate } from 'react-router-dom'
import symbolImage from '../../assets/brand/tripai-symbol.svg'
import { useAuth } from '../../contexts/AuthContext.jsx'
import '../../styles/common.css'

/**
 * Figma Navigation — 로고 · 메인페이지 · 내 여행 · 로그인/로그아웃
 * onNavigate: 이동 전에 확인이 필요한 화면(편집 중 이탈 방지)에서 가로챌 때 사용
 */
export default function SiteHeader({ onNavigate }) {
  const navigate = useNavigate()
  const { isAuthenticated, logout } = useAuth()

  const go = (path) => {
    if (onNavigate) {
      onNavigate(path)
      return
    }

    navigate(path)
  }

  const handleAuthClick = async () => {
    if (!isAuthenticated) {
      go('/login')
      return
    }

    const leave = async () => {
      await logout()
      navigate('/login', { replace: true })
    }

    if (onNavigate) {
      onNavigate('/login', leave)
      return
    }

    await leave()
  }

  return (
    <header className="tp-nav">
      <button className="tp-nav__brand" type="button" onClick={() => go('/')}>
        <img className="tp-nav__symbol" src={symbolImage} alt="" />
        TripAI
      </button>
      <span className="tp-nav__spacer" />
      <button className="tp-btn tp-nav__link" type="button" onClick={() => go('/')}>
        메인페이지
      </button>
      <button className="tp-btn tp-nav__link" type="button" onClick={() => go('/mypage')}>
        내 여행
      </button>
      <button className="tp-btn tp-btn--secondary tp-nav__auth" type="button" onClick={handleAuthClick}>
        {isAuthenticated ? '로그아웃' : '로그인'}
      </button>
    </header>
  )
}
