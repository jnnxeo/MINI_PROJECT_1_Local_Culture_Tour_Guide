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

  const logout = () => {
    // 요청 인터셉터가 실행되기 전에 토큰을 보관해 서버 요청에도 전달한다.
    const accessToken = getAccessToken()
    clearAuth()
    setIsAuthenticated(false)

    // Stateless 로그아웃은 로컬 인증 해제로 완료된다. 서버가 응답하지 않아도 화면을 막지 않는다.
    void requestLogout(accessToken).catch(() => {})
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
