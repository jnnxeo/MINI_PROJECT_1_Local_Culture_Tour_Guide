import React from 'react'
import { useNavigate } from 'react-router-dom'
import symbolImage from '../../assets/plan/tripai-symbol.svg'
import { useAuth } from '../../contexts/AuthContext.jsx'
import '../../styles/plan-base.css'

/**
 * 나의 일정 화면 전용 상단 메뉴 (Figma Navigation 모양)
 * 공통 헤더·내비게이션은 WBS-101(1조) 담당 — 공통 헤더가 나오면 이 컴포넌트를 교체한다.
 * 이탈 방지(UX-004)를 위해 onNavigate로 이동을 가로챌 수 있게 했다.
 */
export default function PlanHeader({ onNavigate }) {
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
      <button className="tp-btn tp-nav__link" type="button" onClick={() => go('/my-trips')}>
        내 여행
      </button>
      <button className="tp-btn tp-btn--secondary tp-nav__auth" type="button" onClick={handleAuthClick}>
        {isAuthenticated ? '로그아웃' : '로그인'}
      </button>
    </header>
  )
}
