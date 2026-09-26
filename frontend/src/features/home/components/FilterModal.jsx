import React, { useState } from 'react'
import HomeModal from './HomeModal.jsx'

const categories = ['공연', '전시', '전통문화', '축제', '교육·체험']
const districts = ['종로구', '중구', '성동구', '광진구', '동대문구', '중랑구', '성북구', '강북구', '도봉구', '노원구', '은평구', '서대문구', '마포구', '양천구', '강서구', '구로구', '금천구', '영등포구', '동작구', '관악구', '서초구', '강남구', '송파구', '강동구', '용산구']

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
    <HomeModal title={isCategory ? '행사 분야 선택' : '서울 지역 선택'} titleId="filter-modal-title" onClose={onClose}>
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
            <button type="button" className={`filter-modal__free${draft.freeOnly ? ' filter-modal__free--selected' : ''}`} onClick={() => setDraft((current) => ({ ...current, freeOnly: !current.freeOnly }))} aria-pressed={draft.freeOnly}>무료 행사만 보기</button>
          </div>
          <div className="filter-modal__summary" aria-live="polite">
            <span>{draft.categories.length ? draft.categories.join(' · ') : '전체 분야'}</span>
            <span>{draft.freeOnly ? '무료만' : '전체 요금'}</span>
          </div>
        </>
      ) : (
        <>
          <p className="home-modal__description">행사를 찾을 서울 지역을 선택하세요.</p>
          <div className="filter-modal__section">
            <h3 className="home-modal__section-title">서울 지역</h3>
            <div className="filter-modal__choices filter-modal__choices--districts">
              {['전체 지역', ...districts].map((district) => (
                <button type="button" key={district} className={`filter-modal__choice${draft.district === district ? ' filter-modal__choice--selected' : ''}`} onClick={() => setDraft((current) => ({ ...current, district }))} aria-pressed={draft.district === district}>{district}</button>
              ))}
            </div>
          </div>
          <div className="filter-modal__summary" aria-live="polite"><span>{draft.district}</span></div>
        </>
      )}
      <div className="filter-modal__actions">
        <button className="filter-modal__reset" type="button" onClick={reset}>초기화</button>
        <button className="primary-button" type="button" onClick={() => onConfirm(draft)}>확인</button>
      </div>
    </HomeModal>
  )
}
