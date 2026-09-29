import React, { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import PlanHeader from '../../../components/plan/PlanHeader.jsx'
import KeywordModal from './KeywordModal.jsx'

/**
 * 공통 상단 메뉴 + 헤더 키워드 검색.
 * onNavigate: 편집 중 이탈 확인이 필요한 화면(나의 일정·저장 일정)에서 메뉴 이동과 검색 이동을 가로챌 때 사용
 */
export default function HomeNavigation({ enableSearch = false, onNavigate }) {
  const location = useLocation()
  const navigate = useNavigate()
  const [searchOpen, setSearchOpen] = useState(false)

  useEffect(() => { setSearchOpen(false) }, [location.key])

  const search = (keyword) => {
    setSearchOpen(false)
    const target = { pathname: '/events', search: new URLSearchParams({ keyword }).toString() }
    if (onNavigate) {
      onNavigate(`${target.pathname}?${target.search}`)
      return
    }
    navigate(target)
  }

  return (
    <>
      <div className="home-plan-header">
        <PlanHeader onNavigate={onNavigate} onSearch={enableSearch ? () => setSearchOpen(true) : undefined} searchOpen={searchOpen} />
      </div>
      {enableSearch && searchOpen && (
        <KeywordModal
          initialKeyword={new URLSearchParams(location.search).get('keyword') || ''}
          onClose={() => setSearchOpen(false)}
          onSearch={search}
        />
      )}
    </>
  )
}
