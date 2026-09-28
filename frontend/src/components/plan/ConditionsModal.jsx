import React, { useState } from 'react'
import PlanModal from './PlanModal.jsx'
import { formatDate } from '../../utils/planTime.js'

export const INTERESTS = ['문화·역사', '음악·공연', '미술·전시']
export const TRANSPORT_LABELS = {
  WALK_TRANSIT: '도보 + 대중교통',
  WALK: '도보 위주',
}
export const FOOD_PREFERENCE_LABELS = {
  ALL: '전체',
  KOREAN: '한식',
  CHINESE: '중식',
  JAPANESE: '일식',
  WESTERN: '양식',
}
/**
 * Figma "conditions · 팝업" (SCR-010) — 인원·이동 방법은 드롭다운(people/transport 팝업 대체)
 * API-PLAN-003 Body는 {visitDate,startTime,endTime,companion,foodPreference,mealType,transportMode}를 쓴다.
 * Figma의 기존 조건에 음식 선호·식사 시간대를 추가해 맛집 추천 기준을 명확하게 한다.
 * mealType은 "BOTH"(점심+저녁 모두)·"LUNCH"·"DINNER"로 항상 명시적인 값을 보낸다.
 * 생성 API가 mealType 없음을 "둘 다"로 해석하는 것과 같은 의미이며, 조건을 열고 그대로
 * 적용해도 기존 값이 바뀌지 않도록 값이 없을 때는 LUNCH가 아니라 BOTH로 초기화한다.
 * 메인 AI 추천 모달과 같은 구조로 카페 포함 체크 → 점심·저녁 체크 → 끼니마다 음식 종류 하나를 고른다
 * (lunchFoodPreference·dinnerFoodPreference·includeCafe, docs/07 [제안]). 둘 다 해제하면 적용할 수 없다.
 */
export default function ConditionsModal({ visitDate, conditions, coreItem, onSubmit, onClose }) {
  const [headcount, setHeadcount] = useState(2)
  const [transportMode, setTransportMode] = useState(conditions?.transportMode ?? 'WALK_TRANSIT')
  const savedMealType = conditions?.mealType ?? 'BOTH'
  const [lunch, setLunch] = useState(savedMealType !== 'DINNER')
  const [dinner, setDinner] = useState(savedMealType !== 'LUNCH')
  const [lunchFood, setLunchFood] = useState(conditions?.lunchFoodPreference ?? conditions?.foodPreference ?? 'ALL')
  const [dinnerFood, setDinnerFood] = useState(conditions?.dinnerFoodPreference ?? conditions?.foodPreference ?? 'ALL')
  const [includeCafe, setIncludeCafe] = useState(Boolean(conditions?.includeCafe))
  const [interests, setInterests] = useState([])
  const [useAi, setUseAi] = useState(true)

  const toggleInterest = (interest) => {
    setInterests((current) => (current.includes(interest)
      ? current.filter((value) => value !== interest)
      : [...current, interest]))
  }

  const coreInfo = coreItem?.timeFixed ? `${coreItem.startTime} 시작` : '운영시간 정보 없음'
  const noMeal = !lunch && !dinner
  const mealType = lunch && dinner ? 'BOTH' : (lunch ? 'LUNCH' : 'DINNER')

  const mealField = (label, checked, setChecked, food, setFood) => (
    <div className="tp-field plan-conditions__meal">
      <button
        className={`tp-btn tp-btn--secondary plan-toggle${checked ? ' is-checked' : ''}`}
        type="button"
        role="checkbox"
        aria-checked={checked}
        onClick={() => setChecked((value) => !value)}
      >
        {`${checked ? '☑' : '☐'} ${label}`}
      </button>
      <span className="tp-field__label">{`${label} 음식 종류`}</span>
      <select
        aria-label={`${label} 음식 종류`}
        value={food}
        disabled={!checked}
        onChange={(event) => setFood(event.target.value)}
      >
        {Object.entries(FOOD_PREFERENCE_LABELS).map(([value, text]) => (
          <option key={value} value={value}>{text}</option>
        ))}
      </select>
      <p className="plan-conditions__meal-hint">
        {checked ? '눌러서 음식 종류를 바꿀 수 있어요' : `${label}을 체크하면 고를 수 있어요`}
      </p>
    </div>
  )

  return (
    <PlanModal title="어떤 하루를 만들어 볼까요?" onClose={onClose}>
      <p className="tp-modal__desc">{`선택한 행사 · ${coreInfo}`}</p>

      <div className="tp-field">
        <span className="tp-field__label">여행 날짜</span>
        <span className="tp-field__value">{`${formatDate(visitDate)} · 당일 여행`}</span>
      </div>

      <div className="plan-conditions__row">
        <label className="tp-field">
          <span className="tp-field__label">인원 (추천에는 반영되지 않음)</span>
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

      <p className="plan-conditions__label">식사 · 카페</p>
      <p className="plan-conditions__help">원하는 끼니를 체크하고, 끼니마다 음식 종류를 하나씩 골라 주세요.</p>
      <button
        className={`tp-btn tp-btn--secondary tp-btn--block plan-toggle${includeCafe ? ' is-checked' : ''}`}
        type="button"
        role="checkbox"
        aria-checked={includeCafe}
        onClick={() => setIncludeCafe((value) => !value)}
      >
        {`${includeCafe ? '☑' : '☐'} 카페 포함 (식사 시간 외 빈 시간에 근처 카페 1곳)`}
      </button>
      <div className="plan-conditions__row">
        {mealField('점심', lunch, setLunch, lunchFood, setLunchFood)}
        {mealField('저녁', dinner, setDinner, dinnerFood, setDinnerFood)}
      </div>
      {noMeal && <p className="tp-modal__error" role="alert">점심과 저녁 중 하나 이상 선택해 주세요.</p>}

      <p className="plan-conditions__label">관심사 (추천에는 반영되지 않음)</p>
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
      <p className="plan-conditions__help">점심·저녁마다 선택한 음식 종류로, 그 시간에 영업 중인 행사 주변 맛집을 추천합니다.</p>

      <button
        className={`tp-btn tp-btn--secondary tp-btn--bold tp-btn--block plan-toggle${useAi ? ' is-checked' : ''}`}
        type="button"
        role="checkbox"
        aria-checked={useAi}
        onClick={() => setUseAi((value) => !value)}
      >
        {`${useAi ? '☑' : '☐'} AI 추천받기`}
      </button>
      <p className="plan-conditions__note">AI 추천받기 선택은 아직 추천 결과에 반영되지 않습니다(항상 AI 추천 기준으로 생성).</p>

      <button
        className="tp-btn tp-btn--primary tp-btn--block"
        type="button"
        disabled={noMeal}
        onClick={() => onSubmit({
          headcount,
          transportMode,
          mealType,
          lunchFoodPreference: lunchFood,
          dinnerFoodPreference: dinnerFood,
          includeCafe,
          interests,
          useAi,
        })}
      >
        {useAi ? 'AI로 하루 일정 만들기' : '하루 일정 만들기'}
      </button>
      <p className="plan-conditions__note">AI 해제 시 규칙에 따른 기본 코스를 구성합니다.</p>
    </PlanModal>
  )
}
