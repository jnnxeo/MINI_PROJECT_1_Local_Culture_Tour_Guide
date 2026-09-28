import React, { useState } from 'react'
import Modal from '../../common/Modal.jsx'
import { recommendDraft } from '../../../services/planService.js'
import EventSelect from './EventSelect.jsx'

export default function EventCreateModal({ event, onClose, onCreated }) {
  const today = new Date().toLocaleDateString('sv-SE', {
    timeZone: 'Asia/Seoul',
  })
  // 저녁 8시가 지나면 오늘 일정은 만들 수 없으므로(서버 규칙) 기본 날짜를 내일로 둔다
  const seoulHour = Number(new Date().toLocaleString('en-US', { timeZone: 'Asia/Seoul', hour: '2-digit', hour12: false }))
  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000).toLocaleDateString('sv-SE', { timeZone: 'Asia/Seoul' })
  const earliest = seoulHour >= 20 ? tomorrow : today
  const firstDate = event.startDate && event.startDate > earliest ? event.startDate : earliest

  const [tripDate, setTripDate] = useState(firstDate)
  const [headcount, setHeadcount] = useState(2)
  const [transportMode, setTransportMode] = useState('WALK_TRANSIT')
  const [foodPreference, setFoodPreference] = useState('ALL')
  const [mealType, setMealType] = useState('BOTH')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const unavailableReason = !event.startDate || !event.endDate
    || event.startDate > event.endDate
    ? '행사 날짜 정보가 없어 여행 날짜를 선택할 수 없습니다.'
    : event.endDate < today
      ? '종료된 행사는 일정으로 만들 수 없습니다.'
      : event.lat == null || event.lng == null
        ? '행사 위치 정보가 없어 일정을 만들 수 없습니다.'
        : null

  async function handleSubmit() {
    if (unavailableReason) {
      setError(unavailableReason)
      return
    }
    if (!tripDate || tripDate < event.startDate || tripDate > event.endDate) {
      setError('행사 기간 안의 여행 날짜를 선택해 주세요.')
      return
    }

    setSaving(true)
    setError('')

    try {
      const draft = await recommendDraft({
        eventId: event.eventId,
        visitDate: tripDate,
        headcount,
        transportMode,
        foodPreference,
        mealType,
      })
      onCreated(draft)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title="어떤 하루를 만들어 볼까요?" onClose={onClose}>
      <p className="tp-modal__desc">
        {event.title} · {event.startTime ? `${event.startTime.slice(0, 5)} 시작` : '운영시간 정보 없음'}
      </p>

      <label className="tp-field">
        <span className="tp-field__label">여행 날짜 · 당일 여행</span>
        <input
          type="date"
          value={tripDate}
          min={firstDate}
          max={event.endDate}
          onChange={(e) => setTripDate(e.target.value)}
        />
      </label>

      <div className="event-modal__row">
        <EventSelect
            label="인원"
            value={headcount}
            onChange={setHeadcount}
            options={[
            { value: 1, label: '1명' },
            { value: 2, label: '2명' },
            { value: 3, label: '3명' },
            { value: 4, label: '4명 이상' },
            ]}
        />
        <EventSelect
            label="이동 방법"
            value={transportMode}
            onChange={setTransportMode}
            options={[
            { value: 'WALK_TRANSIT', label: '도보 + 대중교통' },
            { value: 'WALK', label: '도보 위주' },
            ]}
        />
      </div>

      <div className="event-modal__row">
        <EventSelect
          label="음식 종류"
          value={foodPreference}
          onChange={setFoodPreference}
          options={[
            { value: 'ALL', label: '전체' },
            { value: 'KOREAN', label: '한식' },
            { value: 'CHINESE', label: '중식' },
            { value: 'JAPANESE', label: '일식' },
            { value: 'WESTERN', label: '양식' },
          ]}
        />
        <EventSelect
          label="식사 시간"
          value={mealType}
          onChange={setMealType}
          options={[
            { value: 'BOTH', label: '점심 + 저녁' },
            { value: 'LUNCH', label: '점심' },
            { value: 'DINNER', label: '저녁' },
          ]}
        />
      </div>

      <p className="event-modal__hint">행사 시간과 선택한 음식 종류에 맞는 주변 맛집을 추천합니다.</p>

      {error && <p className="tp-modal__error" role="alert">{error}</p>}
      {unavailableReason && !error && (
        <p className="tp-modal__error" role="alert">{unavailableReason}</p>
      )}

      <button
        type="button"
        className="tp-btn tp-btn--primary tp-btn--block"
        disabled={saving || Boolean(unavailableReason)}
        onClick={handleSubmit}
      >
        {saving ? '일정을 만드는 중입니다…' : '하루 일정 만들기'}
      </button>

      <p className="event-modal__hint">
        주변에 조건에 맞는 음식점이 없으면 일정이 생성되지 않으며 사유를 안내합니다.
      </p>
    </Modal>
  )
}
