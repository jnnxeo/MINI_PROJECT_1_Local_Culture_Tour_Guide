import React, { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import PlanHeader from '../../../components/plan/PlanHeader.jsx'
import KeywordModal from './KeywordModal.jsx'

export default function HomeNavigation({ enableSearch = false }) {
  const location = useLocation()
  const navigate = useNavigate()
  const [searchOpen, setSearchOpen] = useState(false)

  useEffect(() => { setSearchOpen(false) }, [location.key])

  const search = (keyword) => {
    setSearchOpen(false)
    navigate({ pathname: '/events', search: new URLSearchParams({ keyword }).toString() })
  }

  return (
    <>
      <div className="home-plan-header">
        <PlanHeader onSearch={enableSearch ? () => setSearchOpen(true) : undefined} searchOpen={searchOpen} />
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
