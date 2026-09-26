import React from 'react'

function formatPeriod(startDate, endDate) {
  if (!startDate || !endDate) return '일시 정보 없음'
  const format = (value) => new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
  }).format(new Date(value))

  const start = new Date(`${startDate}T00:00:00`)
  const end = new Date(`${endDate}T00:00:00`)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return '일시 정보 없음'
  return `${format(start)} ~ ${format(end)}`
}

export default function EventCard({ event, featured, isFavorite, onFavorite, onDetail }) {
  return (
    <article className={`event-card${featured ? ' event-card--featured' : ''}`}>
      <div className={`event-card__image event-card__image--${event.imageTone || 'gallery'}`}>
        {event.imageUrl
          ? <img className="event-card__photo" src={event.imageUrl} alt="" loading="lazy" />
          : <div className="event-card__image-shape" aria-hidden="true" />}
        <button
          className={`event-card__favorite${isFavorite ? ' event-card__favorite--active' : ''}`}
          type="button"
          onClick={onFavorite}
          aria-label={`${event.title} ${isFavorite ? '찜 해제' : '찜하기'}`}
          aria-pressed={isFavorite}
        >
          {isFavorite ? '♥' : '♡'}
        </button>
      </div>

      <p className="event-card__category">{event.category || '분야 정보 없음'} · {event.district || '지역 정보 없음'}</p>
      <h3>{event.title}</h3>
      <p className="event-card__meta">{formatPeriod(event.startDate, event.endDate)}</p>
      <p className={`event-card__fee${event.freeYn === true ? ' event-card__fee--free' : ''}`}>
        {event.freeYn === true ? '무료' : event.freeYn === false ? `유료 · ${event.fee || '요금 정보 없음'}` : '요금 정보 없음'}
      </p>
      <button
        className={`event-card__detail${featured ? '' : ' event-card__detail--primary'}`}
        type="button"
        onClick={onDetail}
      >
        행사 자세히 보기
      </button>
    </article>
  )
}
