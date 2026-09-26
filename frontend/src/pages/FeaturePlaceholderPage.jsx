import React from 'react'
import { Link } from 'react-router-dom'

export default function FeaturePlaceholderPage({ title }) {
  return (
    <main className="feature-placeholder">
      <h1>{title}</h1>
      <p>이 화면은 담당 기능 구현 후 연결됩니다.</p>
      <Link to="/">메인페이지로 돌아가기</Link>
    </main>
  )
}
