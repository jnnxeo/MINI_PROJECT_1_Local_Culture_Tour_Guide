import React, { useState } from 'react'
import PlanModal from './PlanModal.jsx'
import { CheckCard, ChoiceChips, ExpandSection, Segmented, Stepper } from './PlanControls.jsx'
import DateCalendar, { earliestTripDate } from '../common/DateCalendar.jsx'
import { ALL_DISTRICTS, EVENT_CATEGORIES, SEOUL_DISTRICTS } from '../../constants/eventFilters.js'

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

const ALL = '전체'
const FOOD_OPTIONS = Object.entries(FOOD_PREFERENCE_LABELS).map(([value, label]) => ({ value, label }))
const TRANSPORT_OPTIONS = Object.entries(TRANSPORT_LABELS).map(([value, label]) => ({ value, label }))
const CATEGORY_OPTIONS = [ALL, ...EVENT_CATEGORIES].map((category) => ({ value: category, label: category }))
const DISTRICT_OPTIONS = [ALL_DISTRICTS, ...SEOUL_DISTRICTS].map((district) => ({ value: district, label: district }))

/**
 * Figma "conditions · 팝업" (SCR-010) — 추천 조건 수정 (API-PLAN-003)
 * - 여행 날짜: 달력으로 바꾼다 (09-28 팀 결정).
 *   메인 AI 추천(조건)으로 만든 초안은 갈 수 있는 날을 여러 개, 행사를 직접 고른 초안(행사 상세)은 그 행사 기간 안에서 하루.
 * - 행사 조건(분야·지역·무료): 조건으로 만든 초안만 — 바꾸면 조건에 맞는 행사를 다시 고른다.
 * - 식사: 점심·저녁 체크 카드 + 끼니별 음식 종류, 카페 포함 (lunchFoodPreference·dinnerFoodPreference·includeCafe).
 * mealType은 "BOTH"·"LUNCH"·"DINNER"로 항상 명시적인 값을 보낸다. 선택지는 드롭다운 없이 모두 펼쳐 둔다.
 */
