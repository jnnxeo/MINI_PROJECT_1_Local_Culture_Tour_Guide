import React, { useEffect, useRef } from 'react'
import '../../styles/plan-base.css'

/**
 * 나의 일정 팝업 틀 (Figma "* · 팝업") — 제목 + 닫기(×) + 내용
 * variant="list"는 장소 검색처럼 목록이 긴 팝업(여백 24/32, 간격 12)
 */
export default function PlanModal({ title, onClose, variant, children, labelledById }) {
  const dialogRef = useRef(null)
  const titleId = labelledById ?? 'tp-modal-title'

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        onClose?.()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    dialogRef.current?.focus()
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  return (
    <div
      className="tp-modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose?.()
        }
      }}
    >
      <section
        ref={dialogRef}
        className={`tp-modal${variant === 'list' ? ' tp-modal--list' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
      >
        <div className="tp-modal__heading">
          <h2 className="tp-modal__title" id={titleId}>{title}</h2>
          {onClose && (
            <button className="tp-btn tp-modal__close" type="button" aria-label="닫기" onClick={onClose}>
              ×
            </button>
          )}
        </div>
        {children}
      </section>
    </div>
  )
}
