import galleryImage from '../../assets/mock/gallery.jpg'
import palaceImage from '../../assets/mock/palace.jpg'

const events = [
  { eventId: 'DEV-EV-001', title: '고궁의 밤, 달빛 산책', category: '전통문화', district: '종로구', place: '경복궁', startMonthDay: '09-01', endMonthDay: '10-31', freeYn: true, fee: '무료', imageUrl: palaceImage },
  { eventId: 'mock-102', title: '도심 속 재즈 피크닉', category: '공연', district: '마포구', place: '서울광장', startMonthDay: '03-01', endMonthDay: '10-31', freeYn: false, fee: '20,000원', imageUrl: null, imageTone: 'concert' },
  { eventId: 'mock-103', title: '빛과 색의 새로운 시선', category: '전시', district: '성동구', place: '성수 전시관', startMonthDay: '02-10', endMonthDay: '11-30', freeYn: true, fee: '무료', imageUrl: galleryImage },
  { eventId: 'mock-104', title: '작품 사이를 걷는 시간', category: '전시', district: '용산구', startMonthDay: '04-01', endMonthDay: '12-20', freeYn: false, fee: '12,000원', imageUrl: galleryImage },
  { eventId: 'mock-105', title: '도시의 작은 전시', category: '전시', district: '중구', startMonthDay: '01-05', endMonthDay: '08-31', freeYn: true, fee: '무료', imageUrl: galleryImage },
  { eventId: 'mock-106', title: '주말 갤러리 산책', category: '전시', district: '종로구', startMonthDay: '09-01', endMonthDay: '12-31', freeYn: false, fee: '8,000원', imageUrl: galleryImage },
  { eventId: 'mock-107', title: '음악으로 만나는 저녁', category: '공연', district: '강남구', startMonthDay: '01-01', endMonthDay: '06-30', freeYn: false, fee: '30,000원', imageUrl: null, imageTone: 'concert' },
  { eventId: 'mock-108', title: '작은 무대의 큰 울림', category: '공연', district: '서대문구', startMonthDay: '05-01', endMonthDay: '12-31', freeYn: false, fee: '15,000원', imageUrl: null, imageTone: 'concert' },
  { eventId: 'mock-109', title: '주말 라이브 공연', category: '공연', district: '광진구', startMonthDay: '07-01', endMonthDay: '12-31', freeYn: false, fee: '25,000원', imageUrl: null, imageTone: 'concert' },
  { eventId: 'mock-110', title: '서울 문화 산책', category: '축제', district: '중구', startMonthDay: '09-01', endMonthDay: '10-31', freeYn: null, fee: null, imageUrl: null, imageTone: 'gallery' },
]

// 검색 결과의 페이지 이동을 확인할 데이터. 홈 월별 카드 목업에는 영향을 주지 않는다.
const searchOnlyEvents = [
  { eventId: 'mock-111', title: '한강의 노래', category: '공연', district: '영등포구', startMonthDay: '09-01', endMonthDay: '12-31', freeYn: true, fee: '무료', imageUrl: null, imageTone: 'concert' },
  { eventId: 'mock-112', title: '우리 동네 사진전', category: '전시', district: '서대문구', startMonthDay: '01-01', endMonthDay: '12-31', freeYn: null, fee: null, imageUrl: galleryImage },
  { eventId: 'mock-113', title: '서울의 가을 축제', category: '축제', district: '강남구', startMonthDay: '09-01', endMonthDay: '11-30', freeYn: false, fee: '5,000원', imageUrl: null, imageTone: 'palace' },
]

const SORT_VALUES = ['startDateAsc', 'endDateAsc', 'titleAsc']

