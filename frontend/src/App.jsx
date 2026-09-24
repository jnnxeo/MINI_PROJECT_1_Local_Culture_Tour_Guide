import React from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import Header from './components/Header.jsx'
import { useAuth } from './contexts/AuthContext.jsx'
import HomePage from './pages/HomePage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import PlanEditorPage from './pages/PlanEditorPage.jsx'
import SignupPage from './pages/SignupPage.jsx'

function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  // 로그인 후 원래 가려던 화면으로 돌아오도록 위치를 넘긴다 (UX-005)
  return isAuthenticated ? children : <Navigate to="/login" replace state={{ from: location }} />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route
        path="/trips/draft"
        element={(
          <ProtectedRoute>
            <PlanEditorPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/my-trips/:planId"
        element={(
          <ProtectedRoute>
            <PlanEditorPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/"
        element={(
          <ProtectedRoute>
            <Header />
            <HomePage />
          </ProtectedRoute>
        )}
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
