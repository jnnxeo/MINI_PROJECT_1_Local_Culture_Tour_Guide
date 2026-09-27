import React, { useState } from 'react'
import KeywordModal from './KeywordModal.jsx'
import FilterModal from './FilterModal.jsx'
import MonthModal from './MonthModal.jsx'

const initialFilters = { categories: [], district: '전체 지역', freeOnly: false }

export default function SearchPanel({ month, onMonthChange, onKeywordSearch, onFilterSearch }) {
  const [filters, setFilters] = useState(initialFilters)
  const [aiRecommended, setAiRecommended] = useState(false)
  const [activeModal, setActiveModal] = useState(null)
  const categoryLabel = filters.categories.length ? filters.categories.join(' · ') : '전체 분야'
  const [yearValue, monthValue] = month.split('-').map(Number)

  const submitFilters = (event) => {
    event.preventDefault()
    onFilterSearch({ month, ...filters, aiRecommended })
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
        <button className="home-search__field home-search__field--button" type="button" onClick={() => setActiveModal('month')} aria-haspopup="dialog">
          <span>월별 탐색</span>
          <strong>{yearValue}년 {monthValue}월</strong>
        </button>

        <button className="home-search__field home-search__field--button" type="button" onClick={() => setActiveModal('category')} aria-haspopup="dialog">
          <span>행사 분야 · 요금</span>
          <strong>{categoryLabel}{filters.freeOnly ? ' · 무료만' : ''}</strong>
        </button>

        <button className="home-search__field home-search__field--button" type="button" onClick={() => setActiveModal('district')} aria-haspopup="dialog">
          <span>서울 지역</span>
          <strong>{filters.district}</strong>
        </button>

        <label className={`ai-toggle${aiRecommended ? ' ai-toggle--checked' : ''}`}>
          <input type="checkbox" checked={aiRecommended} onChange={(event) => setAiRecommended(event.target.checked)} />
          <span>AI 추천받기</span>
        </label>

        <button className="primary-button home-search__submit" type="submit">조건 검색</button>
      </form>
      {activeModal === 'keyword' && (
        <KeywordModal
          onClose={() => setActiveModal(null)}
          onSearch={(keyword) => { setActiveModal(null); onKeywordSearch(keyword) }}
        />
      )}
      {activeModal === 'month' && (
        <MonthModal
          month={month}
          onConfirm={(nextMonth) => { onMonthChange(nextMonth); setActiveModal(null) }}
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
