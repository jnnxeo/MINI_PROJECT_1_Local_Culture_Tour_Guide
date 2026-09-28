import test from 'node:test'
import assert from 'node:assert/strict'
import { readSearchConditions, buildSearchParams } from './searchParams.js'

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
