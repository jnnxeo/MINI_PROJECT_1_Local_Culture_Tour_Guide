import React, { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import EventSection from '../features/home/components/EventSection.jsx'
import HeroSection from '../features/home/components/HeroSection.jsx'
import MonthSelector from '../features/home/components/MonthSelector.jsx'
import SearchPanel from '../features/home/components/SearchPanel.jsx'
import { getHomeEventPreviews } from '../services/eventService.js'
import { recommendDraft } from '../services/planService.js'
import ConditionsModal from '../components/plan/ConditionsModal.jsx'
import useFavoriteEvents from '../hooks/useFavoriteEvents.js'
import '../styles/plan.css'

export default function HomePage() {
  const navigate = useNavigate()
  const location = useLocation()
  const guide = location.state?.guide === 'favorite' || location.state?.guide === 'plan' ? location.state.guide : null
  const today = new Date()
  const [selectedYear] = useState(today.getFullYear())
  const [selectedMonth, setSelectedMonth] = useState(today.getMonth() + 1)
  // 하트는 서버 관심 행사와 연결 — 내 여행 > 관심 행사에 그대로 나온다
  const { favoriteIds, toggleFavorite, favoriteError } = useFavoriteEvents()
  const [eventSections, setEventSections] = useState({ all: [], exhibition: [], performance: [] })
  const [eventStatus, setEventStatus] = useState('loading')
  const [eventError, setEventError] = useState('')
  const [searchError, setSearchError] = useState('')
  const [recommendConditions, setRecommendConditions] = useState(null)
  const [recommending, setRecommending] = useState(false)
  const recommendationPending = useRef(false)
  const [reloadCount, setReloadCount] = useState(0)
  const selectedPeriod = `${selectedYear}-${String(selectedMonth).padStart(2, '0')}`

  useEffect(() => {
    let active = true
    setEventStatus('loading')
    getHomeEventPreviews(selectedPeriod)
      .then((sections) => {
        if (!active) return
        setEventSections(sections)
        setEventStatus('success')
        setEventError('')
      })
      .catch((error) => {
        if (!active) return
        setEventSections({ all: [], exhibition: [], performance: [] })
        setEventStatus('error')
        setEventError(error.message || '행사 목록을 불러오지 못했습니다.')
      })
    return () => { active = false }
  }, [selectedPeriod, reloadCount])

  useEffect(() => {
    if (!guide) return undefined
    const target = guide === 'plan' ? 'home-plan-start' : 'monthly-events'
    const frame = requestAnimationFrame(() => document.getElementById(target)?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
    return () => cancelAnimationFrame(frame)
  }, [guide, location.key])

  const handleFavorite = (eventId) => toggleFavorite(eventId)

  const handleMonthSelect = (month) => {
    if (month === selectedMonth) return
    setEventStatus('loading')
    setSelectedMonth(month)
  }

  const handleKeywordSearch = (keyword) => {
    navigate(`/events?${new URLSearchParams({ keyword })}`)
  }

  const handleFilterSearch = ({ dates, categories, district, freeOnly }) => {
    const params = new URLSearchParams()
    dates.forEach((date) => params.append('date', date))
    categories.forEach((category) => params.append('category', category))
    if (district !== '전체 지역') params.set('district', district)
    if (freeOnly) params.set('freeYn', 'true')
    navigate(`/events?${params}`)
  }

  const createRecommendation = async (conditions) => {
    if (recommendationPending.current) return
    recommendationPending.current = true
    setRecommending(true)
    setSearchError('')
    try {
      const { draftId } = await recommendDraft(conditions)
      navigate('/trips/draft', { state: { draftId } })
    } catch (error) {
      setSearchError(error.message || '일정을 만들지 못했습니다. 조건을 바꿔 다시 시도해 주세요.')
    } finally {
      recommendationPending.current = false
      setRecommending(false)
    }
  }

  const showMore = (category) => {
    const params = new URLSearchParams({ month: selectedPeriod })
    if (category) params.set('category', category)
    navigate(`/events?${params}`)
  }

  const scrollToEvents = () => {
    document.querySelector('#monthly-events')?.scrollIntoView({ behavior: 'smooth' })
  }

  const retryEvents = () => {
    setEventStatus('loading')
    setReloadCount((count) => count + 1)
  }

  return (
    <main className="home-page">
      <HeroSection onExplore={scrollToEvents} />

      <div className="home-content">
        {guide === 'plan' && <p className="home-return-guide" role="status" id="home-plan-start">행사를 골라 일정을 만들고 저장해 보세요. 날짜를 고른 뒤 AI 추천받기를 누르면 시작할 수 있습니다.</p>}
        <SearchPanel onKeywordSearch={handleKeywordSearch} onFilterSearch={handleFilterSearch} onRecommend={(conditions) => { setSearchError(''); setRecommendConditions(conditions) }} />
        {recommendConditions && <ConditionsModal
          mode="create" conditions={recommendConditions} busy={recommending} error={searchError}
          onSubmit={createRecommendation}
          onClose={() => { if (!recommendationPending.current) setRecommendConditions(null) }}
        />}
        <MonthSelector selectedMonth={selectedMonth} onSelect={handleMonthSelect} />

        <p className="home-visually-hidden" role="status">
          {eventStatus === 'loading' ? '행사 목록을 불러오는 중입니다.' : ''}
        </p>

        {favoriteError && <p className="home-search__error" role="alert">{favoriteError}</p>}
        {guide === 'favorite' && <p className="home-return-guide" role="status">마음에 드는 행사에 하트를 눌러 저장해 보세요. 저장한 행사는 내 여행의 관심 행사에서 볼 수 있습니다.</p>}
        <EventSection
          id="monthly-events"
          title={`${selectedYear}년 ${selectedMonth}월의 문화행사`}
          events={eventSections.all}
          loading={eventStatus === 'loading'}
          error={eventStatus === 'error'}
          errorMessage={eventError}
          onRetry={retryEvents}
          favoriteIds={favoriteIds}
          onFavorite={handleFavorite}
          onDetail={(eventId) => navigate(`/events/${eventId}`)}
          featured
          hint="옆으로 넘겨보기 →"
          onMore={() => showMore()}
        />

        <EventSection
          title="전시 · 새로운 시선"
          events={eventSections.exhibition}
          loading={eventStatus === 'loading'}
          error={eventStatus === 'error'}
          favoriteIds={favoriteIds}
          onFavorite={handleFavorite}
          onDetail={(eventId) => navigate(`/events/${eventId}`)}
          onMore={() => showMore('전시')}
        />

        <EventSection
          title="공연 · 음악이 있는 하루"
          events={eventSections.performance}
          loading={eventStatus === 'loading'}
          error={eventStatus === 'error'}
          favoriteIds={favoriteIds}
          onFavorite={handleFavorite}
          onDetail={(eventId) => navigate(`/events/${eventId}`)}
          onMore={() => showMore('공연')}
        />
      </div>

      <footer className="home-footer">
        <span>TripAI · 문화행사에서 시작하는 서울 여행</span>
        <span>서울의 다양한 문화행사를 둘러보세요.</span>
      </footer>
    </main>
  )
}
