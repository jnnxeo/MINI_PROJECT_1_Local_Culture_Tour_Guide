import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import PlanHeader from '../components/plan/PlanHeader.jsx'
import PlanFooter from '../components/plan/PlanFooter.jsx'
import PlanModal from '../components/plan/PlanModal.jsx'
import RouteMap from '../components/plan/RouteMap.jsx'
import TimelineItem, { getItemHeading } from '../components/plan/TimelineItem.jsx'
import { getPlan, updateSavedEventTime, updateSavedPlanTitle } from '../services/planService.js'
import { TITLE_MAX_LENGTH, distanceMeters, toMinutes, toTime } from '../utils/planTime.js'
import '../styles/plan-base.css'
import '../styles/plan.css'

const FREE_GAP_MINUTES = 120

function dDayLabel(days) {
  if (days == null) return null
  if (days === 0) return 'D-day'
  return days > 0 ? `D-${days}` : `D+${Math.abs(days)}`
}

function displayItems(plan) {
  return (plan?.items ?? []).map((item) => ({
    ...item,
    key: `item-${item.itemId}`,
    sequence: item.seq,
    placeId: item.contentId,
    addr: item.address,
    showVisitTime: item.type === 'EVENT',
    cuisineType: item.category === '카페' ? 'CAFE' : undefined,
  }))
}

