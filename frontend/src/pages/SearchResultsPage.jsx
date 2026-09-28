import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import FilterModal from '../features/home/components/FilterModal.jsx'
import DateModal from '../features/home/components/DateModal.jsx'
import SearchResultCard from '../features/search/components/SearchResultCard.jsx'
import { buildSubmittedSearchParams, readSearchConditions, SEARCH_PAGE_SIZE } from '../features/search/searchParams.js'
import { getDefaultSort, getSortOptions } from '../features/search/eventSort.js'
import { getEventSearchResults } from '../services/eventSearchService.js'
import '../features/search/search.css'
import '../styles/condition-filters.css'
import useFavoriteEvents from '../hooks/useFavoriteEvents.js'

function SortDropdown({ selected, dates, onSelect }) {
  const options = getSortOptions(dates)
  const [open, setOpen] = useState(false)
  const containerRef = useRef(null)
  const label = options.find((option) => option.value === selected)?.label || options[0].label

  useEffect(() => {
    if (!open) return undefined
    const closeOnOutsideClick = (event) => {
      if (!containerRef.current?.contains(event.target)) setOpen(false)
    }
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [open])

  return (
    <div className="search-results__sort" ref={containerRef}>
      <button type="button" className="search-results__sort-trigger" onClick={() => setOpen((current) => !current)} aria-expanded={open} aria-controls="event-sort-options">
        {label}
        <svg className="search-results__sort-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
          <path d="m6 9 6 6 6-6" />
        </svg>
      </button>
      {open && (
        <div className="search-results__sort-options" id="event-sort-options" role="group" aria-label="정렬 방식">
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              className={selected === option.value ? 'search-results__sort-option--active' : ''}
              onClick={() => { setOpen(false); onSelect(option.value) }}
              aria-pressed={selected === option.value}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

export default function SearchResultsPage() {
  const { favoriteIds, pendingIds, toggleFavorite, favoriteError } = useFavoriteEvents()
  const location = useLocation()
  const navigate = useNavigate()
  const applied = useMemo(() => readSearchConditions(new URLSearchParams(location.search)), [location.search])
  const appliedFilterKey = JSON.stringify([applied.keyword, applied.month, applied.dates, applied.categories, applied.district, applied.freeOnly])
  const [draft, setDraft] = useState(applied)
  const [activeModal, setActiveModal] = useState(null)
  const [result, setResult] = useState({ items: [], page: 0, totalCount: 0 })
  const [status, setStatus] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')
  const [retryCount, setRetryCount] = useState(0)
  const keywordSummaryRef = useRef(null)
  const [keywordSummaryHeight, setKeywordSummaryHeight] = useState(0)

  useEffect(() => {
    if (applied.keyword) setKeywordSummaryHeight(0)
  }, [applied.keyword])

  useEffect(() => { setDraft(readSearchConditions(new URLSearchParams(location.search))) }, [appliedFilterKey, location.key])

  useEffect(() => {
    let active = true
    const params = new URLSearchParams(location.search)
    params.set('size', String(SEARCH_PAGE_SIZE))
    setStatus('loading')
    getEventSearchResults(params)
      .then((data) => {
        if (!active) return
        if (applied.page > 0 && data.totalCount > 0 && data.items.length === 0) {
          const firstPage = new URLSearchParams(location.search)
          firstPage.delete('page')
          navigate({ pathname: '/events', search: firstPage.toString() }, { replace: true })
          return
        }
        setResult(data)
        setErrorMessage('')
        setStatus('success')
      })
      .catch((error) => {
        if (!active) return
        setResult({ items: [], page: 0, totalCount: 0 })
        setErrorMessage(error.message || '행사 목록을 불러오지 못했습니다.')
        setStatus('error')
      })
    return () => { active = false }
  }, [location.key, location.search, applied.page, retryCount, navigate])

  const navigateWith = (params) => navigate({ pathname: '/events', search: params.toString() })

  const clearKeyword = () => {
    const params = new URLSearchParams(location.search)
    params.delete('keyword')
    params.delete('page')
    setKeywordSummaryHeight(keywordSummaryRef.current?.getBoundingClientRect().height || 0)
    keywordSummaryRef.current?.focus({ preventScroll: true })
    navigate({ pathname: '/events', search: params.toString() }, { replace: true, preventScrollReset: true })
  }

  const submitSearch = (event) => {
    event.preventDefault()
    const params = buildSubmittedSearchParams(draft, applied)
    if (params.toString() === new URLSearchParams(location.search).toString()) {
      setRetryCount((count) => count + 1)
    } else {
      navigateWith(params)
    }
  }

  const selectSort = (sort) => {
    const params = new URLSearchParams(location.search)
    if (sort === getDefaultSort(applied.dates)) params.delete('sort')
    else params.set('sort', sort)
    params.delete('page')
    navigateWith(params)
  }

  const selectPage = (page) => {
    const params = new URLSearchParams(location.search)
    if (page === 0) params.delete('page')
    else params.set('page', String(page))
    navigateWith(params)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const totalPages = Math.ceil(result.totalCount / SEARCH_PAGE_SIZE)
  const firstVisiblePage = Math.max(0, Math.min(applied.page - 2, totalPages - 5))
  const visiblePages = Array.from({ length: Math.min(totalPages, 5) }, (_, offset) => firstVisiblePage + offset)
  const filterSummary = `${draft.categories.length ? draft.categories.join(' · ') : '전체 분야'}${draft.freeOnly ? ' · 무료만' : ''}`

  return (
    <main className="search-results-page">
      <header className="search-results-hero">
        <div className="search-results-hero__inner">
          <p>문화행사 찾기</p>
          <h1>어떤 하루를 보내고 싶으세요?</h1>
          <p>키워드와 날짜, 행사 분야로 나에게 맞는 행사를 찾아보세요.</p>
        </div>
      </header>

      <div className="search-results-content">
        <div className="search-results-keyword" ref={keywordSummaryRef} tabIndex={-1} style={keywordSummaryHeight ? { minHeight: keywordSummaryHeight } : undefined}>
          <p aria-live="polite">검색어: <strong>{applied.keyword || '없음'}</strong></p>
          <button type="button" onClick={clearKeyword} disabled={!applied.keyword} className={!applied.keyword ? 'search-results-keyword__clear--hidden' : undefined}>검색어 지우기</button>
        </div>
        <form className="search-results-filters event-condition-filters" onSubmit={submitSearch} aria-label="문화행사 검색 조건">
          <div className="search-results-filters__month">
            <button type="button" className="home-search__field home-search__field--button" onClick={() => setActiveModal('date')} aria-haspopup="dialog">
              <span>날짜 선택</span>
              <strong>{draft.dates.length ? `${draft.dates.length}일 선택 · ${draft.dates.slice(0, 2).join(', ')}${draft.dates.length > 2 ? ' 외' : ''}` : (draft.month || '전체 기간')}</strong>
            </button>
            {(draft.month || draft.dates.length > 0) && <button type="button" className="search-results-filters__clear" onClick={() => setDraft((current) => ({ ...current, month: '', dates: [] }))} aria-label="날짜 조건 해제">×</button>}
          </div>
          <button type="button" className="home-search__field home-search__field--button" onClick={() => setActiveModal('category')} aria-haspopup="dialog">
            <span>행사 분야 · 요금</span>
            <strong>{filterSummary}</strong>
          </button>
          <button type="button" className="home-search__field home-search__field--button" onClick={() => setActiveModal('district')} aria-haspopup="dialog">
            <span>서울 지역</span>
            <strong>{draft.district}</strong>
          </button>
          <button type="submit" className="primary-button search-results-filters__submit" disabled={status === 'loading'}>조건 검색</button>
        </form>
        <button
          className="search-results-filters__reset"
          type="button"
          onClick={() => setDraft((current) => ({ ...current, month: '', dates: [], categories: [], district: '전체 지역', freeOnly: false }))}
        >
          조건 초기화
        </button>

        <div className="search-results-toolbar">
          <h2>{status === 'success' ? (result.totalCount ? `검색 결과 ${result.totalCount}건` : '검색 결과가 없어요') : '검색 결과'}</h2>
          <SortDropdown selected={applied.sort} dates={applied.dates} onSelect={selectSort} />
        </div>

        <p className="home-visually-hidden" role="status">{status === 'loading' ? '행사 검색 결과를 불러오는 중입니다.' : ''}</p>
        {status === 'loading' && <div className="search-results-state" aria-hidden="true"><div className="search-results-skeleton" /><div className="search-results-skeleton" /><div className="search-results-skeleton" /></div>}
        {status === 'error' && (
          <div className="search-results-state" role="alert">
            <h3>행사 목록을 불러오지 못했습니다.</h3>
            <p>{errorMessage}</p>
            <button type="button" className="primary-button search-results-state__button" onClick={() => setRetryCount((count) => count + 1)}>다시 시도</button>
          </div>
        )}
        {status === 'success' && result.totalCount === 0 && (
          <div className="search-results-state">
            <p>조건에 맞는 행사가 없어요. 다른 검색어나 조건으로 다시 시도해 주세요.</p>
            <button type="button" className="primary-button search-results-state__button" onClick={() => navigate('/events')}>전체 행사 보기</button>
          </div>
        )}
        {status === 'success' && result.items.length > 0 && (
          <>
            {favoriteError && <p className="search-results-favorite-error" role="alert">{favoriteError}</p>}
            <div className="search-results-list">
              {result.items.map((event) => (
                <SearchResultCard
                  key={event.eventId}
                  event={event}
                  isFavorite={favoriteIds.has(event.eventId)}
                  favoritePending={pendingIds.has(event.eventId)}
                  onFavorite={() => toggleFavorite(event.eventId)}
                  onDetail={() => navigate(`/events/${event.eventId}`)}
                />
              ))}
            </div>
            {totalPages > 1 && (
              <nav className="search-results-pagination" aria-label="검색 결과 페이지">
                <button type="button" onClick={() => selectPage(applied.page - 1)} disabled={applied.page === 0}>이전</button>
                {visiblePages.map((page) => (
                  <button key={page} type="button" className={applied.page === page ? 'search-results-pagination__active' : ''} onClick={() => selectPage(page)} aria-current={applied.page === page ? 'page' : undefined}>{page + 1}</button>
                ))}
                <button type="button" onClick={() => selectPage(applied.page + 1)} disabled={applied.page >= totalPages - 1}>다음</button>
              </nav>
            )}
          </>
        )}
      </div>

      <footer className="home-footer">
        <span>TripAI · 문화행사에서 시작하는 서울 여행</span>
        <span>행사 정보는 방문 전 공식 안내를 확인해 주세요.</span>
      </footer>

      {activeModal === 'date' && (
        <DateModal
          dates={draft.dates}
          onConfirm={(dates) => { setDraft((current) => ({ ...current, month: '', dates })); setActiveModal(null) }}
          onClose={() => setActiveModal(null)}
        />
      )}
      {(activeModal === 'category' || activeModal === 'district') && (
        <FilterModal
          mode={activeModal}
          filters={draft}
          onConfirm={(filters) => { setDraft((current) => ({ ...current, ...filters })); setActiveModal(null) }}
          onClose={() => setActiveModal(null)}
        />
      )}
    </main>
  )
}
