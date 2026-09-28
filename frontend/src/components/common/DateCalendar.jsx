import React, { useMemo, useState } from 'react'
import './date-calendar.css'

/**
 * 방문 날짜 달력 — 메인 AI 추천(가능한 날 여러 개 체크)과 나의 일정 조건 수정에서 같이 쓴다.
 * - multiple: 여러 날을 눌러 켜고 끈다. 아니면 하루만 고른다.
 * - min/max(YYYY-MM-DD) 밖의 날짜와 오늘 이전 날짜는 누를 수 없다.
 * - 값은 'YYYY-MM-DD' 문자열 배열(하루만 고를 때도 배열)로 주고받는다.
 * 날짜 계산은 모두 Asia/Seoul 기준 문자열로 해서 기기 시간대와 상관없이 같은 날짜를 가리킨다.
 */

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

export function todayInSeoul() {
  return new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Seoul' })
}

function toKey(year, month, day) {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function parseKey(key) {
  const [year, month, day] = key.split('-').map(Number)
  return { year, month, day }
}

function weekdayOf(key) {
  const { year, month, day } = parseKey(key)
  return new Date(Date.UTC(year, month - 1, day)).getUTCDay()
}

function addDays(key, days) {
  const { year, month, day } = parseKey(key)
  const date = new Date(Date.UTC(year, month - 1, day + days))
  return toKey(date.getUTCFullYear(), date.getUTCMonth() + 1, date.getUTCDate())
}

/** "10월 3일 (토)" */
export function formatDayLabel(key) {
  const { month, day } = parseKey(key)
  return `${month}월 ${day}일 (${WEEKDAYS[weekdayOf(key)]})`
}

/** 오늘 기준 이번·다음 주말(토·일) 날짜 */
function weekendOf(today, weeksLater) {
  const saturday = addDays(today, ((6 - weekdayOf(today)) + 7) % 7 + weeksLater * 7)
  return [saturday, addDays(saturday, 1)]
}

export default function DateCalendar({ value = [], onChange, multiple = true, min, max, label = '방문 날짜' }) {
  const today = todayInSeoul()
  const lower = min && min > today ? min : today
  const selected = useMemo(() => new Set(value), [value])
  const first = value.length ? [...value].sort()[0] : lower
  const [view, setView] = useState(() => {
    const { year, month } = parseKey(first)
    return { year, month }
  })

  const isDisabled = (key) => key < lower || (max && key > max)

  const toggle = (key) => {
    if (isDisabled(key)) return
    if (!multiple) {
      onChange([key])
      return
    }
    const next = new Set(selected)
    next.has(key) ? next.delete(key) : next.add(key)
    onChange([...next].sort())
  }

  const selectWeekend = (weeksLater) => {
    const days = weekendOf(today, weeksLater).filter((key) => !isDisabled(key))
    if (!days.length) return
    onChange(multiple ? [...new Set([...value, ...days])].sort() : [days[0]])
    const { year, month } = parseKey(days[0])
    setView({ year, month })
  }

  const move = (step) => {
    setView(({ year, month }) => {
      const index = year * 12 + (month - 1) + step
      return { year: Math.floor(index / 12), month: (index % 12) + 1 }
    })
  }

  const daysInMonth = new Date(Date.UTC(view.year, view.month, 0)).getUTCDate()
  const leading = weekdayOf(toKey(view.year, view.month, 1))
  const cells = [
    ...Array.from({ length: leading }, () => null),
    ...Array.from({ length: daysInMonth }, (_, index) => toKey(view.year, view.month, index + 1)),
  ]
  const monthStart = toKey(view.year, view.month, 1)
  const canPrev = monthStart > lower.slice(0, 8) + '01'
  const canNext = !max || toKey(view.year, view.month, daysInMonth) < max
  const sortedValue = [...value].sort()

  return (
    <div className="tp-cal" role="group" aria-label={label}>
      {multiple && (
        <div className="tp-cal__quick">
          <button type="button" className="tp-cal__quick-chip" onClick={() => selectWeekend(0)}>이번 주말</button>
          <button type="button" className="tp-cal__quick-chip" onClick={() => selectWeekend(1)}>다음 주말</button>
          {value.length > 0 && (
            <button type="button" className="tp-cal__quick-chip tp-cal__quick-chip--ghost" onClick={() => onChange([])}>
              선택 해제
            </button>
          )}
        </div>
      )}

      <div className="tp-cal__head">
        <button type="button" className="tp-cal__nav" aria-label="이전 달" disabled={!canPrev} onClick={() => move(-1)}>‹</button>
        <strong className="tp-cal__month" aria-live="polite">{`${view.year}년 ${view.month}월`}</strong>
        <button type="button" className="tp-cal__nav" aria-label="다음 달" disabled={!canNext} onClick={() => move(1)}>›</button>
      </div>

      <div className="tp-cal__grid" role="grid">
        {WEEKDAYS.map((weekday, index) => (
          <span key={weekday} className={`tp-cal__weekday tp-cal__weekday--${index}`} role="columnheader">{weekday}</span>
        ))}
        {cells.map((key, index) => {
          if (!key) return <span key={`blank-${index}`} className="tp-cal__blank" />
          const disabled = isDisabled(key)
          const isSelected = selected.has(key)
          const weekday = weekdayOf(key)

          return (
            <button
              key={key}
              type="button"
              role="gridcell"
              className={`tp-cal__day tp-cal__day--w${weekday}${isSelected ? ' is-selected' : ''}${key === today ? ' is-today' : ''}`}
              aria-pressed={isSelected}
              aria-label={`${formatDayLabel(key)}${isSelected ? ' 선택됨' : ''}`}
              disabled={disabled}
              onClick={() => toggle(key)}
            >
              {parseKey(key).day}
            </button>
          )
        })}
      </div>

      <p className="tp-cal__summary" aria-live="polite">
        {sortedValue.length === 0
          ? (multiple ? '갈 수 있는 날을 모두 눌러 주세요. 그중 하루에 맞는 행사를 찾아요.' : '방문할 날을 골라 주세요.')
          : multiple
            ? `${sortedValue.length}일 선택 · ${sortedValue.slice(0, 3).map(formatDayLabel).join(', ')}${sortedValue.length > 3 ? ` 외 ${sortedValue.length - 3}일` : ''}`
            : `${formatDayLabel(sortedValue[0])} 방문`}
      </p>
    </div>
  )
}
