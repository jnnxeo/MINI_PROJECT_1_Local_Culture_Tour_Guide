import React, { useState } from 'react'
import HomeModal from './HomeModal.jsx'

const suggestions = ['고궁 야간 행사', '전통문화', '종로구']

export default function KeywordModal({ onClose, onSearch }) {
  const [keyword, setKeyword] = useState('')
  const [validationError, setValidationError] = useState('')

  const submit = (event) => {
    event.preventDefault()
    if (!keyword.trim()) {
      setValidationError('검색어를 입력해 주세요.')
      return
    }
    onSearch(keyword.trim())
  }

  return (
    <HomeModal title="어떤 행사를 찾으세요?" titleId="keyword-modal-title" onClose={onClose}>
      <p className="home-modal__description">행사명, 장소, 지역, 행사 분야로 찾아보세요.</p>
      <form onSubmit={submit} noValidate>
        <label className={`keyword-modal__field${validationError ? ' keyword-modal__field--error' : ''}`}>
          <span>검색어</span>
          <input type="search" value={keyword} onChange={(event) => { setKeyword(event.target.value); setValidationError('') }} placeholder="행사명 · 장소 · 지역 · 행사 분야" aria-invalid={Boolean(validationError)} aria-describedby={validationError ? 'keyword-error' : undefined} autoFocus />
        </label>
        {validationError && <p className="home-search__error" id="keyword-error" role="alert">{validationError}</p>}
        <h3 className="home-modal__section-title">추천 검색어</h3>
        <div className="keyword-modal__suggestions">
          {suggestions.map((suggestion) => (
            <button type="button" key={suggestion} onClick={() => { setKeyword(suggestion); setValidationError('') }}>{suggestion}</button>
          ))}
        </div>
        <button className="primary-button home-modal__full-button" type="submit">검색 결과 보기</button>
      </form>
    </HomeModal>
  )
}
