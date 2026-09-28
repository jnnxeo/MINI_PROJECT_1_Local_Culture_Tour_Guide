export const SEARCH_PAGE_SIZE = 10
import { getDefaultSort, resolveSort } from './eventSort.js'

export function getCurrentMonth() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`
}

export function readSearchConditions(params) {
  const dates = params.getAll('date')
  return {
    keyword: params.get('keyword') || '',
    month: params.get('month') || '',
    dates,
    categories: params.getAll('category'),
    district: params.get('district') || '전체 지역',
    freeOnly: params.get('freeYn') === 'true',
    sort: resolveSort(params.get('sort'), dates),
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
  const sort = resolveSort(conditions.sort, conditions.dates)
  if (sort !== getDefaultSort(conditions.dates)) params.set('sort', sort)
  if (conditions.page > 0) params.set('page', String(conditions.page))
  return params
}

export function buildSubmittedSearchParams(draft, applied) {
  const dateKey = (dates) => JSON.stringify([...new Set(dates)].sort())
  const datesChanged = dateKey(draft.dates) !== dateKey(applied.dates)
  return buildSearchParams({ ...draft, page: 0,
    sort: datesChanged ? getDefaultSort(draft.dates) : applied.sort })
}
