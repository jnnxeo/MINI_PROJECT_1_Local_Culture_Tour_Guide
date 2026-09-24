import React, { useEffect, useState } from 'react'
import Modal from '../common/Modal.jsx'
import { searchPlaces } from '../../services/planService.js'
import { formatDistance } from '../../utils/planTime.js'
import { withObject } from '../../utils/korean.js'

const COPY = {
  add: {
    title: '일정에 장소 추가',
    description: '행사 주변 맛집을 검색해 일정에 추가합니다. 관광지는 추천 대상이 아닙니다.',
    hint: '장소를 선택해 주세요',
    confirm: '선택한 장소 추가',
  },
  change: {
    title: '장소 변경',
    description: '현재 항목을 다른 맛집 후보로 교체합니다.',
    hint: '교체할 장소를 선택해 주세요',
    confirm: '이 장소로 교체',
  },
}

function describePlace(place) {
  const parts = ['맛집', place.district]
  const distance = formatDistance(place.distanceM)

  if (distance) {
    parts.push(`행사장에서 약 ${distance}`)
  }

  if (!place.openTime) {
    parts.push('영업시간 정보 없음')
  } else if (place.openAtRequestedTime === false) {
    parts.push('이 시간엔 영업하지 않아요')
  }

  return parts.filter(Boolean).join(' · ')
}

/** Figma "place · 팝업"(추가) / "placechange · 팝업"(변경) */
export default function PlaceSearchModal({ mode, eventId, usedContentIds, time, durationMin, onConfirm, onClose }) {
  const copy = COPY[mode]
  const [keyword, setKeyword] = useState('')
  const [state, setState] = useState({ status: 'loading', places: [], message: '' })
  const [selectedId, setSelectedId] = useState(null)
  const [retryCount, setRetryCount] = useState(0)

  useEffect(() => {
    let ignore = false
    setState((previous) => ({ ...previous, status: 'loading' }))

    const timeoutId = window.setTimeout(async () => {
      try {
        const { items } = await searchPlaces({ eventId, keyword, time, durationMin })

        if (!ignore) {
          setState({ status: 'done', places: items, message: '' })
        }
      } catch (error) {
        if (!ignore) {
          setState({ status: 'error', places: [], message: error.message })
        }
      }
    }, 250)

    return () => {
      ignore = true
      window.clearTimeout(timeoutId)
    }
  }, [eventId, keyword, time, durationMin, retryCount])

  const selectedPlace = state.places.find((place) => place.contentId === selectedId)

  return (
    <Modal title={copy.title} variant="list" onClose={onClose}>
      <p className="tp-modal__desc">{copy.description}</p>

      <label className="tp-field">
        <span className="tp-field__label">장소 검색</span>
        <input
          type="search"
          value={keyword}
          placeholder="장소 이름이나 지역 (예: 서촌)"
          onChange={(event) => setKeyword(event.target.value)}
        />
      </label>

      {state.status === 'error' && (
        <div className="plan-place-state">
          <p className="tp-modal__error">{state.message}</p>
          <button className="tp-btn tp-btn--secondary" type="button" onClick={() => setRetryCount((count) => count + 1)}>
            다시 시도
          </button>
        </div>
      )}

      {state.status === 'done' && state.places.length === 0 && (
        <p className="plan-place-state">검색 결과가 없어요. 다른 이름이나 지역으로 검색해 보세요.</p>
      )}

      {state.status === 'loading' && state.places.length === 0 && (
        <p className="plan-place-state" aria-live="polite">장소를 찾고 있어요…</p>
      )}

      <ul className="plan-place-list" aria-busy={state.status === 'loading'}>
        {state.places.map((place) => {
          const isUsed = usedContentIds.includes(place.contentId)
          const isSelected = place.contentId === selectedId

          return (
            <li key={place.contentId} className={`plan-place${isSelected ? ' is-selected' : ''}`}>
              <div className="plan-place__photo">
                {place.imageUrl ? <img src={place.imageUrl} alt="" /> : <span>사진 없음</span>}
              </div>
              <p className="plan-place__name">{place.name}</p>
              <p className="plan-place__meta">{describePlace(place)}</p>
              <button
                className={`tp-btn ${isSelected ? 'tp-btn--primary' : 'tp-btn--secondary'} plan-place__select`}
                type="button"
                disabled={isUsed}
                aria-pressed={isSelected}
                onClick={() => setSelectedId(place.contentId)}
              >
                {isUsed ? '이미 일정에 있어요' : isSelected ? '선택됨' : '이 장소 선택'}
              </button>
            </li>
          )
        })}
      </ul>

      <p className="tp-modal__hint" aria-live="polite">
        {selectedPlace ? `${withObject(selectedPlace.name)} 선택했어요` : copy.hint}
      </p>

      <button
        className="tp-btn tp-btn--primary tp-btn--bold tp-btn--block"
        type="button"
        disabled={!selectedPlace}
        onClick={() => onConfirm(selectedPlace)}
      >
        {copy.confirm}
      </button>
    </Modal>
  )
}