/** SCR-017: 서버에 저장된 일정. 초안 편집 화면의 카드와 동선 컴포넌트를 공유한다. */
export default function EventSavedPlanPage() {
  const { planId } = useParams()
  const { state } = useLocation()
  const navigate = useNavigate()
  const routeRef = useRef(null)
  const [plan, setPlan] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modal, setModal] = useState(null)
  const [modalError, setModalError] = useState('')
  const [saving, setSaving] = useState(false)
  const [selectedKey, setSelectedKey] = useState(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    getPlan(planId)
      .then((data) => {
        if (!active) return
        setPlan(data)
        const added = data.items.find((item) => item.contentId === state?.addedEventId)
        setSelectedKey(`item-${added?.itemId ?? data.items[0]?.itemId}`)
        setError('')
      })
      .catch((requestError) => { if (active) setError(requestError.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [planId, state?.addedEventId])

  const items = useMemo(() => displayItems(plan), [plan])
  const selectedItem = items.find((item) => item.key === selectedKey) ?? items[0]
  const coreItem = items.find((item) => item.type === 'EVENT' && item.contentId === plan?.anchorEventId)
    ?? items.find((item) => item.type === 'EVENT')
  const routeSummary = items.map((item) => `${item.sequence} ${item.name}`).join(' → ')

  const closeModal = () => { if (!saving) { setModal(null); setModalError('') } }
  const saveModal = async (event) => {
    event.preventDefault()
    if (saving || !modal) return
    const value = modal.value.trim()
    if (!value) { setModalError(modal.type === 'rename' ? '일정 이름을 입력해 주세요.' : '방문 시간을 입력해 주세요.'); return }
    setSaving(true)
    setModalError('')
    try {
      if (modal.type === 'rename') await updateSavedPlanTitle(planId, value)
      else await updateSavedEventTime(planId, modal.itemId, value)
      const updated = await getPlan(planId)
      setPlan(updated)
      setModal(null)
    } catch (requestError) {
      setModalError(requestError.message || '변경 내용을 저장하지 못했습니다.')
    } finally {
      setSaving(false)
    }
  }

  const showOnMap = (item) => {
    setSelectedKey(item.key)
    routeRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }

  return (
    <div className="plan-page">
      <PlanHeader onNavigate={(path, action) => {
        if (action) action()
        else navigate(path === '/my-trips' ? '/mypage' : path)
      }} />
      <section className="plan-heading">
        <p className="plan-heading__eyebrow">저장한 일정 · {plan?.tripDate ?? ''}{plan && ` · ${dDayLabel(plan.dDay) ?? '날짜 정보 없음'}`}</p>
        <h1 className="plan-heading__title">{plan?.title ?? '저장한 일정'}</h1>
        {plan && <p className="plan-heading__desc">저장한 장소와 방문 순서를 확인하세요.</p>}
        {plan && <button className="plan-heading__rename" type="button" aria-label="일정 이름 변경"
          onClick={() => setModal({ type: 'rename', value: plan.title })}>✎</button>}
      </section>
      <main className="plan-editor">
        {loading && <p role="status">일정을 불러오는 중입니다.</p>}
        {error && <p role="alert" className="tp-modal__error">{error}</p>}
        {!loading && plan && <>
          <div className="plan-toolbar">
            <p className="plan-toolbar__desc">당일 여행 · 저장한 장소와 방문 순서</p>
            <button className="tp-btn tp-btn--secondary plan-toolbar__button" type="button" onClick={() => navigate('/mypage')}>내 여행으로</button>
          </div>
          {state?.addedEventId && <p className="plan-saved-notice" role="status">선택한 행사를 일정에 추가했습니다.</p>}
          <div className="plan-layout">
            <div className="plan-timeline">
              {items.map((item, index) => {
                const next = items[index + 1]
                const gapStart = item.startTime ? toMinutes(item.startTime) + item.durationMin : null
                const gap = gapStart != null && next?.startTime ? toMinutes(next.startTime) - gapStart : 0
                return <React.Fragment key={item.key}>
                  <TimelineItem item={item} isCore={item === coreItem} reason={item.aiReason}
                    isLast={index === items.length - 1} isSelected={item.key === selectedItem?.key}
                    distanceToNext={distanceMeters(item, next)} locked
                    onEditEventTime={item.type === 'EVENT' && !item.timeFixed ? () => setModal({ type: 'time', itemId: item.itemId, value: item.startTime?.slice(0, 5) ?? '' }) : undefined}
                    onShowOnMap={() => showOnMap(item)} />
                  {gap >= FREE_GAP_MINUTES && <p className="plan-gap" role="note">
                    <span className="plan-gap__text">{`${toTime(gapStart)}~${next.startTime} · ${Math.floor(gap / 60)}시간${gap % 60 ? ` ${gap % 60}분` : ''} 비어 있어요`}</span>
                  </p>}
                </React.Fragment>
              })}
              {items.length === 0 && <p>저장된 장소가 없습니다.</p>}
            </div>
            <aside className="plan-route" ref={routeRef} aria-label="오늘의 이동 동선">
              <h2 className="plan-route__title">오늘의 이동 동선</h2>
              <RouteMap items={items} selectedKey={selectedItem?.key} onSelect={setSelectedKey} />
              {selectedItem && <div className="plan-route__selected">
                <p className="plan-route__selected-title">{getItemHeading(selectedItem)}</p>
                <p className="plan-route__selected-desc">목록과 지도에서 선택한 장소를 함께 확인합니다.</p>
              </div>}
              <p className="plan-route__summary">{routeSummary}</p>
              <p className="plan-route__note">방문 순서 개념도 · 실제 지도/경로가 아닙니다. 거리는 직선거리 기준입니다.</p>
              {plan.warnings?.map((warning) => <p className="plan-route__note" key={warning}>{warning}</p>)}
              {coreItem && <button className="tp-btn tp-btn--secondary tp-btn--block" type="button" onClick={() => navigate(`/events/${coreItem.contentId}`)}>선택한 행사 다시 보기</button>}
            </aside>
          </div>
        </>}
      </main>
      <PlanFooter />
      {modal && <PlanModal title={modal.type === 'rename' ? '일정 이름 변경' : '행사 방문 시간 변경'} onClose={closeModal}>
        <form className="plan-form" onSubmit={saveModal}>
          <label className="tp-field">
            <span className="tp-field__label">{modal.type === 'rename' ? '일정 이름' : '방문 시간'}</span>
            <input type={modal.type === 'rename' ? 'text' : 'time'} value={modal.value} maxLength={modal.type === 'rename' ? TITLE_MAX_LENGTH : undefined}
              disabled={saving} required onChange={(event) => { setModal({ ...modal, value: event.target.value }); setModalError('') }} />
          </label>
          {modalError && <p className="tp-modal__error" role="alert">{modalError}</p>}
          <button className="tp-btn tp-btn--primary tp-btn--block" type="submit" disabled={saving}>{saving ? '저장 중…' : '저장'}</button>
        </form>
      </PlanModal>}
    </div>
  )
}
