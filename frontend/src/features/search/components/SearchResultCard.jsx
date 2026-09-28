import React, { useState } from 'react'

function formatDate(date) {
  if (!date) return null
  const [year, month, day] = date.split('-')
  return year && month && day ? `${year}.${month}.${day}` : null
}

function formatPeriod(startDate, endDate) {
  const start = formatDate(startDate)
  const end = formatDate(endDate)
  if (!start || !end) return '일시 정보 없음'
  return start === end ? start : `${start} ~ ${end}`
}

function formatFee(event) {
  if (event.freeYn === true) return '무료'
  if (event.freeYn === false) return event.fee ? `유료 · ${event.fee}` : '유료 · 요금 정보 없음'
  return '요금 정보 없음'
}

export default function SearchResultCard({ event, onDetail, isFavorite = false, favoritePending = false, onFavorite }) {
  const [failedImageUrl, setFailedImageUrl] = useState(null)
  const [landscapeImageUrl, setLandscapeImageUrl] = useState(null)
  const showImage = Boolean(event.imageUrl) && event.imageUrl !== failedImageUrl

  return (
    <article className="search-result-card">
      <div className={`search-result-card__image search-result-card__image--${event.imageTone || 'default'}`}>
        {showImage
          ? <img
              src={event.imageUrl}
              alt=""
              loading="lazy"
              className={event.imageUrl === landscapeImageUrl ? 'search-result-card__image-photo--landscape' : undefined}
              onLoad={(imageEvent) => {
                const image = imageEvent.currentTarget
                setLandscapeImageUrl(image.naturalWidth >= image.naturalHeight ? event.imageUrl : null)
              }}
              onError={() => setFailedImageUrl(event.imageUrl)}
            />
          : <span aria-hidden="true">TripAI</span>}
        <button
          type="button"
          className={`search-result-card__favorite${isFavorite ? ' search-result-card__favorite--active' : ''}`}
          onClick={onFavorite}
          disabled={!onFavorite || favoritePending}
          aria-label={`${event.title} ${isFavorite ? '관심 행사 해제' : '관심 행사 저장'}`}
          aria-pressed={isFavorite}
        >
          {isFavorite ? '♥' : '♡'}
        </button>
      </div>
      <div className="search-result-card__body">
        <p className="search-result-card__category">{event.category || '분야 정보 없음'} · {event.district || '지역 정보 없음'}</p>
        <h3>{event.title}</h3>
        <p className="search-result-card__period">{formatPeriod(event.startDate, event.endDate)}</p>
        <p className={`search-result-card__fee${event.freeYn === true ? ' search-result-card__fee--free' : ''}`}>{formatFee(event)}</p>
      </div>
      <button className="primary-button search-result-card__detail" type="button" onClick={onDetail}>상세 보기</button>
    </article>
  )
}
