import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getEventDetail } from '../services/eventService.js'
import EventCreateModal from '../components/layout/event/EventCreateModal.jsx'
import FavoriteButton from '../components/FavoriteButton.jsx'
import HomeNavigation from '../features/home/components/HomeNavigation.jsx'
import SiteFooter from '../components/layout/SiteFooter.jsx'
import '../styles/event-base.css'
import '../styles/eventdetail.css'
import EventSavedPlanModal from '../components/plan/EventSavedPlanModal.jsx'
import Modal from '../components/common/Modal.jsx'

// null, 빈 문자열 등 표시할 정보가 없으면 같은 문구를 사용한다.
function displayText(value) {
  return value == null || String(value).trim() === ''
    ? '정보 없음'
    : value
}

function displayDate(event) {
  if (event.startDate && event.endDate) {
    return event.startDate === event.endDate
      ? event.startDate
      : `${event.startDate} ~ ${event.endDate}`
  }

  return displayText(event.startDate ?? event.endDate)
}

function displayTime(event) {
  const start = event.startTime?.slice(0, 5)
  const end = event.endTime?.slice(0, 5)

  if (start && end) return `${start} ~ ${end}`
  if (start || end) return start ?? end
  return '정보 없음'
}

function getOfficialUrl(homepage) {
  if (!homepage) return null

  try {
    const url = new URL(homepage)
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null
  } catch {
    return null
  }
}

export default function EventDetailPage() {
  const { eventId } = useParams()
  const navigate = useNavigate()

  const [event, setEvent] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [imageFailed, setImageFailed] = useState(false)
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [savedPlanModalOpen, setSavedPlanModalOpen] = useState(false)
  const [officialConfirmOpen, setOfficialConfirmOpen] = useState(false)

  useEffect(() => {
    let active = true

    async function loadEvent() {
      setLoading(true)
      setError('')
      setEvent(null)
      setImageFailed(false)

      try {
        const data = await getEventDetail(eventId)
        if (active) setEvent(data)
      } catch (requestError) {
        if (active) setError(requestError.message)
      } finally {
        if (active) setLoading(false)
      }
    }

    loadEvent()

    // 주소가 바뀌거나 화면을 떠난 뒤에는 이전 요청 결과를 반영하지 않는다.
    return () => {
      active = false
    }
  }, [eventId])

  if (loading) {
    return <main><p role="status">행사 정보를 불러오는 중입니다.</p></main>
  }

  if (error) {
    return <main><p role="alert">{error}</p></main>
  }

  if (!event) {
    return <main><p>행사 정보가 없습니다.</p></main>
  }

  const officialUrl = getOfficialUrl(event.homepage)

  return (
    <div className="event-page">
      <HomeNavigation enableSearch />

      <main>
        <header className="event-heading">
          <p>
            {displayText(event.category)} / {displayText(event.district)}
          </p>
          <h1>{displayText(event.title)}</h1>
          <p>{displayText(event.overview)}</p>
        </header>

        <div className="event-detail-body">
          <section className="event-detail-content" aria-label="행사 소개">
            {event.imageUrl && !imageFailed ? (
              <img
                src={event.imageUrl}
                alt={`${displayText(event.title)} 행사 이미지`}
                onError={() => setImageFailed(true)}
              />
            ) : (
              <p>이미지 정보 없음</p>
            )}

            <h2>{displayText(event.title)}</h2>
            <p>{displayText(event.overview)}</p>

            <h3>방문 전 확인해 주세요</h3>
            <p>입장 방법과 운영 여부는 공식 안내 확인이 필요합니다.</p>
            <p>날씨나 운영 사정에 따라 일정이 달라질 수 있습니다.</p>

            {officialUrl ? (
              <a
                className="tp-btn tp-btn--secondary"
                href={officialUrl}
                target="_blank"
                rel="noopener noreferrer"
                onClick={(clickEvent) => {
                  // 브라우저 기본 확인 창은 환경에 따라 바로 '취소'가 되어 이동이 막힌다 → 화면 안 확인 창 (EVENT-002)
                  clickEvent.preventDefault()
                  setOfficialConfirmOpen(true)
                }}
              >
                공식 안내 확인
              </a>
            ) : (
              <button className="tp-btn tp-btn--secondary" type="button" disabled>
                공식 안내 링크가 없습니다
              </button>
            )}
          </section>

          <aside className="event-detail-info" aria-label="행사 정보">
            <div className="event-detail-info__top">
              <h2>행사 정보</h2>
              <FavoriteButton
                className="event-favorite"
                eventContentId={event.eventId}
                initialFavorite={event.favorited === true}
              />
            </div>
            <p>날짜: {displayDate(event)}</p>
            <p>시간: {displayTime(event)}</p>
            <p>지역: {displayText(event.district)}</p>
            <p>장소: {displayText(event.place)}</p>
            <p>요금: {displayText(event.fee)}</p>

            <hr />

            <section aria-labelledby="event-plan-heading">
              <h2 id="event-plan-heading">이 행사에 맞춘 하루 여행</h2>
              <p>
                음식 종류와 이동 방식을 선택하면<br />
                행사 전후에 들를 맛집을 추천해 드려요.
              </p>

              <button
                className="tp-btn tp-btn--primary tp-btn--block"
                type="button"
                onClick={() => setCreateModalOpen(true)}
              >
                이 행사로 일정 만들기
              </button>

              <button
                className="tp-btn tp-btn--secondary tp-btn--block event-add-plan-button"
                type="button"
                onClick={() => setSavedPlanModalOpen(true)}
              >
                저장한 일정에 추가
              </button>
            </section>
          </aside>
        </div>
      </main>

      <SiteFooter />

      {createModalOpen && (
        <EventCreateModal
          event={event}
          onClose={() => setCreateModalOpen(false)}
          onCreated={(draft) => navigate('/trips/draft', {
            state: { draftId: draft.draftId },
          })}
        />
      )}
      {officialConfirmOpen && officialUrl && (
        <Modal title="공식 안내로 이동할까요?" onClose={() => setOfficialConfirmOpen(false)}>
          <p className="tp-modal__desc">공식 안내 페이지가 새 탭에서 열립니다.</p>
          <button
            className="tp-btn tp-btn--primary tp-btn--block"
            type="button"
            onClick={() => {
              window.open(officialUrl, '_blank', 'noopener,noreferrer')
              setOfficialConfirmOpen(false)
            }}
          >
            새 탭에서 열기
          </button>
          <button className="tp-btn tp-btn--secondary tp-btn--block" type="button" onClick={() => setOfficialConfirmOpen(false)}>
            취소
          </button>
        </Modal>
      )}
      {savedPlanModalOpen && (
        <EventSavedPlanModal
          event={event}
          onClose={() => setSavedPlanModalOpen(false)}
          onChoose={(planId) => {
            navigate(`/my-trips/${planId}`, { state: { addedEventId: event.eventId } })
          }}
        />
      )}
    </div>
  )
}
