import test from 'node:test'
import assert from 'node:assert/strict'
import { readSearchConditions, buildSearchParams, buildSubmittedSearchParams } from './searchParams.js'

test('multiple dates and other filters survive edit and resubmit', () => {
  const source = new URLSearchParams('date=2026-10-03&date=2026-10-04&category=전시&district=종로구&freeYn=true&page=2')
  const conditions = readSearchConditions(source)
  const result = buildSearchParams({ ...conditions, page: 0 })
  assert.deepEqual(result.getAll('date'), ['2026-10-03', '2026-10-04'])
  assert.deepEqual(result.getAll('category'), ['전시'])
  assert.equal(result.get('district'), '종로구')
  assert.equal(result.get('freeYn'), 'true')
  assert.equal(result.has('page'), false)
})

test('legacy monthly links stay valid and date reset removes both constraints', () => {
  const monthly = readSearchConditions(new URLSearchParams('month=2026-10'))
  assert.deepEqual(monthly.dates, [])
  assert.equal(buildSearchParams(monthly).get('month'), '2026-10')
  const reset = buildSearchParams({ ...monthly, month: '', dates: [] })
  assert.equal(reset.has('month'), false)
  assert.equal(reset.has('date'), false)
})

test('dates from different years survive sorting and paging', () => {
  const source = new URLSearchParams('date=2026-12-31&date=2027-01-01&category=공연&district=종로구&freeYn=true')
  const conditions = readSearchConditions(source)
  const sorted = buildSearchParams({ ...conditions, sort: 'titleAsc', page: 0 })
  const nextPage = buildSearchParams({ ...readSearchConditions(sorted), page: 1 })

  assert.deepEqual(nextPage.getAll('date'), ['2026-12-31', '2027-01-01'])
  assert.deepEqual(nextPage.getAll('category'), ['공연'])
  assert.equal(nextPage.get('district'), '종로구')
  assert.equal(nextPage.get('freeYn'), 'true')
  assert.equal(nextPage.get('sort'), 'titleAsc')
  assert.equal(nextPage.get('page'), '1')
})

test('default sort follows the selected dates and explicit legacy sorts remain available', () => {
  assert.equal(readSearchConditions(new URLSearchParams()).sort, 'recentStart')
  assert.equal(readSearchConditions(new URLSearchParams('date=2026-09-28')).sort, 'dateClosest')
  assert.equal(readSearchConditions(new URLSearchParams('date=2026-09-28&sort=startDateAsc')).sort, 'startDateAsc')
  assert.equal(readSearchConditions(new URLSearchParams('sort=dateClosest')).sort, 'recentStart')
  assert.equal(buildSearchParams(readSearchConditions(new URLSearchParams('sort=startDateAsc'))).get('sort'), 'startDateAsc')
})

test('submitting changed dates resets sorting, other filter edits preserve the chosen sort', () => {
  const applied = readSearchConditions(new URLSearchParams('date=2026-09-28&sort=titleAsc&page=3'))
  const changed = readSearchConditions(buildSubmittedSearchParams({ ...applied, dates: ['2027-01-01'] }, applied))
  assert.equal(changed.sort, 'dateClosest')
  assert.equal(changed.page, 0)
  assert.equal(readSearchConditions(buildSubmittedSearchParams({ ...applied, dates: [] }, applied)).sort, 'recentStart')
  assert.equal(readSearchConditions(buildSubmittedSearchParams({ ...applied, district: '중구' }, applied)).sort, 'titleAsc')
})
