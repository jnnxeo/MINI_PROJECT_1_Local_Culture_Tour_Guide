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
  hint,
  onMore,
}) {
  const headingId = `${id || title.replaceAll(' ', '-')}-title`

  return (
    <section className="event-section" id={id} aria-labelledby={headingId}>
      <div className="event-section__heading">
        <h2 id={headingId}>{title}</h2>
        <div className="event-section__heading-actions">
          {hint && <span>{hint}</span>}
          {onMore && <button className="event-section__more" type="button" onClick={onMore} aria-label={`${title} 더 보기`}>더 보기 ›</button>}
        </div>
      </div>

      {events.length > 0 ? (
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
    </section>
  )
}