export default function ConditionsModal({ visitDate, conditions, selectedEvent, coreItem, onSubmit, onClose, mode = 'edit', busy = false, error = '' }) {
  const conditionMode = Array.isArray(conditions?.availableDates)
  // 지난 날짜(밤 8시 이후면 오늘 포함)는 달력에서 고를 수도 풀 수도 없으니 처음부터 뺀다
  const [dates, setDates] = useState(() => (conditionMode ? conditions.availableDates : [visitDate])
    .filter((date) => date && date >= earliestTripDate()))
  const [categories, setCategories] = useState(conditions?.categories ?? [])
  const [district, setDistrict] = useState(conditions?.district ?? ALL_DISTRICTS)
  const [freeOnly, setFreeOnly] = useState(conditions?.freeYn === true)
  const [headcount, setHeadcount] = useState(2)
  const [transportMode, setTransportMode] = useState(conditions?.transportMode ?? 'WALK_TRANSIT')
  const savedMealType = conditions?.mealType ?? 'BOTH'
  const [lunch, setLunch] = useState(savedMealType !== 'DINNER')
  const [dinner, setDinner] = useState(savedMealType !== 'LUNCH')
  const [lunchFood, setLunchFood] = useState(conditions?.lunchFoodPreference ?? conditions?.foodPreference ?? 'ALL')
  const [dinnerFood, setDinnerFood] = useState(conditions?.dinnerFoodPreference ?? conditions?.foodPreference ?? 'ALL')
  const [includeCafe, setIncludeCafe] = useState(Boolean(conditions?.includeCafe))
  const [useAi, setUseAi] = useState(true)

  const coreInfo = coreItem?.timeFixed ? `${coreItem.startTime} 시작` : '운영시간 정보 없음'
  const noMeal = !lunch && !dinner
  const noDate = dates.length === 0
  const tooManyDates = dates.length > 31
  const mealType = lunch && dinner ? 'BOTH' : (lunch ? 'LUNCH' : 'DINNER')
  const today = earliestTripDate()
  const eventStart = selectedEvent?.startDate
  const eventEnd = selectedEvent?.endDate

  // "전체"는 분야를 하나도 고르지 않은 상태 — 다른 분야를 누르면 풀리고, "전체"를 누르면 모두 해제
  const categoryValue = categories.length ? categories : [ALL]
  const changeCategories = (next) => {
    const added = next.find((value) => !categoryValue.includes(value))
    setCategories(added === ALL || next.length === 0 ? [] : next.filter((value) => value !== ALL))
  }

  const submit = () => {
    const common = {
      headcount,
      transportMode,
      mealType,
      lunchFoodPreference: lunchFood,
      dinnerFoodPreference: dinnerFood,
      includeCafe,
      useAi,
    }
    if (conditionMode) {
      onSubmit({
        ...common,
        availableDates: dates,
        categories,
        district: district === ALL_DISTRICTS ? null : district,
        freeYn: freeOnly,
      })
      return
    }
    onSubmit({ ...common, visitDate: dates[0] })
  }

  return (
    <PlanModal title="어떤 하루를 만들어 볼까요?" onClose={busy ? undefined : onClose}>
      <fieldset disabled={busy} className="plan-conditions__fields">
      <p className="tp-modal__desc">
        {conditionMode
          ? (mode === 'create' ? '갈 수 있는 날과 취향을 고르면 행사와 맛집을 묶어 하루 일정을 만들어요.' : '조건을 바꾸면 조건에 맞는 행사와 맛집으로 일정을 다시 만들어요.')
          : `선택한 행사 · ${selectedEvent?.title ?? ''} · ${coreInfo}`}
      </p>

      <section className="plan-conditions__section" aria-label="여행 날짜">
        <p className="plan-conditions__label">
          {conditionMode ? '갈 수 있는 날' : '여행 날짜'}
          <small>{conditionMode ? '여러 날을 고르면 그중 하루에 맞는 행사를 찾아요' : '행사 기간 안에서 골라 주세요'}</small>
        </p>
        <DateCalendar
          value={dates}
          onChange={setDates}
          multiple={conditionMode}
          min={conditionMode ? today : (eventStart && eventStart > today ? eventStart : today)}
          max={conditionMode ? undefined : eventEnd}
          label={conditionMode ? '갈 수 있는 날' : '여행 날짜'}
        />
      </section>

      {conditionMode && (
        <section className="plan-conditions__section" aria-label="행사 조건">
          <p className="plan-conditions__label">행사 조건</p>
          <ChoiceChips label="행사 분야" options={CATEGORY_OPTIONS} value={categoryValue} onChange={changeCategories} multiple />
          <ExpandSection title="서울 지역" value={district}>
            <ChoiceChips label="서울 지역" options={DISTRICT_OPTIONS} value={district} onChange={setDistrict} />
          </ExpandSection>
          <CheckCard
            checked={freeOnly}
            onChange={setFreeOnly}
            title="무료 행사만"
            description={freeOnly ? '무료 행사 중에서만 골라요' : '무료·유료 행사를 모두 봐요'}
          />
        </section>
      )}

      <section className="plan-conditions__section" aria-label="여행 방식">
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
      </section>

      {mode !== 'create' && <CheckCard
        checked={useAi}
        onChange={setUseAi}
        title="AI 추천받기"
        description="아직 추천 결과에는 반영되지 않아요 (항상 AI 추천 기준으로 만들어요)"
      />}

      {error && <p className="tp-modal__error" role="alert">{error}</p>}
      {busy && <p role="status">조건에 맞는 하루 일정을 만들고 있어요. 잠시 기다려 주세요.</p>}
      {tooManyDates && <p className="tp-modal__error" role="alert">날짜는 최대 31일까지 선택해 주세요.</p>}
      {noDate && <p className="tp-modal__error" role="alert">날짜를 하루 이상 골라 주세요.</p>}
      {noMeal && <p className="tp-modal__error" role="alert">점심과 저녁 중 하나 이상 켜 주세요.</p>}

      <button
        className="tp-btn tp-btn--primary tp-btn--block tp-btn--bold"
        type="button"
        disabled={busy || noMeal || noDate || tooManyDates}
        onClick={submit}
      >
        {busy ? '일정 만드는 중…' : (useAi ? 'AI로 하루 일정 만들기' : '하루 일정 만들기')}
      </button>
      </fieldset>
    </PlanModal>
  )
}
