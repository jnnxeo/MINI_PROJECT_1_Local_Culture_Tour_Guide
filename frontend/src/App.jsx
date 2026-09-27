import React from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import PlanHeader from './components/plan/PlanHeader.jsx'
import FeaturePlaceholderPage from './pages/FeaturePlaceholderPage.jsx'
import { useAuth } from './contexts/AuthContext.jsx'
import HomePage from './pages/HomePage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import PlanEditorPage from './pages/PlanEditorPage.jsx'
import SignupPage from './pages/SignupPage.jsx'
import EventDetailPage from './pages/EventDetailPage.jsx'
import MyPage from './pages/MyPage.jsx'

function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth()

  return isAuthenticated ? children : <Navigate to="/login" replace />
}

function HomeNavigation() {
  return <div className="home-plan-header"><PlanHeader /></div>
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      <Route
        path="/mypage"
        element={(
          <ProtectedRoute>
            <MyPage />
          </ProtectedRoute>
        )}
      />

      <Route
        path="/trips/draft"
        element={(
          <ProtectedRoute>
            <PlanEditorPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/events/:eventId"
        element={(
          <ProtectedRoute>
            <EventDetailPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/"
        element={(
          <ProtectedRoute>
            <HomeNavigation />
            <HomePage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/events"
        element={(
          <ProtectedRoute>
            <HomeNavigation />
            <FeaturePlaceholderPage title="문화행사 검색 결과" />
          </ProtectedRoute>
        )}
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
