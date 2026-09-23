import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import {
  clearAuth,
  getAccessToken,
  requestLogout,
  saveAuth,
} from '../services/authService.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getAccessToken()))

  useEffect(() => {
    const handleAuthExpired = () => setIsAuthenticated(false)

    window.addEventListener('tripai:auth-expired', handleAuthExpired)
    return () => window.removeEventListener('tripai:auth-expired', handleAuthExpired)
  }, [])

  const login = (loginResponse) => {
    saveAuth(loginResponse)
    setIsAuthenticated(true)
  }

  const logout = async () => {
    clearAuth()
    setIsAuthenticated(false)

    try {
      await requestLogout()
    } catch {
      // JWT Stateless 로그아웃은 로컬 토큰 삭제만으로 완료된다.
    }
  }

  const value = useMemo(() => ({ isAuthenticated, login, logout }), [isAuthenticated])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth는 AuthProvider 안에서 사용해야 합니다.')
  }

  return context
}
