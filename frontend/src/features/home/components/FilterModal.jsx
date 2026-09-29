import React, { useState } from 'react'
import HomeModal from './HomeModal.jsx'
import './filter-modal.css'

import { EVENT_CATEGORIES as categories, SEOUL_DISTRICTS as districts } from '../../../constants/eventFilters.js'

export default function FilterModal({ mode, filters, onConfirm, onClose }) {
  const [draft, setDraft] = useState(() => ({ ...filters, categories: [...filters.categories] }))
  const isCategory = mode === 'category'

  const toggleCategory = (category) => {
    setDraft((current) => ({
      ...current,
      categories: current.categories.includes(category)
        ? current.categories.filter((item) => item !== category)
        : [...current.categories, category],
    }))
  }

  const reset = () => setDraft((current) => isCategory
    ? { ...current, categories: [], freeOnly: false }
    : { ...current, district: '전체 지역' })

  return (
    <HomeModal title={isCategory ? '행사 분야 선택' : '서울 지역 선택'} titleId="filter-modal-title" className="filter-modal" onClose={onClose}>
      {isCategory ? (
        <>
          <p className="home-modal__description">분야는 여러 개 선택할 수 있어요. 미선택 시 전체 분야를 조회합니다.</p>
          <div className="filter-modal__section">
            <h3 className="home-modal__section-title">행사 분야</h3>
            <div className="filter-modal__choices">
              <button type="button" className={`filter-modal__choice${draft.categories.length === 0 ? ' filter-modal__choice--selected' : ''}`} onClick={() => setDraft((current) => ({ ...current, categories: [] }))} aria-pressed={draft.categories.length === 0}>전체</button>
              {categories.map((category) => (
                <button type="button" key={category} className={`filter-modal__choice${draft.categories.includes(category) ? ' filter-modal__choice--selected' : ''}`} onClick={() => toggleCategory(category)} aria-pressed={draft.categories.includes(category)}>{category}</button>
              ))}
            </div>
          </div>
          <div className="filter-modal__section">
            <h3 className="home-modal__section-title">요금</h3>
            <button type="button" className={`filter-modal__free${draft.freeOnly ? ' filter-modal__free--selected' : ''}`} onClick={() => setDraft((current) => ({ ...current, freeOnly: !current.freeOnly }))} aria-pressed={draft.freeOnly}>
              <span className="filter-modal__free-copy"><strong>무료 행사만 보기</strong><small>{draft.freeOnly ? '무료 행사만 검색해요' : '무료·유료 행사를 모두 검색해요'}</small></span>
              <span className="filter-modal__check" aria-hidden="true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="m5 12 4 4L19 6" /></svg></span>
            </button>
          </div>
          <div className="filter-modal__summary filter-modal__summary--details" aria-live="polite">
            <div className="filter-modal__summary-row"><span>행사 분야:</span><strong>{draft.categories.length ? draft.categories.join(' · ') : '전체 분야'}</strong></div>
            <div className="filter-modal__summary-row"><span>요금:</span><strong>{draft.freeOnly ? '무료만' : '전체 요금'}</strong></div>
          </div>
        </>
      ) : (
        <>
          <p className="home-modal__description">행사를 찾을 서울 지역을 한 곳 선택하세요. 전체 지역도 선택할 수 있어요.</p>
          <div className="filter-modal__section">
            <h3 className="home-modal__section-title">서울 지역</h3>
            <div className="filter-modal__choices filter-modal__choices--districts">
              {['전체 지역', ...districts].map((district) => (
                <button type="button" key={district} className={`filter-modal__choice${draft.district === district ? ' filter-modal__choice--selected' : ''}`} onClick={() => setDraft((current) => ({ ...current, district }))} aria-pressed={draft.district === district}>{district}</button>
              ))}
            </div>
          </div>
          <div className="filter-modal__summary filter-modal__summary--details" aria-live="polite">
            <div className="filter-modal__summary-row"><span>서울 지역:</span><strong>{draft.district}</strong></div>
          </div>
        </>
      )}
      <div className="filter-modal__actions">
        <button className="filter-modal__reset" type="button" onClick={reset}>초기화</button>
        <button className="primary-button" type="button" onClick={() => onConfirm(draft)}>선택 적용</button>
      </div>
    </HomeModal>
  )
}
