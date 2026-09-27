import React from 'react'
import PlanModal from './PlanModal.jsx'

/**
 * 확인형 팝업 — Figma remove / regenerate / timeerror / saved 팝업이 같은 구조
 * (제목 · 설명 · 주 버튼 · 보조 버튼)
 */
export default function ConfirmModal({
  title,
  description,
  confirmLabel,
  confirmVariant = 'primary',
  cancelLabel,
  busy = false,
  onConfirm,
  onCancel,
  onClose,
}) {
  return (
    <PlanModal title={title} onClose={onClose}>
      {description && <p className="tp-modal__desc">{description}</p>}
      <button
        className={`tp-btn tp-btn--${confirmVariant} tp-btn--block`}
        type="button"
        disabled={busy}
        onClick={onConfirm}
      >
        {confirmLabel}
      </button>
      {cancelLabel && (
        <button className="tp-btn tp-btn--secondary tp-btn--block" type="button" onClick={onCancel ?? onClose}>
          {cancelLabel}
        </button>
      )}
    </PlanModal>
  )
}
