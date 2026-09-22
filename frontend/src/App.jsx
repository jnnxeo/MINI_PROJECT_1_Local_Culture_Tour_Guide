import React from 'react'
import { Route, Routes } from 'react-router-dom'
import SignupPage from './pages/SignupPage.jsx'

function HomePage() {
  return (
    <main>
      <h1>TripAI</h1>
      <p>프론트엔드 개발</p>
    </main>
  )
}

function LoginPlaceholder() {
  return (
    <main>
      <h1>로그인</h1>
      <p>로그인 페이지 작업 중입니다.</p>
    </main>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/login" element={<LoginPlaceholder />} />
    </Routes>
  )
}