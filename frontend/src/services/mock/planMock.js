/*
 * 나의 일정 목업 API — 백엔드 완성 전까지 화면 개발용
 * 기준: 08_TripAI_요구사항정의서 「API 명세」 API-PLAN-001~009, API-PLACE-001
 *  - URL·요청 필드·응답 최상위 필드·오류 코드는 명세 그대로 따른다.
 *  - 명세에 "items:[...]"처럼 모양이 정해지지 않은 부분은 [제안] 표시 (docs/07 참고).
 * 초안은 sessionStorage에 두어 새로고침해도 유지된다(UX-005).
 * 사진은 Figma 시안의 참고 이미지이며 실제 데이터 연결 시 API imageUrl로 대체된다.
 */
import cafeImage from '../../assets/mock/cafe.jpg'
import galleryImage from '../../assets/mock/gallery.jpg'
import hanokImage from '../../assets/mock/hanok.jpg'
import palaceImage from '../../assets/mock/palace.jpg'
import restaurantImage from '../../assets/mock/restaurant.jpg'
import { distanceMeters, sortItems, validatePlan } from '../../utils/planTime.js'

const STORAGE_KEY = 'tripai.mock.plans.v2'
export const MOCK_DRAFT_ID = 12

// 문화행사 (API-EVENT-002 필드 이름 기준)
const EVENTS = {
  'DEV-EV-001': {
    eventId: 'DEV-EV-001',
    title: '고궁의 밤, 달빛 산책',
    category: '전통문화',
    place: '경복궁',
    lat: 37.5796,
    lng: 126.977,
    imageUrl: palaceImage,
    startDate: '2026-09-01',
    endDate: '2026-10-31',
    startTime: '18:00',
    durationMin: 180,
  },
}

// 맛집 (API-PLACE-001 응답 필드 이름 기준)
const RESTAURANTS = [
  { placeId: 'DEV-PL-001', name: '행사장 근처 한식당', addr: '서울 종로구 사직로 125', lat: 37.5758, lng: 126.9731, imageUrl: restaurantImage, openTime: '11:00', breakTime: '15:00~17:00', closeTime: '21:00', firstMenu: '비빔밥' },
  { placeId: 'DEV-PL-002', name: '서촌 동네 카페', addr: '서울 종로구 자하문로 20', lat: 37.5788, lng: 126.9703, imageUrl: cafeImage, openTime: '10:00', breakTime: null, closeTime: '22:00', firstMenu: '핸드드립' },
  { placeId: 'DEV-PL-003', name: '종로 한식당', addr: '서울 종로구 세종대로 175', lat: 37.572, lng: 126.9769, imageUrl: hanokImage, openTime: '07:00', breakTime: null, closeTime: '22:00', firstMenu: '국밥' },
  { placeId: 'DEV-PL-004', name: '한옥 카페', addr: '서울 종로구 삼청로 101', lat: 37.583, lng: 126.9817, imageUrl: restaurantImage, openTime: '10:30', breakTime: null, closeTime: '20:00', firstMenu: '쌍화차' },
  { placeId: 'DEV-PL-005', name: '서촌 베이커리 카페', addr: '서울 종로구 필운대로 12', lat: 37.5795, lng: 126.969, imageUrl: galleryImage, openTime: '08:00', breakTime: null, closeTime: '19:00', firstMenu: '소금빵' },
  { placeId: 'DEV-PL-006', name: '삼청동 칼국수', addr: '서울 종로구 삼청로 110', lat: 37.5835, lng: 126.982, imageUrl: null, openTime: '10:30', breakTime: null, closeTime: '20:00', firstMenu: '칼국수' },
  { placeId: 'DEV-PL-007', name: '영업시간 미상 식당', addr: '서울 종로구 율곡로 50', lat: 37.576, lng: 126.985, imageUrl: null, openTime: null, breakTime: null, closeTime: null, firstMenu: null },
]

