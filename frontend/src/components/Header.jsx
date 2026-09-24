import React from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext.jsx'

export default function Header() {
  const navigate = useNavigate()
  const { logout } = useAuth()

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="app-header">
      <button className="brand-button" type="button" onClick={() => navigate('/')}>
        TripAI
      </button>
      <button className="logout-button" type="button" onClick={handleLogout}>
        로그아웃
      </button>
    </header>
  )
}
