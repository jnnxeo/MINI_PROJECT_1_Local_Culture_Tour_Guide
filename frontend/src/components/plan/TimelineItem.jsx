import React from 'react'
import { formatDistance } from '../../utils/planTime.js'

/** 카드 제목: "HH:mm  이름" (Figma). 시작 시간이 정해지지 않은 행사는 이름만 */
export function getItemHeading(item) {
  if (item.type === 'EVENT' && !item.timeFixed) {
    return item.name
  }

  return `${item.startTime}  ${item.name}`
}

export default function TimelineItem({
  item,
  isCore,
  reason,
  distanceToNext,
  isLast,
  isSelected,
  locked = false,
  onEditTime,
  onRemove,
  onShowOnMap,
}) {
  const isEvent = item.type === 'EVENT'
  const distanceText = formatDistance(distanceToNext)

  return (
    <article
      className={`plan-item${isEvent ? ' plan-item--event' : ''}${isSelected ? ' is-selected' : ''}`}
      aria-label={`${item.sequence}번째 일정 ${item.name}`}
    >
      <div className="plan-item__photo">
        {item.imageUrl
          ? <img src={item.imageUrl} alt="" />
          : <span className="plan-item__photo-empty">사진 정보 없음</span>}
      </div>

      <h3 className="plan-item__title">{getItemHeading(item)}</h3>

      <p className="plan-item__meta">
        {isEvent
          ? `${isCore ? '선택한 행사' : '추가한 행사'} · ${item.timeFixed ? '시작 시간 고정' : '운영시간 정보 없음'} · ${item.durationMin}분`
          : `${item.durationMin}분 · 맛집`}
      </p>

      {isEvent ? (
        <p className="plan-item__event-note">행사 시간을 기준으로 주변 일정을 조정합니다.</p>
      ) : (
        <div className="plan-item__actions">
          <button className="tp-btn tp-btn--secondary plan-item__action" type="button" disabled={locked} onClick={onEditTime}>
            시간 변경
          </button>
          <button className="tp-btn tp-btn--danger plan-item__remove" type="button" disabled={locked} onClick={onRemove}>
            삭제
          </button>
        </div>
      )}

      {reason && <p className="plan-item__reason">{`추천 기준 · ${reason}`}</p>}

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
