import assert from 'node:assert/strict'
import test from 'node:test'
import { buildEventPlanItem } from './eventPlan.js'

const event = {
  eventId: 'EVT002',
  title: '두 번째 행사',
  startDate: '2026-09-20',
  endDate: '2026-09-30',
  startTime: '18:00:00',
  endTime: '19:00:00',
}

const plan = {
  planId: 1,
  saved: true,
  tripDate: '2026-09-25',
  visitStartTime: '10:00',
  visitEndTime: '21:00',
  items: [{ type: 'EVENT', contentId: 'EVT001', startTime: '15:00', durationMin: 60 }],
}

test('저장 일정에 겹치지 않는 두 번째 행사를 배치한다', () => {
  const result = buildEventPlanItem(plan, event)
  assert.equal(result.item?.contentId, 'EVT002')
  assert.equal(result.item?.startTime, '18:00')
  assert.equal(result.item?.timeFixed, true)
})

test('동일 행사와 세 번째 행사는 차단한다', () => {
  assert.match(buildEventPlanItem(plan, { ...event, eventId: 'EVT001' }).error, /이미/)
  const fullPlan = {
    ...plan,
    items: [...plan.items, { type: 'EVENT', contentId: 'EVT003', startTime: '12:00', durationMin: 60 }],
  }
  assert.match(buildEventPlanItem(fullPlan, event).error, /2개/)
})

test('날짜와 시간 충돌을 안내한다', () => {
  assert.match(buildEventPlanItem({ ...plan, tripDate: '2026-10-01' }, event).error, /날짜/)
  const occupiedPlan = {
    ...plan,
    items: [{ type: 'EVENT', contentId: 'EVT001', startTime: '18:30', durationMin: 60 }],
  }
  assert.match(buildEventPlanItem(occupiedPlan, event).error, /행사/)
})

test('시작 시각이 없는 행사는 빈 시간에 배치한다', () => {
  const result = buildEventPlanItem(plan, { ...event, startTime: null, endTime: null })
  assert.equal(result.item?.startTime, '10:00')
  assert.equal(result.item?.durationMin, 60)
  assert.equal(result.item?.timeFixed, false)
})
