import React, { useState } from 'react'
import PlanModal from './PlanModal.jsx'
import { TITLE_MAX_LENGTH } from '../../utils/planTime.js'

/** Figma "rename · 팝업" */
export default function RenameModal({ title, onSave, onClose }) {
  const [value, setValue] = useState(title)
  const [error, setError] = useState('')

  const handleSubmit = (event) => {
    event.preventDefault()
    const trimmed = value.trim()

    if (!trimmed) {
      setError('일정 이름을 입력해 주세요.')
      return
    }

    onSave(trimmed)
  }

  return (
    <PlanModal title="일정 이름 변경" onClose={onClose}>
      <p className="tp-modal__desc">여행을 기억하기 좋은 이름을 붙여 보세요.</p>
      <form className="plan-form" onSubmit={handleSubmit} noValidate>
        <label className="tp-field">
          <span className="tp-field__label">일정 이름</span>
          <input
            value={value}
            maxLength={TITLE_MAX_LENGTH}
            aria-invalid={Boolean(error)}
            autoFocus
            onChange={(event) => {
              setValue(event.target.value)
              setError('')
            }}
          />
        </label>
        {error && <p className="tp-modal__error" role="alert">{error}</p>}
        <button className="tp-btn tp-btn--primary tp-btn--block" type="submit">
          이름 저장
        </button>
      </form>
    </PlanModal>
  )
}
