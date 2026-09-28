import React, { useRef, useState } from 'react'
import HomeModal from './HomeModal.jsx'

const suggestions = ['전시', '공연', '종로구']
const MAX_KEYWORD_LENGTH = 200

export default function KeywordModal({ initialKeyword = '', onClose, onSearch }) {
  const [keyword, setKeyword] = useState(initialKeyword)
  const [validationError, setValidationError] = useState('')
  const inputRef = useRef(null)

  const updateKeyword = (value) => {
    setKeyword(value)
    setValidationError('')
  }

  const submit = (event) => {
    event.preventDefault()
    if (!keyword.trim()) {
      setValidationError('검색어를 입력해 주세요.')
      inputRef.current?.focus()
      return
    }
    if (keyword.trim().length > MAX_KEYWORD_LENGTH) {
      setValidationError('검색어는 최대 200자까지 입력할 수 있어요.')
      inputRef.current?.focus()
      return
    }
    onSearch(keyword.trim())
  }

  return (
    <HomeModal title="행사 키워드 검색" titleId="keyword-modal-title" className="keyword-modal" onClose={onClose}>
      <p className="home-modal__description">행사명, 장소, 지역, 행사 분야로 찾아보세요.</p>
      <form onSubmit={submit} noValidate>
        <div className={`keyword-modal__field${validationError ? ' keyword-modal__field--error' : ''}`}>
          <label htmlFor="keyword-input">검색어</label>
          <div className="keyword-modal__input-row">
            <input id="keyword-input" ref={inputRef} type="search" maxLength={MAX_KEYWORD_LENGTH} value={keyword} onChange={(event) => updateKeyword(event.target.value)} placeholder="행사명 · 장소 · 지역 · 행사 분야" aria-invalid={Boolean(validationError)} aria-describedby={validationError ? 'keyword-error' : undefined} />
            {keyword && (
              <button className="keyword-modal__clear" type="button" aria-label="검색어 지우기" onClick={() => { updateKeyword(''); inputRef.current?.focus() }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" /></svg>
              </button>
            )}
          </div>
        </div>
        {validationError && <p className="home-search__error" id="keyword-error" role="alert">{validationError}</p>}
        <h3 className="home-modal__section-title">검색 예시</h3>
        <div className="keyword-modal__suggestions">
          {suggestions.map((suggestion) => (
            <button type="button" key={suggestion} onClick={() => { updateKeyword(suggestion); inputRef.current?.focus() }}>{suggestion}</button>
          ))}
        </div>
        <button className="primary-button home-modal__full-button" type="submit">검색하기</button>
      </form>
    </HomeModal>
  )
}
