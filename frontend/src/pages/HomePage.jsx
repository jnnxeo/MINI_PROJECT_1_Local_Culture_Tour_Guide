import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import EventSection from '../features/home/components/EventSection.jsx'
import HeroSection from '../features/home/components/HeroSection.jsx'
import MonthSelector from '../features/home/components/MonthSelector.jsx'
import SearchPanel from '../features/home/components/SearchPanel.jsx'
import { getHomeEventPreviews } from '../services/eventService.js'
import { startHomeRecommendation } from '../services/homeRecommendationService.js'

export default function HomePage() {
  const navigate = useNavigate()
  const today = new Date()
  const [selectedYear, setSelectedYear] = useState(today.getFullYear())
  const [selectedMonth, setSelectedMonth] = useState(today.getMonth() + 1)
  const [favoriteIds, setFavoriteIds] = useState(new Set())
  const [eventSections, setEventSections] = useState({ all: [], exhibition: [], performance: [] })
  const [eventStatus, setEventStatus] = useState('loading')
  const [eventError, setEventError] = useState('')
  const [searchError, setSearchError] = useState('')
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

  const handleFavorite = (eventId) => {
    setFavoriteIds((current) => {
      const next = new Set(current)
      next.has(eventId) ? next.delete(eventId) : next.add(eventId)
      return next
    })
  }

  const handleMonthChange = (period) => {
    if (!/^\d{4}-\d{2}$/.test(period)) return
    const [year, month] = period.split('-').map(Number)
    if (month < 1 || month > 12) return
    setSelectedYear(year)
    setSelectedMonth(month)
  }

  const handleKeywordSearch = (keyword) => {
    navigate(`/events?${new URLSearchParams({ keyword })}`)
  }

  const handleFilterSearch = async ({ month, categories, district, freeOnly, aiRecommended }) => {
    setSearchError('')
    const params = new URLSearchParams()
    if (month) params.set('month', month)
    categories.forEach((category) => params.append('category', category))
    if (district !== '전체 지역') params.set('district', district)
    if (freeOnly) params.set('freeYn', 'true')
    const query = params.toString()
    if (aiRecommended) {
      try {
        const { draftId } = await startHomeRecommendation()
        navigate('/trips/draft', {
          state: { draftId, searchConditions: { month, categories, district, freeYn: freeOnly } },
        })
      } catch (error) {
        setSearchError(error.message || 'AI 일정을 열지 못했습니다.')
      }
      return
    }
    navigate(query ? `/events?${query}` : '/events')
  }

  const showMore = (category) => {
    const params = new URLSearchParams({ month: selectedPeriod })
    if (category) params.set('category', category)
    navigate(`/events?${params}`)
  }

  const scrollToEvents = () => {
    document.querySelector('#monthly-events')?.scrollIntoView({ behavior: 'smooth' })
  }

  return (
    <main className="home-page">
      <HeroSection onExplore={scrollToEvents} />

      <div className="home-content">
        <SearchPanel month={selectedPeriod} onMonthChange={handleMonthChange} onKeywordSearch={handleKeywordSearch} onFilterSearch={handleFilterSearch} />
        {searchError && <p className="home-search__error" role="alert">{searchError}</p>}
        <MonthSelector selectedMonth={selectedMonth} onSelect={setSelectedMonth} />

        {eventStatus === 'error' && (
          <div className="event-section__empty" role="alert">
            <strong>{eventError}</strong>
            <button type="button" onClick={() => setReloadCount((count) => count + 1)}>다시 시도</button>
          </div>
        )}
        {eventStatus === 'loading' && <p className="home-data-note" role="status">행사 목록을 불러오는 중입니다.</p>}

        {eventStatus === 'success' && <EventSection
          id="monthly-events"
          title={`${selectedYear}년 ${selectedMonth}월의 문화행사`}
          events={eventSections.all}
          favoriteIds={favoriteIds}
          onFavorite={handleFavorite}
          onDetail={(eventId) => navigate(`/events/${eventId}`)}
          featured
          hint="옆으로 넘겨보기 →"
          onMore={() => showMore()}
        />}

        {eventStatus === 'success' && <EventSection
          title={`${selectedMonth}월 전시 · 새로운 시선`}
          events={eventSections.exhibition}
          favoriteIds={favoriteIds}
          onFavorite={handleFavorite}
          onDetail={(eventId) => navigate(`/events/${eventId}`)}
          onMore={() => showMore('전시')}
        />}

        {eventStatus === 'success' && <EventSection
          title={`${selectedMonth}월 공연 · 음악이 있는 하루`}
          events={eventSections.performance}
          favoriteIds={favoriteIds}
          onFavorite={handleFavorite}
          onDetail={(eventId) => navigate(`/events/${eventId}`)}
          onMore={() => showMore('공연')}
        />}

        <p className="home-data-note">
          현재는 화면 확인을 위한 예시 행사입니다. 실제 행사 목록은 공공데이터 연동 후 제공됩니다.
        </p>
      </div>

      <footer className="home-footer">
        <span>TripAI · 문화행사에서 시작하는 서울 여행</span>
        <span>사진 영역은 행사 API 이미지로 교체됩니다.</span>
      </footer>
    </main>
  )
}
