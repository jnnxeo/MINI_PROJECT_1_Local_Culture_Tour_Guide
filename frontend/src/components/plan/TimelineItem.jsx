import React from 'react'
import { formatDistance, toMinutes } from '../../utils/planTime.js'

const LUNCH = [toMinutes('11:00'), toMinutes('14:30')]
const DINNER = [toMinutes('17:00'), toMinutes('20:30')]

/** 식사 장소는 시간대에 따라 "점심 · " / "저녁 · "을 붙인다 (Figma: "12:30  점심 · 행사장 근처 한식당") */
export function getItemLabel(item) {
  if (item.type !== 'PLACE' || item.category !== '식사') {
    return item.name
  }

  const start = toMinutes(item.startTime)

  if (start >= LUNCH[0] && start < LUNCH[1]) {
    return `점심 · ${item.name}`
  }

  if (start >= DINNER[0] && start < DINNER[1]) {
    return `저녁 · ${item.name}`
  }

  return item.name
}

/** 시간이 정해지지 않은 행사는 Figma처럼 이름만, 나머지는 "HH:mm  이름" */
export function getItemHeading(item) {
  if (item.type === 'EVENT' && !item.timeFixed) {
    return item.name
  }

  return `${item.startTime}  ${getItemLabel(item)}`
}

export default function TimelineItem({
  item,
  isAnchor,
  distanceToNext,
  isLast,
  isSelected,
  onEditTime,
  onChangePlace,
  onRemove,
  onShowOnMap,
}) {
  const isEvent = item.type === 'EVENT'
  const distanceText = formatDistance(distanceToNext)

  return (
    <article
      className={`plan-item${isEvent ? ' plan-item--event' : ''}${isSelected ? ' is-selected' : ''}`}
      aria-label={`${item.seq}번째 일정 ${item.name}`}
    >
      <div className="plan-item__photo">
        {item.imageUrl
          ? <img src={item.imageUrl} alt="" />
          : <span className="plan-item__photo-empty">사진 정보 없음</span>}
      </div>

      <h3 className="plan-item__title">{getItemHeading(item)}</h3>

      <p className="plan-item__meta">
        {isEvent
          ? `${isAnchor ? '선택한 행사' : '추가한 행사'} · ${item.timeFixed ? '시작 시간 고정' : '운영시간 정보 없음'} · ${item.durationMin}분`
          : `${item.durationMin}분 · ${item.category ?? '맛집'}`}
      </p>

      {isEvent ? (
        <p className="plan-item__event-note">행사 시간을 기준으로 주변 일정을 조정합니다.</p>
      ) : (
        <div className="plan-item__actions">
          <button className="tp-btn tp-btn--secondary plan-item__action" type="button" onClick={onEditTime}>
            시간 변경
          </button>
          <button className="tp-btn tp-btn--secondary plan-item__action" type="button" onClick={onChangePlace}>
            장소 변경
          </button>
          <button className="tp-btn tp-btn--danger plan-item__remove" type="button" onClick={onRemove}>
            삭제
          </button>
        </div>
      )}

      {item.aiReason && <p className="plan-item__reason">{`추천 기준 · ${item.aiReason}`}</p>}

      {!isLast && (
        <p className="plan-item__distance">
          {distanceText ? `다음 장소까지 직선거리 약 ${distanceText}` : '다음 장소까지 직선거리 정보 없음'}
        </p>
      )}

      <button className="tp-btn tp-btn--secondary tp-btn--bold plan-item__map" type="button" onClick={onShowOnMap}>
        지도에서 보기
      </button>
    </article>
  )
}
