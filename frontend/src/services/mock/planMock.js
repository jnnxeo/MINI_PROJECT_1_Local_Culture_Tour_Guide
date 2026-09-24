/*
 * 나의 일정 목업 API — 백엔드(#24) 완성 전까지 화면 개발용
 * 응답 모양은 docs/07_나의일정_API_계약.md 3장 "일정 상세 응답"과 같다.
 * 데이터는 sessionStorage에 두어 새로고침해도 초안이 유지된다(UX-005 흉내).
 * 사진은 Figma 시안의 참고 이미지이며 실제 데이터 연결 시 API imageUrl로 대체된다.
 */
import cafeImage from '../../assets/mock/cafe.jpg'
import galleryImage from '../../assets/mock/gallery.jpg'
import hanokImage from '../../assets/mock/hanok.jpg'
import palaceImage from '../../assets/mock/palace.jpg'
import restaurantImage from '../../assets/mock/restaurant.jpg'
import {
  distanceMeters,
  getEndTime,
  getPlanWarnings,
  sortItems,
  toMinutes,
  validatePlan,
} from '../../utils/planTime.js'

const STORAGE_KEY = 'tripai.mock.plans.v1'

const EVENTS = {
  'DEV-EV-001': {
    contentId: 'DEV-EV-001',
    name: '고궁의 밤, 달빛 산책',
    category: '전통문화',
    address: '서울 종로구 사직로 161',
    lat: 37.5796,
    lng: 126.977,
    imageUrl: palaceImage,
    startTime: '18:00',
    durationMin: 180,
    timeFixed: true,
    periodStart: '2026-09-01',
    periodEnd: '2026-10-31',
  },
}

