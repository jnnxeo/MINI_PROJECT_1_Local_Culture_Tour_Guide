import React from 'react'

const months = Array.from({ length: 12 }, (_, index) => index + 1)

export default function MonthSelector({ selectedMonth, onSelect }) {
  return (
    <section className="month-picker" aria-labelledby="month-picker-title">
      <h2 id="month-picker-title">월별 행사 선택</h2>
      <div className="month-picker__list" aria-label="월 선택">
        {months.map((month) => (
          <button
            className={`month-picker__button${month === selectedMonth ? ' month-picker__button--active' : ''}`}
            type="button"
            key={month}
            onClick={() => onSelect(month)}
            aria-pressed={month === selectedMonth}
          >
            {month}월
          </button>
        ))}
      </div>
    </section>
  )
}