// 다시 추천할 때 돌려 쓰는 [점심, 오후] 조합
const COURSES = [
  ['DEV-PL-001', 'DEV-PL-002'],
  ['DEV-PL-003', 'DEV-PL-004'],
  ['DEV-PL-006', 'DEV-PL-005'],
]

const wait = (milliseconds) => new Promise((resolve) => {
  setTimeout(resolve, milliseconds)
})

function mockError(status, message) {
  const error = new Error(message)
  error.status = status
  return error
}

/** [제안] 일정 항목 모양 — 명세는 items:[...]만 정의. PUT 요청 필드(itemId,type,placeId,startTime,durationMin,sequence) + 화면 표시 필드 */
function toRestaurantItem(place, { itemId, startTime, durationMin }) {
  return {
    itemId,
    type: 'PLACE',
    placeId: place.placeId,
    sequence: 0,
    startTime,
    durationMin,
    name: place.name,
    addr: place.addr,
    lat: place.lat,
    lng: place.lng,
    imageUrl: place.imageUrl,
    openTime: place.openTime,
    breakTime: place.breakTime,
    closeTime: place.closeTime,
    timeFixed: false,
  }
}

function toEventItem(event, { itemId }) {
  return {
    itemId,
    type: 'EVENT',
    placeId: event.eventId,
    sequence: 0,
    startTime: event.startTime,
    durationMin: event.durationMin,
    name: event.title,
    addr: event.place,
    lat: event.lat,
    lng: event.lng,
    imageUrl: event.imageUrl,
    openTime: null,
    breakTime: null,
    closeTime: null,
    timeFixed: true,
  }
}

/** [제안] mapPoints 모양 — 방문 순서대로 좌표 */
function toMapPoints(items) {
  return items
    .filter((item) => item.lat != null && item.lng != null)
    .map(({ sequence, lat, lng }) => ({ sequence, lat, lng }))
}

function buildCourse(courseIndex, store) {
  const [lunchId, afternoonId] = COURSES[courseIndex % COURSES.length]
  const nextId = () => {
    store.nextItemId += 1
    return store.nextItemId
  }
  const lunch = RESTAURANTS.find((place) => place.placeId === lunchId)
  const afternoon = RESTAURANTS.find((place) => place.placeId === afternoonId)
  const items = sortItems([
    toRestaurantItem(lunch, { itemId: nextId(), startTime: '12:30', durationMin: 60 }),
    toRestaurantItem(afternoon, { itemId: nextId(), startTime: '14:00', durationMin: 60 }),
    toEventItem(EVENTS['DEV-EV-001'], { itemId: nextId() }),
  ])

  // [제안] recommendationReasons 모양 — 항목별 근거 (AI-007)
  const recommendationReasons = [
    { itemId: items[0].itemId, reason: '행사장 반경 · 식사 시간대 · 영업 중' },
    { itemId: items[1].itemId, reason: '행사장 반경 · 쉬어 가기 좋은 시간대' },
    { itemId: items[2].itemId, reason: '검색 조건 일치' },
  ]

  return { items, recommendationReasons }
}

function createInitialStore() {
  const store = { nextItemId: 100, nextPlanId: 30, drafts: {}, plans: {} }
  const { items, recommendationReasons } = buildCourse(0, store)

  store.drafts[MOCK_DRAFT_ID] = {
    draftId: MOCK_DRAFT_ID,
    title: '고궁의 밤을 기다리는 하루',
    visitDate: '2026-10-03',
    tripType: 'DAY_TRIP',
    selectedEvent: { eventId: 'DEV-EV-001', title: EVENTS['DEV-EV-001'].title },
    conditions: {
      visitDate: '2026-10-03',
      startTime: '11:00',
      endTime: '21:00',
      companion: null,
      foodPreference: null,
      transportMode: 'WALK_TRANSIT',
    },
    items,
    recommendationReasons,
    courseIndex: 0,
    savedPlanId: null,
  }

  return store
}

function readStore() {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : createInitialStore()
  } catch {
    return createInitialStore()
  }
}

