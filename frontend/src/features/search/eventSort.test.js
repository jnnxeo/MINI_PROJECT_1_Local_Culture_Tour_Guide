import test from 'node:test'
import assert from 'node:assert/strict'
import { compareDefaultEvents, getSortOptions } from './eventSort.js'

const event = (eventId, startDate, endDate) => ({ eventId, startDate, endDate })

test('dropdown displays only the default sort applicable to the date condition', () => {
  assert.deepEqual(getSortOptions([]).map(option => option.label), ['최근 시작순', '시작 날짜순', '종료 임박순', '행사명순'])
  assert.deepEqual(getSortOptions(['2026-09-28']).map(option => option.label), ['선택 날짜 기준', '시작 날짜순', '종료 임박순', '행사명순'])
})

test('each event uses a selected date on which it actually runs, with stable end date and ID ties', () => {
  const items = [event('long', '2021-09-28', '2027-01-01'), event('near', '2026-09-27', '2026-10-10'),
    event('second', '2026-10-03', '2026-10-04'), event('b', '2026-09-28', '2026-09-29'),
    event('a', '2026-09-28', '2026-09-29')]
  items.sort((left, right) => compareDefaultEvents(left, right, ['2026-09-28', '2026-10-03'], '2026-09-28'))
  assert.deepEqual(items.map(item => item.eventId), ['a', 'b', 'second', 'near', 'long'])
})

test('without selected dates, ongoing events precede upcoming and ended events', () => {
  const items = [event('ended', '2026-09-01', '2026-09-27'), event('future', '2026-09-29', '2026-10-01'),
    event('old', '2021-09-28', '2027-01-01'), event('recent', '2026-09-27', '2026-10-01')]
  items.sort((left, right) => compareDefaultEvents(left, right, [], '2026-09-28'))
  assert.deepEqual(items.map(item => item.eventId), ['recent', 'old', 'future', 'ended'])
})
