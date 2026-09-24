import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import ConditionsModal from '../components/plan/ConditionsModal.jsx'
import ConfirmModal from '../components/plan/ConfirmModal.jsx'
import GeneratingModal from '../components/plan/GeneratingModal.jsx'
import PlaceChosenModal from '../components/plan/PlaceChosenModal.jsx'
import PlaceSearchModal from '../components/plan/PlaceSearchModal.jsx'
import RenameModal from '../components/plan/RenameModal.jsx'
import RouteMap from '../components/plan/RouteMap.jsx'
import TimelineItem, { getItemHeading } from '../components/plan/TimelineItem.jsx'
import TimePickerModal from '../components/plan/TimePickerModal.jsx'
import SiteFooter from '../components/layout/SiteFooter.jsx'
import SiteHeader from '../components/layout/SiteHeader.jsx'
import { getLatestDraft, getPlan, regeneratePlan, savePlan } from '../services/planService.js'
import {
  distanceMeters,
  findFreeSlot,
  findTimeConflict,
  formatDate,
  getEndTime,
  getPlanWarnings,
  sortItems,
  validatePlan,
} from '../utils/planTime.js'
import { withSubject } from '../utils/korean.js'
import '../styles/common.css'
import '../styles/plan.css'

const NEW_PLACE_DURATION = 60

function withKeys(items) {
  return items.map((item) => ({ ...item, key: `item-${item.itemId}` }))
}

// 저장 대상 필드만 비교해 "바뀐 내용이 있는지" 판단한다.
function snapshot(title, items) {
  return JSON.stringify([
    title.trim(),
    items.map(({ type, contentId, startTime, durationMin }) => [type, contentId, startTime, durationMin]),
  ])
}

function toPlaceItem(place, { key, startTime, durationMin }) {
  return {
    key,
    itemId: null,
    type: 'PLACE',
    contentId: place.contentId,
    name: place.name,
    category: place.category,
    address: place.address,
    lat: place.lat,
    lng: place.lng,
    imageUrl: place.imageUrl,
    startTime,
    durationMin,
    endTime: getEndTime({ startTime, durationMin }),
    timeFixed: false,
    aiReason: null,
    openTime: place.openTime,
    closeTime: place.closeTime,
  }
}

/**
 * SCR-009 나의 일정(저장 전 초안) · SCR-017 저장 일정 상세·편집
 * Figma: editor · 나의 하루 일정 (+ editortime / editorremoved / editoradded 상태)
 * 편집은 화면 상태에서만 하고 "일정 저장"에서 한 번에 PUT 한다(계약서 1장).
 */
