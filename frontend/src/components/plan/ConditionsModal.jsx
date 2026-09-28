import React, { useState } from 'react'
import PlanModal from './PlanModal.jsx'
import { CheckCard, ChoiceChips, Segmented, Stepper } from './PlanControls.jsx'
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

const FOOD_OPTIONS = Object.entries(FOOD_PREFERENCE_LABELS).map(([value, label]) => ({ value, label }))
const TRANSPORT_OPTIONS = Object.entries(TRANSPORT_LABELS).map(([value, label]) => ({ value, label }))
const INTEREST_OPTIONS = INTERESTS.map((interest) => ({ value: interest, label: interest }))

/**
 * Figma "conditions · 팝업" (SCR-010) — 추천 조건 수정
 * API-PLAN-003 Body는 {visitDate,startTime,endTime,companion,foodPreference,mealType,transportMode}를 쓴다.
 * mealType은 "BOTH"(점심+저녁 모두)·"LUNCH"·"DINNER"로 항상 명시적인 값을 보낸다.
 * 생성 API가 mealType 없음을 "둘 다"로 해석하는 것과 같은 의미이며, 조건을 열고 그대로
 * 적용해도 기존 값이 바뀌지 않도록 값이 없을 때는 LUNCH가 아니라 BOTH로 초기화한다.
 * 메인 AI 추천 모달과 같은 구조로 점심·저녁을 체크하고 끼니마다 음식 종류 하나를 고르며, 카페 포함을 정한다
 * (lunchFoodPreference·dinnerFoodPreference·includeCafe, docs/07 [제안]). 점심·저녁을 모두 끄면 적용할 수 없다.
 * 여행 날짜는 바꾸지 않는다 — 다른 날짜는 메인 화면에서 새 여행으로 만든다 (09-28 팀 결정).
 * 선택지는 드롭다운 대신 모두 펼쳐 두고(칩·세그먼트·스테퍼·체크 카드) 한 번에 누르게 한다.
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

  const coreInfo = coreItem?.timeFixed ? `${coreItem.startTime} 시작` : '운영시간 정보 없음'
  const noMeal = !lunch && !dinner
  const mealType = lunch && dinner ? 'BOTH' : (lunch ? 'LUNCH' : 'DINNER')

  return (
    <PlanModal title="어떤 하루를 만들어 볼까요?" onClose={onClose}>
      <p className="tp-modal__desc">{`선택한 행사 · ${coreInfo}`}</p>

      <section className="plan-conditions__section" aria-label="여행 정보">
        <div className="plan-conditions__readonly">
          <span className="plan-conditions__readonly-label">여행 날짜</span>
          <span className="plan-conditions__readonly-value">{`${formatDate(visitDate)} · 당일 여행`}</span>
          <span className="plan-conditions__readonly-hint">다른 날짜로 가려면 메인 화면에서 새 여행을 만들어 주세요.</span>
        </div>

        <div className="plan-conditions__line">
          <span className="plan-conditions__line-label">
            인원
            <small>추천에는 반영되지 않아요</small>
          </span>
          <Stepper label="인원" value={headcount} min={1} max={10} unit="명" onChange={setHeadcount} />
        </div>

        <div className="plan-conditions__line plan-conditions__line--stack">
          <span className="plan-conditions__line-label">이동 방법</span>
          <Segmented label="이동 방법" options={TRANSPORT_OPTIONS} value={transportMode} onChange={setTransportMode} />
        </div>
      </section>

      <section className="plan-conditions__section" aria-label="식사와 카페">
        <p className="plan-conditions__label">식사 · 카페</p>
        <p className="plan-conditions__help">먹을 끼니를 켜고, 끼니마다 음식 종류를 하나씩 골라 주세요.</p>

        <CheckCard
          checked={lunch}
          onChange={setLunch}
          title="점심"
          description={lunch ? `${FOOD_PREFERENCE_LABELS[lunchFood]} · 11:00~14:30 사이` : '점심은 넣지 않아요'}
        >
          <ChoiceChips label="점심 음식 종류" options={FOOD_OPTIONS} value={lunchFood} onChange={setLunchFood} disabled={!lunch} />
        </CheckCard>

        <CheckCard
          checked={dinner}
          onChange={setDinner}
          title="저녁"
          description={dinner ? `${FOOD_PREFERENCE_LABELS[dinnerFood]} · 17:00~20:30 사이` : '저녁은 넣지 않아요'}
        >
          <ChoiceChips label="저녁 음식 종류" options={FOOD_OPTIONS} value={dinnerFood} onChange={setDinnerFood} disabled={!dinner} />
        </CheckCard>

        <CheckCard
          checked={includeCafe}
          onChange={setIncludeCafe}
          title="카페 들르기"
          description="식사 시간 외 빈 시간에 행사장 근처 카페 1곳을 넣어요"
        />

        {noMeal && <p className="tp-modal__error" role="alert">점심과 저녁 중 하나 이상 켜 주세요.</p>}
      </section>

      <section className="plan-conditions__section" aria-label="관심사">
        <p className="plan-conditions__label">
          관심사
          <small>추천에는 반영되지 않아요</small>
        </p>
        <ChoiceChips label="관심사" options={INTEREST_OPTIONS} value={interests} onChange={setInterests} multiple />
      </section>

      <CheckCard
        checked={useAi}
        onChange={setUseAi}
        title="AI 추천받기"
        description="아직 추천 결과에는 반영되지 않아요 (항상 AI 추천 기준으로 만들어요)"
      />

      <button
        className="tp-btn tp-btn--primary tp-btn--block tp-btn--bold"
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
    </PlanModal>
  )
}