function writeStore(store) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(store))
  } catch {
    // 저장소를 쓸 수 없어도 화면은 계속 동작한다.
  }
}

function findDraft(store, draftId) {
  const draft = store.drafts[draftId]

  if (!draft) {
    throw mockError(404, '초안을 찾을 수 없습니다.')
  }

  return draft
}

function coreEventOf(draft) {
  return draft.selectedEvent?.eventId
}

function findCandidate(type, placeId) {
  if (type === 'EVENT') {
    const event = EVENTS[placeId]
    return event ? toEventItem(event, { itemId: null }) : null
  }

  const place = RESTAURANTS.find((candidate) => candidate.placeId === placeId)
  return place ? toRestaurantItem(place, { itemId: null, startTime: '00:00', durationMin: 60 }) : null
}

/** API-PLAN-002 GET /api/plans/drafts/{draftId} */
export async function mockGetDraft(draftId) {
  await wait(300)
  const draft = findDraft(readStore(), draftId)
  const { courseIndex, savedPlanId, ...response } = draft

  // 명세 응답 {draftId,title,visitDate,tripType,items,mapPoints} + [제안] selectedEvent·conditions·recommendationReasons
  return structuredClone({ ...response, mapPoints: toMapPoints(draft.items) })
}

/** API-PLAN-003 PATCH /api/plans/drafts/{draftId}/conditions */
export async function mockUpdateConditions(draftId, conditions) {
  await wait(300)
  const store = readStore()
  const draft = findDraft(store, draftId)

  if (!conditions.visitDate || !conditions.startTime || !conditions.endTime || conditions.startTime >= conditions.endTime) {
    throw mockError(400, '여행 날짜와 방문 시간을 확인해 주세요.')
  }

  draft.conditions = { ...draft.conditions, ...conditions }
  draft.visitDate = conditions.visitDate
  writeStore(store)
  return { draftId: draft.draftId, conditions: structuredClone(draft.conditions) }
}

/** API-PLAN-004 POST /api/plans/drafts/{draftId}/regenerate */
export async function mockRegenerate(draftId) {
  await wait(1800)
  const store = readStore()
  const draft = findDraft(store, draftId)

  if (draft.savedPlanId) {
    throw mockError(409, '이미 저장된 초안입니다.')
  }

  draft.courseIndex += 1
  const { items, recommendationReasons } = buildCourse(draft.courseIndex, store)
  draft.items = items
  draft.recommendationReasons = recommendationReasons
  writeStore(store)

  return structuredClone({
    draftId: draft.draftId,
    title: draft.title,
    tripType: draft.tripType,
    items,
    recommendationReasons,
  })
}

/** API-PLAN-005 PATCH /api/plans/drafts/{draftId}/title */
export async function mockUpdateTitle(draftId, title) {
  await wait(200)
  const store = readStore()
  const draft = findDraft(store, draftId)
  const trimmed = (title ?? '').trim()

  if (!trimmed || trimmed.length > 100) {
    throw mockError(400, '일정 이름은 1~100자로 입력해 주세요.')
  }

  draft.title = trimmed
  writeStore(store)
  return { draftId: draft.draftId, title: trimmed }
}

/** API-PLAN-006 PUT /api/plans/drafts/{draftId}/items — 항목·시간·방문 순서 일괄 수정 */
export async function mockReplaceItems(draftId, requestItems) {
  await wait(300)
  const store = readStore()
  const draft = findDraft(store, draftId)
  const byId = new Map(draft.items.map((item) => [item.itemId, item]))

  const items = requestItems.map((requestItem) => {
    const current = byId.get(requestItem.itemId)

    if (!current) {
      throw mockError(404, '일정 항목을 찾을 수 없습니다.')
    }

    if (current.timeFixed && requestItem.startTime !== current.startTime) {
      throw mockError(400, `${current.name}은(는) 시작 시간이 정해진 행사라 시간을 바꿀 수 없습니다.`)
    }

    return { ...current, startTime: requestItem.startTime, durationMin: requestItem.durationMin, sequence: requestItem.sequence }
  })

  const error = validatePlan({ title: draft.title, items, coreEventPlaceId: coreEventOf(draft), ...draft.conditions })

  if (error) {
    throw mockError(error.status, error.message)
  }

  draft.items = sortItems(items)
  writeStore(store)
  return structuredClone({ draftId: draft.draftId, items: draft.items, mapPoints: toMapPoints(draft.items) })
}

