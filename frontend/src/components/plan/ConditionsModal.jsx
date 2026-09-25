import React, { useState } from 'react'
import PlanModal from './PlanModal.jsx'
import { formatDate } from '../../utils/planTime.js'

export const INTERESTS = ['문화·역사', '음악·공연', '미술·전시']
export const TRANSPORT_LABELS = {
  WALK_TRANSIT: '도보 + 대중교통',
  WALK: '도보 위주',
}

/**
 * Figma "conditions · 팝업" (SCR-010) — 인원·이동 방법은 드롭다운(people/transport 팝업 대체)
 * API-PLAN-003 Body는 {visitDate,startTime,endTime,companion,foodPreference,transportMode}라
 * 화면의 인원·관심사·AI 추천받기는 대응 필드가 없다 → 팀 확인 전까지 이동 방법만 전송한다.
 */
export default function ConditionsModal({ visitDate, conditions, coreItem, onSubmit, onClose }) {
  const [headcount, setHeadcount] = useState(2)
  const [transportMode, setTransportMode] = useState(conditions?.transportMode ?? 'WALK_TRANSIT')
  const [interests, setInterests] = useState([])
  const [useAi, setUseAi] = useState(true)

  const toggleInterest = (interest) => {
    setInterests((current) => (current.includes(interest)
      ? current.filter((value) => value !== interest)
      : [...current, interest]))
  }

  const coreInfo = coreItem?.timeFixed ? `${coreItem.startTime} 시작` : '운영시간 정보 없음'

  return (
    <PlanModal title="어떤 하루를 만들어 볼까요?" onClose={onClose}>
      <p className="tp-modal__desc">{`선택한 행사 · ${coreInfo}`}</p>

      <div className="tp-field">
        <span className="tp-field__label">여행 날짜</span>
        <span className="tp-field__value">{`${formatDate(visitDate)} · 당일 여행`}</span>
      </div>

      <div className="plan-conditions__row">
        <label className="tp-field">
          <span className="tp-field__label">인원</span>
          <select value={headcount} onChange={(event) => setHeadcount(Number(event.target.value))}>
            {Array.from({ length: 10 }, (_, index) => index + 1).map((count) => (
              <option key={count} value={count}>{`${count}명`}</option>
            ))}
          </select>
        </label>
        <label className="tp-field">
          <span className="tp-field__label">이동 방법</span>
          <select value={transportMode} onChange={(event) => setTransportMode(event.target.value)}>
            {Object.entries(TRANSPORT_LABELS).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </label>
      </div>

      <p className="plan-conditions__label">관심사</p>
      <div className="plan-conditions__interests">
        {INTERESTS.map((interest) => {
          const checked = interests.includes(interest)

          return (
            <button
              key={interest}
              className={`tp-btn tp-btn--secondary plan-toggle${checked ? ' is-checked' : ''}`}
              type="button"
              role="checkbox"
              aria-checked={checked}
              onClick={() => toggleInterest(interest)}
            >
              {`${checked ? '☑' : '☐'} ${interest}`}
            </button>
          )
        })}
      </div>
      <p className="plan-conditions__help">행사 시간과 관심사를 기준으로 주변 동선을 추천합니다.</p>

      <button
        className={`tp-btn tp-btn--secondary tp-btn--bold tp-btn--block plan-toggle${useAi ? ' is-checked' : ''}`}
        type="button"
        role="checkbox"
        aria-checked={useAi}
        onClick={() => setUseAi((value) => !value)}
      >
        {`${useAi ? '☑' : '☐'} AI 추천받기`}
      </button>

      <button
        className="tp-btn tp-btn--primary tp-btn--block"
        type="button"
        onClick={() => onSubmit({ headcount, transportMode, interests, useAi })}
      >
        {useAi ? 'AI로 하루 일정 만들기' : '하루 일정 만들기'}
      </button>
      <p className="plan-conditions__note">AI 해제 시 규칙에 따른 기본 코스를 구성합니다.</p>
    </PlanModal>
  )
}
