import React, { useState } from 'react'
import KeywordModal from './KeywordModal.jsx'
import FilterModal from './FilterModal.jsx'
import DateModal from './DateModal.jsx'

const initialFilters = { categories: [], district: '전체 지역', freeOnly: false }

export default function SearchPanel({ onKeywordSearch, onFilterSearch, onRecommend }) {
  const [filters, setFilters] = useState(initialFilters)
  const [dates, setDates] = useState([])
  const [activeModal, setActiveModal] = useState(null)
  const categoryLabel = filters.categories.length ? filters.categories.join(' · ') : '전체 분야'

  const submitFilters = (event) => {
    event.preventDefault()
    onFilterSearch({ dates, ...filters })
  }

  return (
    <div className="home-search-area">
      <div className="home-keyword-search">
        <button className="home-search__field home-search__field--button home-search__field--keyword" type="button" onClick={() => setActiveModal('keyword')} aria-haspopup="dialog">
          <span>행사 검색</span>
          <strong>행사명 · 장소 · 지역 · 행사 분야로 찾기</strong>
        </button>
      </div>

      <form className="home-search" onSubmit={submitFilters} aria-label="문화행사 조건 검색">
        <button className="home-search__field home-search__field--button" type="button" onClick={() => setActiveModal('date')} aria-haspopup="dialog">
          <span>날짜 선택</span>
          <strong>{dates.length ? `${dates.length}일 선택 · ${dates[0].slice(5).replace('-', '/')}${dates.length > 1 ? ' 외' : ''}` : '갈 수 있는 날 고르기'}</strong>
        </button>

        <button className="home-search__field home-search__field--button" type="button" onClick={() => setActiveModal('category')} aria-haspopup="dialog">
          <span>행사 분야 · 요금</span>
          <strong>{categoryLabel}{filters.freeOnly ? ' · 무료만' : ''}</strong>
        </button>

        <button className="home-search__field home-search__field--button" type="button" onClick={() => setActiveModal('district')} aria-haspopup="dialog">
          <span>서울 지역</span>
          <strong>{filters.district}</strong>
        </button>

        <button className="primary-button home-search__recommend" type="button" onClick={() => onRecommend({ availableDates: dates, categories: filters.categories, district: filters.district, freeYn: filters.freeOnly })}>AI 추천받기</button>

        <button className="primary-button home-search__submit" type="submit">조건 검색</button>
      </form>
      {activeModal === 'keyword' && (
        <KeywordModal
          onClose={() => setActiveModal(null)}
          onSearch={(keyword) => { setActiveModal(null); onKeywordSearch(keyword) }}
        />
      )}
      {activeModal === 'date' && (
        <DateModal
          dates={dates}
          onConfirm={(nextDates) => { setDates(nextDates); setActiveModal(null) }}
          onClose={() => setActiveModal(null)}
        />
      )}
      {(activeModal === 'category' || activeModal === 'district') && (
        <FilterModal
          mode={activeModal}
          filters={filters}
          onConfirm={(nextFilters) => { setFilters(nextFilters); setActiveModal(null) }}
          onClose={() => setActiveModal(null)}
        />
      )}
    </div>
  )
}