/** API-PLAN-007 POST /api/plans/drafts/{draftId}/items — 맛집 일정에 추가 */
export async function mockAddItem(draftId, { placeId, type, startTime, durationMin }) {
  await wait(300)
  const store = readStore()
  const draft = findDraft(store, draftId)
  const candidate = findCandidate(type, placeId)

  if (!candidate) {
    throw mockError(404, '장소를 찾을 수 없습니다.')
  }

  if (draft.items.some((item) => item.type === type && item.placeId === placeId)) {
    throw mockError(409, '이미 일정에 있는 장소입니다.')
  }

  store.nextItemId += 1
  const newItem = { ...candidate, itemId: store.nextItemId, startTime, durationMin }
  const items = [...draft.items, newItem]
  const error = validatePlan({ title: draft.title, items, coreEventPlaceId: coreEventOf(draft), ...draft.conditions })

  if (error) {
    throw mockError(error.status === 400 ? 422 : error.status, error.message)
  }

  draft.items = sortItems(items)
  writeStore(store)
  return structuredClone({ itemId: newItem.itemId, items: draft.items, mapPoints: toMapPoints(draft.items) })
}

/** API-PLAN-008 DELETE /api/plans/drafts/{draftId}/items/{itemId} */
export async function mockRemoveItem(draftId, itemId) {
  await wait(200)
  const store = readStore()
  const draft = findDraft(store, draftId)
  const target = draft.items.find((item) => item.itemId === itemId)

  if (!target) {
    throw mockError(404, '일정 항목을 찾을 수 없습니다.')
  }

  if (target.type === 'EVENT' && target.placeId === coreEventOf(draft)) {
    throw mockError(409, '핵심 문화행사는 삭제할 수 없습니다. 다시 추천받거나 다른 행사를 선택해 주세요.')
  }

  draft.items = sortItems(draft.items.filter((item) => item.itemId !== itemId))
  draft.recommendationReasons = draft.recommendationReasons.filter((reason) => reason.itemId !== itemId)
  writeStore(store)
}

/** API-PLAN-009 POST /api/plans — 초안을 저장 일정으로 확정 */
export async function mockSaveDraft(draftId) {
  await wait(400)
  const store = readStore()
  const draft = findDraft(store, draftId)

  if (draft.savedPlanId) {
    throw mockError(409, '이미 저장된 초안입니다.')
  }

  store.nextPlanId += 1
  draft.savedPlanId = store.nextPlanId

  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const dDay = Math.round((new Date(`${draft.visitDate}T00:00:00`) - today) / 86400000)

  store.plans[draft.savedPlanId] = { planId: draft.savedPlanId, title: draft.title, visitDate: draft.visitDate, dDay }
  writeStore(store)
  return structuredClone(store.plans[draft.savedPlanId])
}

/** API-PLACE-001 GET /api/places/restaurants — 행사 주변 맛집 후보 (distance: [제안] 미터 단위) */
export async function mockSearchRestaurants({ eventId }) {
  await wait(250)
  const event = EVENTS[eventId]

  const items = RESTAURANTS
    .map((place) => ({ ...place, distance: distanceMeters(event, place) }))
    .sort((a, b) => (a.distance ?? Infinity) - (b.distance ?? Infinity))

  return { items: structuredClone(items) }
}

/** 목업 초기화 (개발 중 콘솔에서 사용: window.tripaiResetMockPlans()) */
export function resetMockPlans() {
  writeStore(createInitialStore())
}
