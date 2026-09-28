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
import SearchResultsPage from './pages/SearchResultsPage.jsx'
import EventSavedPlanPage from './pages/EventSavedPlanPage.jsx'

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
            {/* 메인·검색 결과와 같은 상단 메뉴(로고·메인페이지·내 여행·로그아웃) */}
            <HomeNavigation />
            <MyPage />
          </ProtectedRoute>
        )}
      />

      <Route
        path="/my-trips"
        element={(
          <ProtectedRoute>
            <Navigate to="/mypage" replace />
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
        path="/my-trips/:planId"
        element={(
          <ProtectedRoute>
            <EventSavedPlanPage />
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
            <SearchResultsPage />
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
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
