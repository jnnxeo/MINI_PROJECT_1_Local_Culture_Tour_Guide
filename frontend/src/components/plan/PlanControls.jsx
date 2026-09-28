import React, { useState } from 'react'

/**
 * 나의 일정 팝업에서 쓰는 선택 컨트롤 모음.
 * 드롭다운처럼 눌러야 목록이 보이는 방식 대신, 고를 수 있는 값을 모두 펼쳐 두고 한 번에 누르게 한다.
 * 선택된 값은 색·체크 표시로 바로 구분되고, 키보드(Tab·Space·Enter)로도 고를 수 있다.
 */

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
      <path d="M5 12.5 10 17l9-10" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

/** 체크 카드 — 누르면 켜지고 꺼진다. 켜졌을 때만 children(세부 선택)을 펼친다. */
export function CheckCard({ checked, onChange, title, description, children }) {
  return (
    <div className={`plan-check-card${checked ? ' is-checked' : ''}`}>
      <button
        className="plan-check-card__head"
        type="button"
        role="checkbox"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
      >
        <span className="plan-check-card__mark" aria-hidden="true"><CheckIcon /></span>
        <span className="plan-check-card__text">
          <span className="plan-check-card__title">{title}</span>
          {description && <span className="plan-check-card__desc">{description}</span>}
        </span>
      </button>
      {children && (
        <div className="plan-check-card__body" aria-hidden={!checked}>
          <div className="plan-check-card__inner">{children}</div>
        </div>
      )}
    </div>
  )
}

/**
 * 칩 선택 — 값이 모두 보이는 버튼 묶음.
 * multiple 이면 여러 개(체크박스), 아니면 하나만(라디오) 고른다.
 */
export function ChoiceChips({ label, options, value, onChange, multiple = false, disabled = false }) {
  const isSelected = (option) => (multiple ? value.includes(option.value) : value === option.value)
  const toggle = (option) => {
    if (!multiple) {
      onChange(option.value)
      return
    }
    onChange(isSelected(option) ? value.filter((item) => item !== option.value) : [...value, option.value])
  }

  return (
    <div className="plan-choice" role={multiple ? 'group' : 'radiogroup'} aria-label={label}>
      {options.map((option) => {
        const selected = isSelected(option)

        return (
          <button
            key={option.value}
            className={`plan-choice__chip${selected ? ' is-selected' : ''}`}
            type="button"
            role={multiple ? 'checkbox' : 'radio'}
            aria-checked={selected}
            disabled={disabled}
            tabIndex={disabled ? -1 : undefined}
            onClick={() => toggle(option)}
          >
            <span className="plan-choice__check" aria-hidden="true"><CheckIcon /></span>
            {option.label}
          </button>
        )
      })}
    </div>
  )
}

/** 세그먼트 — 2~3개 중 하나를 고르는 탭 모양 버튼 */
export function Segmented({ label, options, value, onChange }) {
  const index = Math.max(0, options.findIndex((option) => option.value === value))

  return (
    <div
      className="plan-segmented"
      role="radiogroup"
      aria-label={label}
      style={{ '--plan-seg-count': options.length, '--plan-seg-index': index }}
    >
      <span className="plan-segmented__thumb" aria-hidden="true" />
      {options.map((option) => (
        <button
          key={option.value}
          className={`plan-segmented__option${option.value === value ? ' is-selected' : ''}`}
          type="button"
          role="radio"
          aria-checked={option.value === value}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}

/** 숫자 스테퍼 — −/+ 로 바꾼다 */
export function Stepper({ label, value, min, max, unit = '', onChange }) {
  return (
    <div className="plan-stepper" role="group" aria-label={label}>
      <button
        className="plan-stepper__button"
        type="button"
        aria-label={`${label} 줄이기`}
        disabled={value <= min}
        onClick={() => onChange(value - 1)}
      >
        −
      </button>
      <span className="plan-stepper__value" aria-live="polite">{`${value}${unit}`}</span>
      <button
        className="plan-stepper__button"
        type="button"
        aria-label={`${label} 늘리기`}
        disabled={value >= max}
        onClick={() => onChange(value + 1)}
      >
        +
      </button>
    </div>
  )
}

/**
 * 펼침 칸 — 항목이 많은 선택(예: 서울 25개 구)을 접어 두고, 제목 줄에 지금 값과 화살표를 보여 준다.
 * 누르면 아래로 부드럽게 펼쳐진다.
 */
export function ExpandSection({ title, value, defaultOpen = false, children }) {
  const [open, setOpen] = useState(defaultOpen)

  return (
    <div className={`plan-expand${open ? ' is-open' : ''}`}>
      <button className="plan-expand__head" type="button" aria-expanded={open} onClick={() => setOpen((current) => !current)}>
        <span className="plan-expand__title">{title}</span>
        <span className="plan-expand__value">{value}</span>
        <span className="plan-expand__chevron" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="18" height="18"><path d="m6 9 6 6 6-6" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" /></svg>
        </span>
      </button>
      <div className="plan-expand__body" aria-hidden={!open}>
        <div className="plan-expand__inner">{children}</div>
      </div>
    </div>
  )
}
