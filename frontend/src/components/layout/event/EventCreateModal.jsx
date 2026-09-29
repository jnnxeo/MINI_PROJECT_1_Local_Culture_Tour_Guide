import React, { useRef, useState } from 'react'
import ConditionsModal from '../../plan/ConditionsModal.jsx'
import { earliestTripDate } from '../../common/DateCalendar.jsx'
import { recommendDraft } from '../../../services/planService.js'

/**
 * 행사 상세 "이 행사로 일정 만들기" — 메인 AI 추천·조건 수정과 같은 팝업(ConditionsModal)을 쓴다.
 * 행사를 이미 골랐으므로 행사 조건(분야·지역·무료)은 빼고, 행사 기간 안의 날짜 하루와 이동·식사·카페만 고른다.
 * 요청은 기존과 같은 API-PLAN-001(eventId + visitDate)이며 끼니별 음식 종류·카페만 더해 보낸다.
 */
export default function EventCreateModal({ event, onClose, onCreated }) {
  const earliest = earliestTripDate()
  const firstDate = event.startDate && event.startDate > earliest ? event.startDate : earliest

  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const pending = useRef(false)

  const unavailableReason = !event.startDate || !event.endDate
    || event.startDate > event.endDate
    ? '행사 날짜 정보가 없어 여행 날짜를 선택할 수 없습니다.'
    : event.endDate < firstDate
      ? '종료된 행사는 일정으로 만들 수 없습니다.'
      : event.lat == null || event.lng == null
        ? '행사 위치 정보가 없어 일정을 만들 수 없습니다.'
        : ''

  const coreItem = event.startTime
    ? { timeFixed: true, startTime: event.startTime.slice(0, 5) }
    : null

  async function handleSubmit({ visitDate, headcount, transportMode, mealType, lunchFoodPreference, dinnerFoodPreference, includeCafe }) {
    if (pending.current || unavailableReason) return
    if (!visitDate || visitDate < event.startDate || visitDate > event.endDate) {
      setError('행사 기간 안의 여행 날짜를 선택해 주세요.')
      return
    }

    pending.current = true
    setSaving(true)
    setError('')

    try {
      const draft = await recommendDraft({
        eventId: event.eventId,
        visitDate,
        headcount,
        transportMode,
        mealType,
        lunchFoodPreference,
        dinnerFoodPreference,
        includeCafe,
      })
      onCreated(draft)
    } catch (requestError) {
      setError(requestError.message || '일정을 만들지 못했습니다. 조건을 바꿔 다시 시도해 주세요.')
    } finally {
      pending.current = false
      setSaving(false)
    }
  }

  return (
    <ConditionsModal
      mode="create"
      title="이 행사로 AI 일정 만들기"
      visitDate={firstDate}
      selectedEvent={event}
      coreItem={coreItem}
      busy={saving}
      error={error}
      blockedReason={unavailableReason}
      onSubmit={handleSubmit}
      onClose={() => { if (!pending.current) onClose() }}
    />
  )
}
