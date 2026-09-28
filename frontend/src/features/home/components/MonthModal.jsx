import React, { useState } from 'react'
import HomeModal from './HomeModal.jsx'

const months = Array.from({ length: 12 }, (_, index) => index + 1)

export default function MonthModal({ month, onConfirm, onClose }) {
  const [initialYear, initialMonth] = month.split('-').map(Number)
  const [year, setYear] = useState(initialYear)
  const [selectedMonth, setSelectedMonth] = useState(initialMonth)

  const confirm = () => onConfirm(`${year}-${String(selectedMonth).padStart(2, '0')}`)

  return (
    <HomeModal title="월별 탐색" titleId="month-modal-title" onClose={onClose}>
      <p className="home-modal__description">행사를 찾을 연도와 월을 선택하세요.</p>
      <div className="month-modal__year" aria-label="연도 선택">
        <button type="button" onClick={() => setYear((current) => current - 1)} aria-label="이전 연도">‹</button>
        <strong>{year}년</strong>
        <button type="button" onClick={() => setYear((current) => current + 1)} aria-label="다음 연도">›</button>
      </div>
      <div className="month-modal__months" aria-label="월 선택">
        {months.map((value) => (
          <button
            key={value}
            type="button"
            className={`month-modal__month${selectedMonth === value ? ' month-modal__month--selected' : ''}`}
            onClick={() => setSelectedMonth(value)}
            aria-pressed={selectedMonth === value}
          >
            {value}월
          </button>
        ))}
      </div>
      <button className="primary-button home-modal__full-button" type="button" onClick={confirm}>확인</button>
    </HomeModal>
  )
}
