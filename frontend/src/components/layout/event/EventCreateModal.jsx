import React, { useState } from 'react'
import Modal from '../../common/Modal.jsx'
import { createEventDraft } from '../../../services/planService.js'
import EventSelect from './EventSelect.jsx'

const INTERESTS = ['문화·역사', '음악·공연', '미술·전시']

export default function EventCreateModal({ event, onClose, onCreated }) {
  const today = new Date().toLocaleDateString('sv-SE', {
    timeZone: 'Asia/Seoul',
  })
  const firstDate = [today, event.startDate].filter(Boolean).sort().at(-1)

  const [tripDate, setTripDate] = useState(firstDate)
  const [headcount, setHeadcount] = useState(2)
  const [transportMode, setTransportMode] = useState('WALK_TRANSIT')
  const [interests, setInterests] = useState([])
  const [useAi, setUseAi] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const unavailableReason = event.endDate < today
    ? '종료된 행사는 일정으로 만들 수 없습니다.'
    : event.lat == null || event.lng == null
      ? '행사 위치 정보가 없어 일정을 만들 수 없습니다.'
      : null

  function toggleInterest(interest) {
    setInterests((current) =>
      current.includes(interest)
        ? current.filter((item) => item !== interest)
        : [...current, interest]
    )
  }

  async function handleSubmit() {
    if (unavailableReason) {
      setError(unavailableReason)
      return
    }
    if (interests.length === 0) {
      setError('관심사를 1개 이상 선택해 주세요.')
      return
    }

    if (!tripDate || tripDate < event.startDate || tripDate > event.endDate) {
      setError('행사 기간 안의 여행 날짜를 선택해 주세요.')
      return
    }

    setSaving(true)
    setError('')

    try {
      const draft = await createEventDraft({
        eventId: event.eventId,
        tripDate,
        headcount,
        transportMode,
        interests,
        useAi,
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
      <strong>관심사</strong>
      <div className="event-modal__interests">
        {INTERESTS.map((interest) => {
          const selected = interests.includes(interest)

          return (
            <button
              key={interest}
              type="button"
              className={`tp-btn tp-btn--secondary${selected ? ' is-selected' : ''}`}
              aria-pressed={selected}
              onClick={() => toggleInterest(interest)}
            >
              {selected ? '☑' : '☐'} {interest}
            </button>
          )
        })}
      </div>

      <p className="event-modal__hint">
        행사 시간과 관심사를 기준으로 주변 동선을 추천합니다.
      </p>

      <button
        type="button"
        className={`tp-btn tp-btn--secondary${useAi ? ' is-selected' : ''}`}
        aria-pressed={useAi}
        onClick={() => setUseAi((current) => !current)}
      >
        AI 추천받기
      </button>

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
        {saving
            ? '일정을 만드는 중입니다…'
            : useAi
                ? 'AI로 하루 일정 만들기'
                : '하루 일정 만들기'}
      </button>

      <p className="event-modal__hint">
        AI 해제 시 규칙에 따른 기본 코스를 구성합니다.
      </p>
    </Modal>
  )
}
