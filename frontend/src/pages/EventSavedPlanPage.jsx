import React, { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import PlanHeader from '../components/plan/PlanHeader.jsx'
import PlanFooter from '../components/plan/PlanFooter.jsx'
import RouteMap from '../components/plan/RouteMap.jsx'
import { getPlan, updateSavedEventTime } from '../services/planService.js'
import '../styles/plan.css'

export default function EventSavedPlanPage() {
  const { planId } = useParams()
  const { state } = useLocation()
  const navigate = useNavigate()
  const [plan, setPlan] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [editItemId, setEditItemId] = useState(null)
  const [editTime, setEditTime] = useState('')
  const [selectedKey, setSelectedKey] = useState(null)

  useEffect(() => {
    let active = true
    getPlan(planId)
      .then((data) => {
        if (active) {
          setPlan(data)
          const added = data.items.find((item) => item.contentId === state?.addedEventId)
          setSelectedKey(added?.itemId ?? data.items[0]?.itemId ?? null)
        }
      })
      .catch((requestError) => { if (active) setError(requestError.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [planId, state?.addedEventId])

  async function saveTime() {
    if (!editTime || saving) return
    setSaving(true)
    setError('')
    try {
      const updated = await updateSavedEventTime(planId, editItemId, editTime)
      setPlan(updated)
      setEditItemId(null)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSaving(false)
    }
  }

  const items = (plan?.items ?? []).map((item) => ({
    ...item,
    key: item.itemId,
    sequence: item.seq,
  }))

  return (
    <div className="plan-page">
      <PlanHeader onNavigate={(path, action) => {
        if (action) action()
        else navigate(path === '/my-trips' ? '/mypage' : path)
      }} />
      <section className="plan-heading">
        <p className="plan-heading__eyebrow">저장한 일정 · {plan?.tripDate}</p>
        <h1 className="plan-heading__title">{plan?.title ?? '저장한 일정'}</h1>
        {state?.addedEventId && plan && <p>선택한 행사를 일정에 추가했습니다.</p>}
      </section>
      <main className="plan-editor">
        {loading && <p role="status">일정을 불러오는 중입니다.</p>}
        {error && <p role="alert" className="tp-modal__error">{error}</p>}
        {!loading && plan && (
          <>
            <div className="plan-toolbar">
              <p>당일 여행 · 변경된 일정은 저장되어 있습니다.</p>
              <button className="tp-btn tp-btn--secondary" type="button" onClick={() => navigate('/mypage')}>
                내 여행으로
              </button>
            </div>
            <div className="plan-layout">
              <div className="plan-timeline">
                {items.map((item) => (
                  <article className="plan-route__selected" key={item.key}>
                    <h2>{item.sequence}. {item.name}</h2>
                    <p>{item.startTime}–{item.endTime} · {item.type === 'EVENT' ? '문화행사' : '음식점'}</p>
                    {item.address && <p>{item.address}</p>}
                    {item.type === 'EVENT' && !item.timeFixed && (
                      <>
                        {editItemId === item.itemId ? (
                          <div>
                            <label htmlFor={`event-time-${item.itemId}`}>행사 방문 시간</label>
                            <input id={`event-time-${item.itemId}`} type="time" value={editTime}
                              onChange={(change) => setEditTime(change.target.value)} />
                            <button className="tp-btn tp-btn--primary" type="button" disabled={saving} onClick={saveTime}>
                              {saving ? '저장 중…' : '시간 저장'}
                            </button>
                            <button className="tp-btn tp-btn--secondary" type="button" onClick={() => setEditItemId(null)}>취소</button>
                          </div>
                        ) : (
                          <button className="tp-btn tp-btn--secondary" type="button" onClick={() => {
                            setEditItemId(item.itemId)
                            setEditTime(item.startTime?.slice(0, 5) ?? '')
                          }}>방문 시간 수정</button>
                        )}
                      </>
                    )}
                    {item.type === 'EVENT' && item.timeFixed && <p>공식 행사 시작 시각은 고정되어 있습니다.</p>}
                  </article>
                ))}
              </div>
              <aside className="plan-route" aria-label="오늘의 이동 동선">
                <h2 className="plan-route__title">오늘의 이동 동선</h2>
                <RouteMap items={items} selectedKey={selectedKey} onSelect={setSelectedKey} />
                <p className="plan-route__note">방문 순서 개념도 · 실제 도로 경로가 아닙니다.</p>
              </aside>
            </div>
          </>
        )}
      </main>
      <PlanFooter />
    </div>
  )
}