export default function PlanEditorPage() {
  const { planId } = useParams()
  const navigate = useNavigate()
  const mapRef = useRef(null)
  const loadedPlanIdRef = useRef(null)
  const generationTokenRef = useRef(0)

  const [status, setStatus] = useState('loading')
  const [loadError, setLoadError] = useState('')
  const [reloadCount, setReloadCount] = useState(0)
  const [plan, setPlan] = useState(null)
  const [title, setTitle] = useState('')
  const [items, setItems] = useState([])
  const [selectedKey, setSelectedKey] = useState(null)
  const [modal, setModal] = useState(null)
  const [saving, setSaving] = useState(false)
  const [generation, setGeneration] = useState(null)

  const applyPlan = useCallback((nextPlan) => {
    const keyedItems = withKeys(sortItems(nextPlan.items))
    loadedPlanIdRef.current = String(nextPlan.planId)
    setPlan(nextPlan)
    setTitle(nextPlan.title)
    setItems(keyedItems)
    setSelectedKey(keyedItems[0]?.key ?? null)
    setStatus('ready')
  }, [])

  useEffect(() => {
    if (planId && loadedPlanIdRef.current === planId) {
      return undefined
    }

    let ignore = false
    setStatus('loading')

    const load = planId ? () => getPlan(planId) : getLatestDraft

    load()
      .then((loadedPlan) => {
        if (!ignore) {
          applyPlan(loadedPlan)
        }
      })
      .catch((error) => {
        if (ignore) {
          return
        }

        if (!planId && error.status === 404) {
          setStatus('empty')
          return
        }

        setLoadError(error.message)
        setStatus('error')
      })

    return () => {
      ignore = true
    }
  }, [planId, reloadCount, applyPlan])

  const isDirty = Boolean(plan) && snapshot(title, items) !== snapshot(plan.title, sortItems(plan.items))

  // UX-004 새로고침·탭 닫기 이탈 방지
  useEffect(() => {
    if (!isDirty) {
      return undefined
    }

    const handleBeforeUnload = (event) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty])

  const visitWindow = useMemo(() => ({
    visitStartTime: plan?.visitStartTime,
    visitEndTime: plan?.visitEndTime,
  }), [plan])

  const anchorItem = items.find((item) => item.type === 'EVENT' && item.contentId === plan?.anchorEventId)
  const selectedItem = items.find((item) => item.key === selectedKey) ?? items[0]
  const warnings = useMemo(() => getPlanWarnings(items), [items])

  const closeModal = () => setModal(null)

  // UX-004 화면 안 이동(상단 메뉴 등) 이탈 방지
  const requestNavigate = (path, action) => {
    const leave = action ?? (() => navigate(path))

    if (isDirty) {
      setModal({ type: 'leave', leave })
      return
    }

    leave()
  }

  const showTimeError = (conflict, retry) => {
    setModal({ type: 'timeError', conflict, retry })
  }

  const applyItemTime = (key, { startTime, durationMin }) => {
    const nextItems = items.map((item) => (item.key === key
      ? { ...item, startTime, durationMin, endTime: getEndTime({ startTime, durationMin }) }
      : item))
    const conflict = findTimeConflict(nextItems, visitWindow)

    if (conflict) {
      showTimeError(conflict, { type: 'time', key, startTime, durationMin })
      return
    }

    setItems(sortItems(nextItems))
    setSelectedKey(key)
    closeModal()
  }

  const replacePlace = (key, place) => {
    const newKey = `new-${place.contentId}-${Date.now()}`

    setItems((current) => current.map((item) => (item.key === key
      ? { ...toPlaceItem(place, { key: newKey, startTime: item.startTime, durationMin: item.durationMin }), seq: item.seq }
      : item)))
    setSelectedKey(newKey)
    closeModal()
  }

  const addPlace = ({ place, startTime, durationMin }) => {
    const newItem = toPlaceItem(place, { key: `new-${place.contentId}-${Date.now()}`, startTime, durationMin })
    const nextItems = [...items, newItem]
    const conflict = findTimeConflict(nextItems, visitWindow)

    if (conflict) {
      showTimeError(conflict, { type: 'time', mode: 'add', place, startTime, durationMin })
      return
    }

    setItems(sortItems(nextItems))
    setSelectedKey(newItem.key)
    closeModal()
  }

  const removeItem = (key) => {
    setItems((current) => sortItems(current.filter((item) => item.key !== key)))
    setSelectedKey((current) => (current === key ? null : current))
    closeModal()
  }

  const handleSave = async () => {
    const error = validatePlan({ title, anchorEventId: plan.anchorEventId, items, ...visitWindow })

    if (error) {
      setModal({ type: 'notice', title: error.title, message: error.message })
      return
    }

    setSaving(true)

    try {
      const saved = await savePlan(plan.planId, { title, tripDate: plan.tripDate, anchorEventId: plan.anchorEventId, items })
      applyPlan(saved)
      setModal({ type: 'saved', plan: saved })

      if (String(saved.planId) !== planId) {
        navigate(`/my-trips/${saved.planId}`, { replace: true })
      }
    } catch (saveError) {
      setModal({ type: 'notice', title: '저장하지 못했어요', message: saveError.message })
    } finally {
      setSaving(false)
    }
  }

  const startGeneration = (conditions) => {
    const unchanged = conditions
      && conditions.headcount === plan.headcount
      && conditions.transportMode === plan.transportMode
      && conditions.useAi === plan.aiGenerated
      && [...conditions.interests].sort().join() === [...(plan.interests ?? [])].sort().join()

    // AI-009 예외: 조건이 그대로면 요청하지 않고 기존 초안을 유지한다.
    if (unchanged) {
      setModal({ type: 'notice', title: '조건이 그대로예요', message: '조건이 바뀌지 않아 기존 일정을 유지합니다.' })
      return
    }

    const token = generationTokenRef.current + 1
    generationTokenRef.current = token
    setGeneration({ status: 'running' })
    setModal({ type: 'generating' })

    regeneratePlan(plan.planId, conditions)
      .then((result) => {
        if (generationTokenRef.current === token) {
          setGeneration({ status: 'done', result })
        }
      })
      .catch((error) => {
        if (generationTokenRef.current === token) {
          setGeneration({ status: 'error', message: error.message })
        }
      })
  }

  const cancelGeneration = () => {
    generationTokenRef.current += 1
    setGeneration(null)
    closeModal()
  }

  const showOnMap = (item) => {
    setSelectedKey(item.key)
    mapRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }

  const renderModal = () => {
    if (!modal) {
      return null
    }

    switch (modal.type) {
      case 'time': {
        const isAdd = modal.mode === 'add'
        const backToChosen = (time) => setModal({ type: 'placeChosen', place: modal.place, ...time })

        return (
          <TimePickerModal
            title={isAdd ? '방문 시간 선택' : '방문 시간 변경'}
            startTime={modal.startTime}
            durationMin={modal.durationMin}
            onApply={(time) => (isAdd ? backToChosen(time) : applyItemTime(modal.key, time))}
            onClose={isAdd ? () => backToChosen({ startTime: modal.startTime, durationMin: modal.durationMin }) : closeModal}
          />
        )
      }
      case 'timeError':
        return (
          <ConfirmModal
            title={modal.conflict.title}
            description={modal.conflict.message}
            confirmLabel="시간 다시 선택"
            onConfirm={() => setModal(modal.retry)}
            onClose={closeModal}
          />
        )
      case 'placeChange': {
        const target = items.find((item) => item.key === modal.key)

        return (
          <PlaceSearchModal
            mode="change"
            eventId={plan.anchorEventId}
            usedContentIds={items.map((item) => item.contentId)}
            time={target?.startTime}
            durationMin={target?.durationMin}
            onConfirm={(place) => replacePlace(modal.key, place)}
            onClose={closeModal}
          />
        )
      }
      case 'placeAdd':
        return (
          <PlaceSearchModal
            mode="add"
            eventId={plan.anchorEventId}
            usedContentIds={items.map((item) => item.contentId)}
            onConfirm={(place) => setModal({
              type: 'placeChosen',
              place,
              startTime: findFreeSlot(items, NEW_PLACE_DURATION, visitWindow),
              durationMin: NEW_PLACE_DURATION,
            })}
            onClose={closeModal}
          />
        )
      case 'placeChosen':
        return (
          <PlaceChosenModal
            place={modal.place}
            startTime={modal.startTime}
            durationMin={modal.durationMin}
            onEditTime={() => setModal({
              ...modal,
              type: 'time',
              mode: 'add',
              startTime: modal.startTime ?? plan.visitStartTime ?? '12:00',
            })}
            onConfirm={() => addPlace(modal)}
            onClose={closeModal}
          />
        )
      case 'remove': {
        const target = items.find((item) => item.key === modal.key)

        return (
          <ConfirmModal
            title="이 장소를 일정에서 뺄까요?"
            description={`${withSubject(target?.name ?? '선택한 장소')} 일정에서 삭제됩니다.`}
            confirmLabel="장소 삭제"
            confirmVariant="danger"
            cancelLabel="취소"
            onConfirm={() => removeItem(modal.key)}
            onClose={closeModal}
          />
        )
      }
      case 'rename':
        return (
          <RenameModal
            title={title}
            onSave={(nextTitle) => {
              setTitle(nextTitle)
              closeModal()
            }}
            onClose={closeModal}
          />
        )
      case 'saved':
        return (
          <ConfirmModal
            title="일정을 저장했어요"
            description={`${modal.plan.title}\n${formatDate(modal.plan.tripDate)} · 당일 여행`}
            confirmLabel="저장한 일정 확인"
            cancelLabel="계속 편집"
            onConfirm={() => navigate('/mypage')}
            onClose={closeModal}
          />
        )
      case 'regenerate':
        return (
          <ConfirmModal
            title="새로운 일정으로 추천받을까요?"
            description="지금 편집한 일정은 새 추천으로 바뀝니다. 선택한 행사는 유지됩니다."
            confirmLabel="다시 추천받기"
            cancelLabel="현재 일정 유지"
            onConfirm={() => startGeneration(null)}
            onClose={closeModal}
          />
        )
      case 'conditions':
        return (
          <ConditionsModal
            plan={plan}
            anchorItem={anchorItem}
            onSubmit={startGeneration}
            onClose={closeModal}
          />
        )
      case 'generating':
        return (
          <GeneratingModal
            status={generation?.status ?? 'running'}
            errorMessage={generation?.message}
            onConfirm={() => {
              applyPlan(generation.result)
              setGeneration(null)
              closeModal()
            }}
            onCancel={cancelGeneration}
          />
        )
      case 'leave':
        return (
          <ConfirmModal
            title="저장하지 않고 나갈까요?"
            description="지금까지 바꾼 일정은 저장되지 않습니다."
            confirmLabel="저장하지 않고 나가기"
            confirmVariant="danger"
            cancelLabel="계속 편집"
            onConfirm={() => {
              closeModal()
              modal.leave()
            }}
            onClose={closeModal}
          />
        )
      case 'notice':
        return (
          <ConfirmModal
            title={modal.title}
            description={modal.message}
            confirmLabel="확인"
            onConfirm={closeModal}
            onClose={closeModal}
          />
        )
      default:
        return null
    }
  }

  if (status !== 'ready') {
    return (
      <div className="plan-page">
        <SiteHeader />
        <main className="plan-status">
          {status === 'loading' && <p aria-live="polite">일정을 불러오는 중이에요…</p>}
          {status === 'empty' && (
            <>
              <h1>아직 만든 일정이 없어요</h1>
              <p>문화행사를 고르고 추천을 받아 하루 일정을 만들어 보세요.</p>
              <button className="tp-btn tp-btn--primary" type="button" onClick={() => navigate('/')}>
                문화행사 둘러보기
              </button>
            </>
          )}
          {status === 'error' && (
            <>
              <h1>일정을 불러오지 못했어요</h1>
              <p>{loadError}</p>
              <button className="tp-btn tp-btn--secondary" type="button" onClick={() => setReloadCount((count) => count + 1)}>
                다시 시도
              </button>
            </>
          )}
        </main>
        <SiteFooter />
      </div>
    )
  }

  const routeSummary = items.map((item) => `${item.seq} ${item.name}`).join(' → ')

  return (
    <div className="plan-page">
      <SiteHeader onNavigate={requestNavigate} />

      <section className="plan-heading">
        <p className="plan-heading__eyebrow">
          {`나의 일정 · ${plan.saved ? '저장한 일정' : '저장 전 초안'} · ${formatDate(plan.tripDate)}`}
        </p>
        <h1 className="plan-heading__title">{title}</h1>
        <p className="plan-heading__desc">
          {anchorItem?.timeFixed
            ? `추천 시간은 조정 가능한 예시입니다. 선택한 행사는 ${anchorItem.startTime}에 시작해요.`
            : '추천 시간은 조정 가능한 예시입니다. 실제 행사 운영시간은 정보 없음.'}
        </p>
        <button
          className="plan-heading__rename"
          type="button"
          aria-label="일정 이름 변경"
          onClick={() => setModal({ type: 'rename' })}
        >
          ✎
        </button>
      </section>

      <main className="plan-editor">
        <div className="plan-toolbar">
          <p className="plan-toolbar__desc">당일 여행 · 선택한 행사에 맞춘 동선</p>
          {!plan.saved && (
            <>
              <button className="tp-btn tp-btn--secondary plan-toolbar__button" type="button" onClick={() => setModal({ type: 'conditions' })}>
                조건 수정
              </button>
              <button className="tp-btn tp-btn--secondary plan-toolbar__button" type="button" onClick={() => setModal({ type: 'regenerate' })}>
                다시 추천
              </button>
            </>
          )}
          <button
            className="tp-btn tp-btn--primary plan-toolbar__save"
            type="button"
            disabled={saving || (plan.saved && !isDirty)}
            onClick={handleSave}
          >
            {saving ? '저장 중…' : plan.saved ? '변경 저장' : '일정 저장'}
          </button>
        </div>

        <div className="plan-layout">
          <div className="plan-timeline">
            {items.map((item, index) => (
              <TimelineItem
                key={item.key}
                item={item}
                isAnchor={item === anchorItem}
                isLast={index === items.length - 1}
                isSelected={item.key === selectedItem?.key}
                distanceToNext={distanceMeters(item, items[index + 1])}
                onEditTime={() => setModal({ type: 'time', key: item.key, startTime: item.startTime, durationMin: item.durationMin })}
                onChangePlace={() => setModal({ type: 'placeChange', key: item.key })}
                onRemove={() => setModal({ type: 'remove', key: item.key })}
                onShowOnMap={() => showOnMap(item)}
              />
            ))}
            <button className="tp-btn tp-btn--secondary tp-btn--block" type="button" onClick={() => setModal({ type: 'placeAdd' })}>
              + 장소 직접 추가
            </button>
          </div>

          <aside className="plan-route" ref={mapRef} aria-label="오늘의 이동 동선">
            <h2 className="plan-route__title">오늘의 이동 동선</h2>
            <RouteMap items={items} selectedKey={selectedItem?.key} onSelect={setSelectedKey} />

            {selectedItem && (
              <div className="plan-route__selected">
                <p className="plan-route__selected-title">{getItemHeading(selectedItem)}</p>
                <p className="plan-route__selected-desc">목록과 지도에서 선택한 장소를 함께 확인합니다.</p>
              </div>
            )}

            <p className="plan-route__summary">{routeSummary}</p>
            <p className="plan-route__note">방문 순서 개념도 · 실제 지도/경로가 아닙니다. 거리는 직선거리 기준입니다.</p>

            <button
              className="tp-btn tp-btn--secondary tp-btn--block"
              type="button"
              onClick={() => requestNavigate(`/events/${plan.anchorEventId}`)}
            >
              선택한 행사 다시 보기
            </button>

            <div className="plan-basis">
              <h3 className="plan-basis__title">코스 구성 기준</h3>
              <p className="plan-basis__text">행사: 검색 조건 일치 · 맛집: 행사장 거리 + 식사 시간대 + 영업시간</p>
              {warnings.length > 0 ? (
                <ul className="plan-basis__warnings">
                  {warnings.map((warning) => <li key={`${warning.code}-${warning.message}`}>{warning.message}</li>)}
                </ul>
              ) : (
                <p className="plan-basis__note">모든 장소가 방문 시간에 영업 중이에요.</p>
              )}
            </div>
          </aside>
        </div>
      </main>

      <SiteFooter />
      {renderModal()}
    </div>
  )
}
