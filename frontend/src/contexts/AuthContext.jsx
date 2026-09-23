import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import {
  clearAuth,
  getAuthExpirationDelay,
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

  useEffect(() => {
    if (!isAuthenticated) {
      return undefined
    }

    const timeoutId = window.setTimeout(() => {
      clearAuth()
      setIsAuthenticated(false)
    }, getAuthExpirationDelay())

    return () => window.clearTimeout(timeoutId)
  }, [isAuthenticated])

  const login = (loginResponse) => {
    saveAuth(loginResponse)
    setIsAuthenticated(true)
  }

  const logout = async () => {
    try {
      await requestLogout()
    } catch {
      // 서버 요청 실패와 관계없이 로컬 인증 정보는 제거한다.
    } finally {
      clearAuth()
      setIsAuthenticated(false)
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