export async function mockSearchEvents(params) {
  const month = params.get('month')
  const keyword = params.get('keyword')?.trim().toLocaleLowerCase('ko-KR')
  const categories = params.getAll('category')
  const district = params.get('district')
  const freeYn = params.get('freeYn')
  const sort = params.get('sort') || 'startDateAsc'
  const page = Number(params.get('page') ?? 0)
  const size = Number(params.get('size') ?? 10)

  if ((month && !/^\d{4}-(0[1-9]|1[0-2])$/.test(month))
    || !SORT_VALUES.includes(sort)
    || !Number.isInteger(page) || page < 0
    || !Number.isInteger(size) || size < 1 || size > 100
    || (freeYn != null && freeYn !== 'true' && freeYn !== 'false')) {
    throw new Error('검색 조건을 확인해 주세요.')
  }

  const year = month ? Number(month.slice(0, 4)) : new Date().getFullYear()
  const monthEnd = month ? `${month}-${String(new Date(year, Number(month.slice(5)), 0).getDate()).padStart(2, '0')}` : null
  const today = new Date().toLocaleDateString('sv-SE')
  const matched = [...events, ...searchOnlyEvents]
    .map(({ startMonthDay, endMonthDay, ...event }) => ({
      ...event,
      startDate: `${year}-${startMonthDay}`,
      endDate: `${year}-${endMonthDay}`,
    }))
    .filter((event) => {
      if (month && (event.startDate > monthEnd || event.endDate < `${month}-01`)) return false
      if (categories.length && !categories.includes(event.category)) return false
      if (district && district !== event.district) return false
      if (freeYn === 'true' && event.freeYn !== true) return false
      if (!keyword) return true
      return [event.title, event.place, event.district, event.category]
        .some((value) => value?.toLocaleLowerCase('ko-KR').includes(keyword))
    })
    .sort((left, right) => {
      if (sort === 'titleAsc') return left.title.localeCompare(right.title, 'ko') || left.eventId.localeCompare(right.eventId)
      if (sort === 'endDateAsc') {
        const leftEnded = left.endDate < today
        const rightEnded = right.endDate < today
        if (leftEnded !== rightEnded) return leftEnded ? 1 : -1
        const order = left.endDate.localeCompare(right.endDate)
        return (leftEnded ? -order : order) || left.eventId.localeCompare(right.eventId)
      }
      return left.startDate.localeCompare(right.startDate) || left.eventId.localeCompare(right.eventId)
    })

  return { items: matched.slice(page * size, (page + 1) * size), page, totalCount: matched.length }
}

export async function mockGetMonthlyEvents(month) {
  const [year, monthNumber] = month.split('-').map(Number)
  const firstDay = `${month}-01`
  const lastDay = `${month}-${String(new Date(year, monthNumber, 0).getDate()).padStart(2, '0')}`

  const items = events
    .map(({ startMonthDay, endMonthDay, ...event }) => ({
      ...event,
      startDate: `${year}-${startMonthDay}`,
      endDate: `${year}-${endMonthDay}`,
    }))
    .filter((event) => event.startDate <= lastDay && event.endDate >= firstDay)

  return { month, items, totalCount: items.length }
}

export async function mockGetHomeEventPreviews(month) {
  const { items } = await mockGetMonthlyEvents(month)
  return {
    all: items.slice(0, 10),
    exhibition: items.filter((event) => event.category === '전시').slice(0, 10),
    performance: items.filter((event) => event.category === '공연').slice(0, 10),
  }
}

export async function mockHasMatchingEvents(params) {
  const month = params.get('month')
  const keyword = params.get('keyword')?.trim().toLocaleLowerCase('ko-KR')
  const categories = params.getAll('category')
  const district = params.get('district')
  const freeOnly = params.get('freeYn') === 'true'
  const items = month
    ? (await mockGetMonthlyEvents(month)).items
    : events.map(({ startMonthDay, endMonthDay, ...event }) => event)

  return items.some((event) => {
    if (categories.length && !categories.includes(event.category)) return false
    if (district && district !== event.district) return false
    if (freeOnly && event.freeYn !== true) return false
    if (!keyword) return true
    return [event.title, event.place, event.district, event.category]
      .some((value) => value?.toLocaleLowerCase('ko-KR').includes(keyword))
  })
}
