import React, { useEffect, useState } from 'react'
import Modal from '../common/Modal.jsx'
import { addEventToSavedPlan, getPlan, getSavedPlans } from '../../services/planService.js'
import { buildEventPlanItem } from '../../utils/eventPlan.js'

export default function EventSavedPlanModal({ event, onClose, onChoose }) {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [addingPlanId, setAddingPlanId] = useState(null)

  async function choose(plan, item) {
    setError('')
    setAddingPlanId(plan.planId)
    try {
      await addEventToSavedPlan(plan.planId, event.eventId, item.startTime)
      onChoose(plan.planId)
    } catch (requestError) {
      setError(requestError.message)
      setAddingPlanId(null)
    }
  }

  useEffect(() => {
    let active = true

    async function loadPlans() {
      try {
        const summaries = await getSavedPlans()
        const results = await Promise.all(summaries.map(async (summary) => {
          try {
            const plan = await getPlan(summary.planId, { forceApi: true })
            return { plan, result: buildEventPlanItem(plan, event) }
          } catch (requestError) {
            return {
              plan: summary,
              result: { error: requestError.message },
            }
          }
        }))

        if (active) setRows(results)
      } catch (requestError) {
        if (active) setError(requestError.message)
      } finally {
        if (active) setLoading(false)
      }
    }

    loadPlans()
    return () => { active = false }
  }, [event])

  return (
    <Modal title="저장한 일정에 추가" onClose={onClose}>
      {loading && <p role="status">저장한 일정을 불러오는 중입니다.</p>}
      {error && <p role="alert" className="tp-modal__error">{error}</p>}
      {!loading && !error && rows.length === 0 && <p>저장한 일정이 없습니다.</p>}

      <div className="event-saved-plan-list">
        {rows.map(({ plan, result }) => (
          <div className="event-saved-plan" key={plan.planId}>
            <div>
              <strong>{plan.title}</strong>
              <p>{plan.tripDate ?? plan.visitDate}</p>
              {result.error && <p className="tp-modal__error">{result.error}</p>}
            </div>
            <button
              type="button"
              className="tp-btn tp-btn--secondary"
              disabled={Boolean(result.error) || addingPlanId != null}
              onClick={() => choose(plan, result.item)}
            >
              {addingPlanId === plan.planId ? '추가 중…' : '선택'}
            </button>
          </div>
        ))}
      </div>
    </Modal>
  )
}
