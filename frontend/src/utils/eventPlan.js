import { findFreeSlot, findTimeConflict, getEndTime, toMinutes } from './planTime.js'

export function buildEventPlanItem(plan, event) {
  const items = plan.items ?? []
  const tripDate = plan.tripDate ?? plan.visitDate

  if (plan.saved === false) {
    return { error: '저장한 일정만 선택할 수 있습니다.' }
  }

  if (items.some((item) =>
    item.type === 'EVENT' && item.contentId === event.eventId
  )) {
    return { error: '이미 이 행사가 들어 있는 일정입니다.' }
  }

  if (items.filter((item) => item.type === 'EVENT').length >= 2) {
    return { error: '한 일정에는 문화행사를 2개까지만 넣을 수 있습니다.' }
  }

  if (!tripDate || tripDate < event.startDate || tripDate > event.endDate) {
    return { error: '일정 날짜가 행사 기간과 맞지 않습니다.' }
  }

  const fixedStart = event.startTime?.slice(0, 5)
  const officialEnd = event.endTime?.slice(0, 5)
  const startTime = fixedStart ?? findFreeSlot(items, 60, {
    startTime: plan.visitStartTime,
    endTime: plan.visitEndTime,
  })
  if (!startTime) return { error: '행사를 배치할 빈 시간이 없습니다.' }
  const durationMin = officialEnd && fixedStart
    ? toMinutes(officialEnd) - toMinutes(fixedStart)
    : 60
  if (durationMin <= 0) return { error: '행사 시간 정보를 확인해 주세요.' }
  const endTime = getEndTime({ startTime, durationMin })

  const item = {
    itemId: null,
    type: 'EVENT',
    contentId: event.eventId,
    name: event.title,
    category: event.category,
    address: event.place,
    lat: event.lat,
    lng: event.lng,
    imageUrl: event.imageUrl,
    startTime,
    durationMin,
    endTime,
    timeFixed: Boolean(fixedStart),
    aiReason: null,
    openTime: null,
    closeTime: null,
  }

  const conflict = findTimeConflict([...items, item], {
    startTime: plan.visitStartTime,
    endTime: plan.visitEndTime,
  })

  if (conflict) return { error: conflict.message }
  return { item }
}
