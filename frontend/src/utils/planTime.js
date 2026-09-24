/*
 * 나의 일정 — 시간 계산·검증 도우미
 * 화면(버튼 막기)과 목업 API(저장 검증)가 같은 규칙을 쓰도록 여기 모아 둔다.
 * 규칙 기준: docs/07_나의일정_API_계약.md 의 V1~V11
 */

export const DEFAULT_VISIT_START = '10:00'
export const DEFAULT_VISIT_END = '21:00'
export const TITLE_MAX_LENGTH = 100
export const EVENT_LIMIT = 2
export const MINUTE_STEP = 5

export function toMinutes(time) {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

export function toTime(totalMinutes) {
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
}

export function getEndTime(item) {
  return toTime(toMinutes(item.startTime) + item.durationMin)
}

/** "14:00" → "오후 2:00" */
export function formatKoreanTime(time) {
  const totalMinutes = toMinutes(time)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  const period = hours < 12 ? '오전' : '오후'
  const displayHour = hours % 12 === 0 ? 12 : hours % 12
  return `${period} ${displayHour}:${String(minutes).padStart(2, '0')}`
}

/** "2026-09-20" → "2026.09.20" */
export function formatDate(date) {
  return date ? date.replaceAll('-', '.') : ''
}

/** 시작 시각 순으로 정렬하고 seq를 1부터 다시 매긴다. */
export function sortItems(items) {
  return [...items]
    .sort((a, b) => toMinutes(a.startTime) - toMinutes(b.startTime))
    .map((item, index) => ({ ...item, seq: index + 1 }))
}

/**
 * 시간 충돌을 찾는다. 없으면 null.
 * 반환: { title, message, itemKey } — itemKey는 사용자가 방금 고친 항목을 다시 열 때 쓴다.
 */
export function findTimeConflict(items, { visitStartTime, visitEndTime } = {}) {
  const windowStart = toMinutes(visitStartTime ?? DEFAULT_VISIT_START)
  const windowEnd = toMinutes(visitEndTime ?? DEFAULT_VISIT_END)
  const sorted = sortItems(items)

  for (const item of sorted) {
    const start = toMinutes(item.startTime)
    const end = start + item.durationMin

    if (start < windowStart || end > windowEnd) {
      return {
        title: '방문 가능 시간을 벗어나요',
        message: `${toTime(windowStart)}–${toTime(windowEnd)} 안에서 방문 시간을 정해 주세요.`,
        itemKey: item.key,
      }
    }
  }

  for (let index = 1; index < sorted.length; index += 1) {
    const previous = sorted[index - 1]
    const current = sorted[index]

    if (toMinutes(getEndTime(previous)) <= toMinutes(current.startTime)) {
      continue
    }

    const event = [previous, current].find((item) => item.type === 'EVENT')
    const other = event === previous ? current : previous

    if (event) {
      const message = other === previous
        ? `${event.startTime} 행사에 도착할 수 있도록 방문 종료 시간을 앞당겨 주세요.`
        : `행사가 끝나는 ${getEndTime(event)} 이후로 방문 시간을 옮겨 주세요.`
      return { title: '행사 시간과 겹쳐요', message, itemKey: other.key }
    }

    return {
      title: '다른 일정과 겹쳐요',
      message: `${previous.name}(${previous.startTime}–${getEndTime(previous)})과 겹치지 않도록 시간을 조정해 주세요.`,
      itemKey: current.key,
    }
  }

  return null
}

/**
 * 저장 전 검증 (서버 V1~V11과 같은 순서). 통과하면 null, 아니면 { code, title, message }.
 */
export function validatePlan({ title, anchorEventId, items, visitStartTime, visitEndTime }) {
  const trimmedTitle = (title ?? '').trim()

  if (!trimmedTitle || trimmedTitle.length > TITLE_MAX_LENGTH) {
    return { code: 'INVALID_INPUT', title: '일정 이름을 확인해 주세요', message: `일정 이름은 1~${TITLE_MAX_LENGTH}자로 입력해 주세요.` }
  }

  if (items.length === 0) {
    return { code: 'INVALID_INPUT', title: '일정이 비어 있어요', message: '장소나 행사를 하나 이상 추가해 주세요.' }
  }

  const events = items.filter((item) => item.type === 'EVENT')

  if (!events.some((item) => item.contentId === anchorEventId)) {
    return { code: 'PLAN_ANCHOR_REQUIRED', title: '기준 행사가 필요해요', message: '기준 문화행사가 필요합니다. 다시 추천받거나 다른 행사를 선택해 주세요.' }
  }

  if (events.length > EVENT_LIMIT) {
    return { code: 'PLAN_EVENT_LIMIT', title: '행사는 2개까지예요', message: '문화행사는 일정당 최대 2개까지 추가할 수 있습니다.' }
  }

  const contentIds = items.map((item) => `${item.type}:${item.contentId}`)

  if (new Set(contentIds).size !== contentIds.length) {
    return { code: 'PLAN_ITEM_DUPLICATED', title: '이미 있는 장소예요', message: '이미 일정에 있는 장소입니다.' }
  }

  if (items.some((item) => !(item.durationMin > 0) || (item.aiReason?.length ?? 0) > 100)) {
    return { code: 'INVALID_INPUT', title: '입력값을 확인해 주세요', message: '머무는 시간과 추천 이유를 확인해 주세요.' }
  }

  const conflict = findTimeConflict(items, { visitStartTime, visitEndTime })

  if (conflict) {
    return { code: 'PLAN_TIME_CONFLICT', ...conflict }
  }

  return null
}

/**
 * 새 장소를 넣을 첫 빈 시간을 찾는다(5분 단위). 없으면 null.
 */
export function findFreeSlot(items, durationMin, { visitStartTime, visitEndTime } = {}) {
  const windowStart = toMinutes(visitStartTime ?? DEFAULT_VISIT_START)
  const windowEnd = toMinutes(visitEndTime ?? DEFAULT_VISIT_END)
  const alignStep = (minutes) => Math.ceil(minutes / MINUTE_STEP) * MINUTE_STEP
  let cursor = windowStart

  for (const item of sortItems(items)) {
    const start = toMinutes(item.startTime)

    if (start - alignStep(cursor) >= durationMin) {
      return toTime(alignStep(cursor))
    }

    cursor = Math.max(cursor, start + item.durationMin)
  }

  return alignStep(cursor) + durationMin <= windowEnd ? toTime(alignStep(cursor)) : null
}

/** 두 좌표 사이 직선거리(m). 좌표가 없으면 null. */
export function distanceMeters(from, to) {
  if ([from?.lat, from?.lng, to?.lat, to?.lng].some((value) => value == null)) {
    return null
  }

  const earthRadius = 6371000
  const toRadian = (degree) => (degree * Math.PI) / 180
  const deltaLat = toRadian(to.lat - from.lat)
  const deltaLng = toRadian(to.lng - from.lng)
  const a = Math.sin(deltaLat / 2) ** 2
    + Math.cos(toRadian(from.lat)) * Math.cos(toRadian(to.lat)) * Math.sin(deltaLng / 2) ** 2

  return Math.round(2 * earthRadius * Math.asin(Math.sqrt(a)))
}

export function formatDistance(meters) {
  if (meters == null) {
    return null
  }

  return meters < 1000 ? `${Math.round(meters / 10) * 10}m` : `${(meters / 1000).toFixed(1)}km`
}

/**
 * 저장을 막지는 않지만 알려야 할 사항 (계약서 warnings[]와 같은 모양).
 * 음식점 영업시간이 없거나, 방문 시간이 영업시간 밖이면 안내한다.
 */
export function getPlanWarnings(items) {
  return items
    .filter((item) => item.type === 'PLACE')
    .flatMap((item) => {
      if (!item.openTime || !item.closeTime) {
        return [{ itemSeq: item.seq, code: 'HOURS_UNKNOWN', message: `${item.name} · 영업시간 정보가 없어 방문 전 확인이 필요합니다.` }]
      }

      const start = toMinutes(item.startTime)
      const end = start + item.durationMin

      if (start < toMinutes(item.openTime) || end > toMinutes(item.closeTime)) {
        return [{ itemSeq: item.seq, code: 'OUTSIDE_HOURS', message: `${item.name} · 영업시간(${item.openTime}–${item.closeTime}) 밖 방문입니다.` }]
      }

      return []
    })
}
