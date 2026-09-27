import React from 'react'
import EventCard from './EventCard.jsx'

export default function EventSection({
  id,
  title,
  events,
  favoriteIds,
  onFavorite,
  onDetail,
  featured = false,
  loading = false,
  error = false,
  errorMessage,
  onRetry,
  hint,
  onMore,
}) {
  const headingId = `${id || title.replaceAll(' ', '-')}-title`

  return (
    <section className="event-section" id={id} aria-labelledby={headingId} aria-busy={loading}>
      <div className="event-section__heading">
        <h2 id={headingId}>{title}</h2>
        <div className="event-section__heading-actions">
          {hint && !loading && !error && <span>{hint}</span>}
          {onMore && <button className="event-section__more" type="button" onClick={onMore} disabled={loading || error} aria-label={`${title} 더 보기`}>더 보기 ›</button>}
        </div>
      </div>

      <div className={`event-section__content${featured ? ' event-section__content--featured' : ''}`}>
        {loading ? (
          <div className={`event-section__list event-section__list--loading${featured ? ' event-section__list--featured' : ''}`} aria-hidden="true">
            {[0, 1, 2].map((index) => (
              <div className="event-card-skeleton" key={index}>
                <div className="event-card-skeleton__image" />
                <div className="event-card-skeleton__line event-card-skeleton__line--short" />
                <div className="event-card-skeleton__line event-card-skeleton__line--title" />
                <div className="event-card-skeleton__line" />
                <div className="event-card-skeleton__button" />
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="event-section__empty">
            <strong role={errorMessage ? 'alert' : undefined}>{errorMessage || '행사 정보를 불러오지 못했습니다.'}</strong>
            {onRetry && <button className="event-section__retry" type="button" onClick={onRetry}>다시 시도</button>}
          </div>
        ) : events.length > 0 ? (
          <div className={`event-section__list${featured ? ' event-section__list--featured' : ''}`}>
            {events.map((event) => (
              <EventCard
                key={event.eventId}
                event={event}
                featured={featured}
                isFavorite={favoriteIds.has(event.eventId)}
                onFavorite={() => onFavorite(event.eventId)}
                onDetail={() => onDetail(event.eventId)}
              />
            ))}
          </div>
        ) : (
          <div className="event-section__empty">
            <strong>해당 월의 행사 정보가 없습니다.</strong>
            <span>다른 월을 선택해 문화행사를 찾아보세요.</span>
          </div>
        )}
      </div>
    </section>
  )
}
