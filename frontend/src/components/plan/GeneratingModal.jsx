import React, { useEffect, useState } from 'react'
import PlanModal from './PlanModal.jsx'

const STEPS = ['행사 시간 확인', '주변 맛집 탐색', '이동 순서 정리']

/**
 * Figma "generating · 팝업" — 추천을 만드는 동안 단계 표시
 * status: 'running' | 'done' | 'error'
 */
export default function GeneratingModal({ status, errorMessage, onConfirm, onCancel }) {
  const [doneCount, setDoneCount] = useState(0)

  useEffect(() => {
    if (status !== 'running') {
      return undefined
    }

    const intervalId = window.setInterval(() => {
      setDoneCount((count) => Math.min(count + 1, STEPS.length - 1))
    }, 600)

    return () => window.clearInterval(intervalId)
  }, [status])

  const completed = status === 'done' ? STEPS.length : doneCount

  if (status === 'error') {
    return (
      <PlanModal title="추천을 만들지 못했어요" onClose={onCancel}>
        <p className="tp-modal__desc">{`${errorMessage}\n기존 일정은 그대로 유지됩니다.`}</p>
        <button className="tp-btn tp-btn--primary tp-btn--block" type="button" onClick={onCancel}>
          확인
        </button>
      </PlanModal>
    )
  }

  return (
    <PlanModal title="취향에 맞는 하루를 찾고 있어요" onClose={onCancel}>
      <p className="tp-modal__desc">선택한 행사 시간과 주변 장소를 살펴보고 있어요.</p>
      <ul className="plan-steps" aria-live="polite">
        {STEPS.map((step, index) => (
          <li key={step} className={index < completed ? 'is-done' : ''}>
            {`${index < completed ? '●' : '○'}  ${step}`}
          </li>
        ))}
      </ul>
      <button className="tp-btn tp-btn--primary tp-btn--block" type="button" disabled={status !== 'done'} onClick={onConfirm}>
        추천 일정 확인
      </button>
      <button className="tp-btn tp-btn--secondary tp-btn--block" type="button" onClick={onCancel}>
        취소
      </button>
    </PlanModal>
  )
}
