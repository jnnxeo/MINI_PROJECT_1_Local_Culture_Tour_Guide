export function getDefaultSort(dates = []) {
  return dates.length ? 'dateClosest' : 'recentStart'
}

export function getSortOptions(dates = []) {
  return [
    { value: getDefaultSort(dates), label: dates.length ? '선택 날짜 기준' : '최근 시작순' },
    { value: 'startDateAsc', label: '시작 날짜순' },
    { value: 'endDateAsc', label: '종료 임박순' },
    { value: 'titleAsc', label: '행사명순' },
  ]
}

export function resolveSort(sort, dates = []) {
  if (!sort || sort === 'dateClosest' || sort === 'recentStart') return getDefaultSort(dates)
  return sort
}

// 실제 API의 기본 정렬과 동일한 기준을 목업에도 적용한다.
export function compareDefaultEvents(left, right, dates, today) {
  if (dates.length) {
    const distance = (event) => Math.min(...dates
      .filter(date => event.startDate <= date && date <= event.endDate)
      .map(date => Date.parse(`${date}T00:00:00Z`) - Date.parse(`${event.startDate}T00:00:00Z`)))
    return distance(left) - distance(right)
      || left.endDate.localeCompare(right.endDate) || left.eventId.localeCompare(right.eventId)
  }
  const group = (event) => event.startDate > today ? 1 : event.endDate < today ? 2 : 0
  const order = group(left) - group(right)
  if (order) return order
  const dateOrder = group(left) === 1
    ? left.startDate.localeCompare(right.startDate)
    : group(left) === 0
      ? right.startDate.localeCompare(left.startDate)
      : right.endDate.localeCompare(left.endDate)
  return dateOrder || left.endDate.localeCompare(right.endDate) || left.eventId.localeCompare(right.eventId)
}
