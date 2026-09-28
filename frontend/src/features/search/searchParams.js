export const SEARCH_PAGE_SIZE = 10
export const DEFAULT_SORT = 'startDateAsc'

export function getCurrentMonth() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`
}

export function readSearchConditions(params) {
  return {
    keyword: params.get('keyword') || '',
    month: params.get('month') || '',
    dates: params.getAll('date'),
    categories: params.getAll('category'),
    district: params.get('district') || '전체 지역',
    freeOnly: params.get('freeYn') === 'true',
    sort: params.get('sort') || DEFAULT_SORT,
    page: Number(params.get('page') || 0),
  }
}

export function buildSearchParams(conditions) {
  const params = new URLSearchParams()
  if (conditions.keyword?.trim()) params.set('keyword', conditions.keyword.trim())
  if (conditions.month) params.set('month', conditions.month)
  for (const date of conditions.dates ?? []) params.append('date', date)
  conditions.categories.forEach((category) => params.append('category', category))
  if (conditions.district !== '전체 지역') params.set('district', conditions.district)
  if (conditions.freeOnly) params.set('freeYn', 'true')
  if (conditions.sort !== DEFAULT_SORT) params.set('sort', conditions.sort)
  if (conditions.page > 0) params.set('page', String(conditions.page))
  return params
}