const PLACES = [
  { contentId: 'DEV-PL-001', name: '행사장 근처 한식당', category: '식사', district: '종로구', address: '서울 종로구 사직로 125', lat: 37.5758, lng: 126.9731, imageUrl: restaurantImage, openTime: '11:00', closeTime: '21:00' },
  { contentId: 'DEV-PL-002', name: '서촌 동네 카페', category: '식사와 휴식', district: '종로구', address: '서울 종로구 자하문로 20', lat: 37.5788, lng: 126.9703, imageUrl: cafeImage, openTime: '10:00', closeTime: '22:00' },
  { contentId: 'DEV-PL-003', name: '종로 한식당', category: '식사', district: '종로구', address: '서울 종로구 세종대로 175', lat: 37.572, lng: 126.9769, imageUrl: hanokImage, openTime: '07:00', closeTime: '22:00' },
  { contentId: 'DEV-PL-004', name: '한옥 카페', category: '식사와 휴식', district: '종로구', address: '서울 종로구 삼청로 101', lat: 37.583, lng: 126.9817, imageUrl: restaurantImage, openTime: '10:30', closeTime: '20:00' },
  { contentId: 'DEV-PL-005', name: '서촌 베이커리 카페', category: '식사와 휴식', district: '종로구', address: '서울 종로구 필운대로 12', lat: 37.5795, lng: 126.969, imageUrl: galleryImage, openTime: '08:00', closeTime: '19:00' },
  { contentId: 'DEV-PL-006', name: '삼청동 칼국수', category: '식사', district: '종로구', address: '서울 종로구 삼청로 110', lat: 37.5835, lng: 126.982, imageUrl: null, openTime: '10:30', closeTime: '20:00' },
  { contentId: 'DEV-PL-007', name: '영업시간 미상 식당', category: '식사', district: '종로구', address: '서울 종로구 율곡로 50', lat: 37.576, lng: 126.985, imageUrl: null, openTime: null, closeTime: null },
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

function buildItems(courseIndex, startItemId) {
  const [lunchId, afternoonId] = COURSES[courseIndex % COURSES.length]
  const lunch = PLACES.find((place) => place.contentId === lunchId)
  const afternoon = PLACES.find((place) => place.contentId === afternoonId)
  const event = EVENTS['DEV-EV-001']

  return sortItems([
    toPlaceItem(lunch, { itemId: startItemId, startTime: '12:30', durationMin: 60, aiReason: '행사장 반경 · 식사 시간대' }),
    toPlaceItem(afternoon, { itemId: startItemId + 1, startTime: '14:00', durationMin: 60, aiReason: '행사장 반경 · 쉬어 가기 좋은 시간대' }),
    toEventItem(event, { itemId: startItemId + 2, aiReason: '선택한 문화행사 · 조건 일치' }),
  ])
}

function toPlaceItem(place, { itemId, startTime, durationMin, aiReason = null }) {
  return {
    itemId,
    seq: 0,
    type: 'PLACE',
    contentId: place.contentId,
    name: place.name,
    category: place.category,
    address: place.address,
    lat: place.lat,
    lng: place.lng,
    imageUrl: place.imageUrl,
    startTime,
    durationMin,
    endTime: getEndTime({ startTime, durationMin }),
    timeFixed: false,
    aiReason,
    openTime: place.openTime,
    closeTime: place.closeTime,
  }
}

function toEventItem(event, { itemId, aiReason = null }) {
  return {
    itemId,
    seq: 0,
    type: 'EVENT',
    contentId: event.contentId,
    name: event.name,
    category: event.category,
    address: event.address,
    lat: event.lat,
    lng: event.lng,
    imageUrl: event.imageUrl,
    startTime: event.startTime,
    durationMin: event.durationMin,
    endTime: getEndTime(event),
    timeFixed: event.timeFixed,
    aiReason,
    openTime: null,
    closeTime: null,
  }
}

function createInitialStore() {
  const items = buildItems(0, 101)

  return {
    nextPlanId: 13,
    nextItemId: 200,
    plans: {
      12: {
        planId: 12,
        title: '고궁의 밤을 기다리는 하루',
        tripDate: '2026-10-03',
        visitStartTime: '11:00',
        visitEndTime: '21:00',
        headcount: 2,
        transportMode: 'WALK_TRANSIT',
        interests: ['문화·역사'],
        anchorEventId: 'DEV-EV-001',
        saved: false,
        aiGenerated: true,
        dDay: null,
        courseIndex: 0,
        items,
        warnings: getPlanWarnings(items),
      },
    },
  }
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

function toResponse(plan) {
  const { courseIndex, ...detail } = plan
  return structuredClone(detail)
}

function calcDDay(tripDate) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((new Date(`${tripDate}T00:00:00`) - today) / 86400000)
}

export async function mockGetLatestDraft() {
  await wait(300)
  const store = readStore()
  const drafts = Object.values(store.plans).filter((plan) => !plan.saved)

  if (drafts.length === 0) {
    throw mockError(404, '존재하지 않는 일정입니다.')
  }

  return toResponse(drafts.at(-1))
}

export async function mockGetPlan(planId) {
  await wait(300)
  const plan = readStore().plans[planId]

  if (!plan) {
    throw mockError(404, '존재하지 않는 일정입니다.')
  }

  return toResponse(plan)
}

/** PUT /api/plans/{planId} — 요청 항목을 카탈로그로 채워 검증 후 전체 교체 */
export async function mockSavePlan(planId, request) {
  await wait(500)
  const store = readStore()
  const plan = store.plans[planId]

  if (!plan) {
    throw mockError(404, '존재하지 않는 일정입니다.')
  }

  const items = request.items.map((requestItem) => {
    if (requestItem.type === 'EVENT') {
      const event = EVENTS[requestItem.contentId]

      if (!event) {
        throw mockError(404, '존재하지 않는 행사입니다.')
      }

      if (event.timeFixed && requestItem.startTime !== event.startTime) {
        throw mockError(400, `${event.name}은(는) 시작 시간이 정해진 행사라 시간을 바꿀 수 없습니다.`)
      }

      store.nextItemId += 1
      return toEventItem(event, { itemId: store.nextItemId, aiReason: requestItem.aiReason ?? null })
    }

    const place = PLACES.find((candidate) => candidate.contentId === requestItem.contentId)

    if (!place) {
      throw mockError(404, '존재하지 않는 장소입니다.')
    }

    store.nextItemId += 1
    return toPlaceItem(place, { ...requestItem, itemId: store.nextItemId })
  })

  const error = validatePlan({ ...plan, ...request, items })

  if (error) {
    throw mockError(error.code === 'PLAN_ITEM_DUPLICATED' ? 409 : 400, error.message)
  }

  const sortedItems = sortItems(items)
  const saved = {
    ...plan,
    title: request.title.trim(),
    anchorEventId: request.anchorEventId,
    saved: true,
    dDay: calcDDay(plan.tripDate),
    items: sortedItems,
    warnings: getPlanWarnings(sortedItems),
  }

  store.plans[planId] = saved
  writeStore(store)
  return toResponse(saved)
}

/** POST /api/plans/{planId}/regenerate — conditions가 null이면 "다시 추천" */
export async function mockRegeneratePlan(planId, conditions) {
  await wait(1800)
  const store = readStore()
  const plan = store.plans[planId]

  if (!plan) {
    throw mockError(404, '존재하지 않는 일정입니다.')
  }

  if (plan.saved) {
    throw mockError(409, '저장한 일정은 다시 추천할 수 없습니다.')
  }

  if (conditions) {
    const unchanged = conditions.headcount === plan.headcount
      && conditions.transportMode === plan.transportMode
      && conditions.useAi === plan.aiGenerated
      && [...conditions.interests].sort().join() === [...plan.interests].sort().join()

    if (unchanged) {
      throw mockError(409, '조건이 바뀌지 않아 기존 일정을 유지합니다.')
    }
  }

  const courseIndex = plan.courseIndex + 1
  const items = buildItems(courseIndex, store.nextItemId + 1)
  store.nextItemId += items.length

  const regenerated = {
    ...plan,
    headcount: conditions?.headcount ?? plan.headcount,
    transportMode: conditions?.transportMode ?? plan.transportMode,
    interests: conditions?.interests ?? plan.interests,
    aiGenerated: conditions?.useAi ?? plan.aiGenerated,
    courseIndex,
    items,
    warnings: getPlanWarnings(items),
  }

  store.plans[planId] = regenerated
  writeStore(store)
  return toResponse(regenerated)
}

/** GET /api/places — 맛집 후보, 기준 행사에서 가까운 순 */
export async function mockSearchPlaces({ eventId, keyword = '', time, durationMin }) {
  await wait(250)
  const event = EVENTS[eventId]
  const normalizedKeyword = keyword.trim()

  const items = PLACES
    .filter((place) => !normalizedKeyword
      || [place.name, place.district, place.address].some((text) => text.includes(normalizedKeyword)))
    .map((place) => {
      const openAtRequestedTime = !place.openTime || !time
        ? null
        : toMinutes(time) >= toMinutes(place.openTime)
          && toMinutes(time) + (durationMin ?? 60) <= toMinutes(place.closeTime)

      return {
        contentId: place.contentId,
        name: place.name,
        category: place.category,
        district: place.district,
        address: place.address,
        lat: place.lat,
        lng: place.lng,
        imageUrl: place.imageUrl,
        distanceM: distanceMeters(event, place),
        openTime: place.openTime,
        closeTime: place.closeTime,
        openAtRequestedTime,
      }
    })
    .sort((a, b) => (a.distanceM ?? Infinity) - (b.distanceM ?? Infinity))

  return { items, page: 0, hasNext: false }
}

/** 목업 초기화 (개발 중 콘솔에서 사용: window.tripaiResetMockPlans()) */
export function resetMockPlans() {
  writeStore(createInitialStore())
}
