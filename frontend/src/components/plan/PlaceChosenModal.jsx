import React from 'react'
import Modal from '../common/Modal.jsx'
import { getEndTime } from '../../utils/planTime.js'
import { withObject } from '../../utils/korean.js'

/** Figma "placechosen · 팝업" — 추가할 장소의 방문 시간 확인 */
export default function PlaceChosenModal({ place, startTime, durationMin, onEditTime, onConfirm, onClose }) {
  return (
    <Modal title={`${withObject(place.name)} 추가할까요?`} onClose={onClose}>
      <p className="tp-modal__desc">행사 시작 시간과 겹치지 않게 방문 시간을 정하세요.</p>

      <button className="tp-field" type="button" onClick={onEditTime}>
        <span className="tp-field__label">방문 시간</span>
        <span className="tp-field__value">
          {startTime ? `${startTime} – ${getEndTime({ startTime, durationMin })}` : '방문 시간을 선택해 주세요'}
        </span>
      </button>

      <button className="tp-btn tp-btn--primary tp-btn--block" type="button" disabled={!startTime} onClick={onConfirm}>
        일정에 추가
      </button>
    </Modal>
  )
}
