import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import ConditionsModal from '../components/plan/ConditionsModal.jsx'
import ConfirmModal from '../components/plan/ConfirmModal.jsx'
import GeneratingModal from '../components/plan/GeneratingModal.jsx'
import PlaceChosenModal from '../components/plan/PlaceChosenModal.jsx'
import PlaceSearchModal from '../components/plan/PlaceSearchModal.jsx'
import PlanFooter from '../components/plan/PlanFooter.jsx'
import PlanHeader from '../components/plan/PlanHeader.jsx'
import RenameModal from '../components/plan/RenameModal.jsx'
import RouteMap from '../components/plan/RouteMap.jsx'
import TimelineItem, { getItemHeading } from '../components/plan/TimelineItem.jsx'
import TimePickerModal from '../components/plan/TimePickerModal.jsx'
import {
  addDraftItem,
  forgetDraftId,
  getDraft,
  regenerateDraft,
  removeDraftItem,
  replaceDraftItems,
  resolveDraftId,
  saveDraftAsPlan,
  updateConditions,
  updateDraftTitle,
} from '../services/planService.js'
import {
  distanceMeters,
  findFreeSlot,
  findTimeConflict,
  formatDate,
  getEndTime,
  getPlanWarnings,
  sortItems,
  toMinutes,
  toTime,
  validatePlan,
} from '../utils/planTime.js'
import { withSubject } from '../utils/korean.js'
import '../styles/plan-base.css'
import '../styles/plan.css'

const NEW_PLACE_DURATION = 60
// 이 시간 이상 비는 구간은 카드 사이에 알려 준다
const FREE_GAP_MINUTES = 120

function withKeys(items) {
  return items.map((item) => ({ ...item, key: `item-${item.itemId}` }))
}

// 저장 대상 필드만 비교해 "바뀐 내용이 있는지" 판단한다.
function snapshot(title, items) {
  return JSON.stringify([
    title.trim(),
    items.map(({ type, placeId, startTime, durationMin }) => [type, placeId, startTime, durationMin]),
  ])
}

/**
 * 저장이 중간에 실패한 뒤 서버 상태와 화면 편집본을 맞춘다 (편집 내용은 그대로 둔다).
 * - 이미 서버에 추가된 새 항목은 그 itemId 를 이어받아 다시 저장할 때 중복 추가(409)가 나지 않게 한다
 * - 서버에 더 이상 없는 항목은 새 항목으로 다시 추가되게 itemId 를 비운다
 */
function reconcileWithServer(localItems, serverItems) {
  const serverIds = new Set(serverItems.map((item) => item.itemId))
  const kept = localItems.map((item) => (item.itemId != null && !serverIds.has(item.itemId) ? { ...item, itemId: null } : item))
  const usedIds = new Set(kept.filter((item) => item.itemId != null).map((item) => item.itemId))

  return kept.map((item) => {
    if (item.itemId != null) {
      return item
    }

    const added = serverItems.find((server) => !usedIds.has(server.itemId)
      && server.type === item.type && server.placeId === item.placeId)

    if (!added) {
      return item
    }

    usedIds.add(added.itemId)
    return { ...item, itemId: added.itemId }
  })
}

// API-PLACE-001 후보 → 화면 일정 항목 (아직 서버에 추가되지 않아 itemId 없음)
function toNewItem(place, { key, startTime, durationMin }) {
  return {
    key,
    itemId: null,
    type: 'PLACE',
    placeId: place.placeId,
    sequence: 0,
    startTime,
    durationMin,
    name: place.name,
    addr: place.addr,
    lat: place.lat,
    lng: place.lng,
    imageUrl: place.imageUrl,
    openTime: place.openTime,
    breakTime: place.breakTime,
    closeTime: place.closeTime,
    timeFixed: false,
  }
}

/**
 * SCR-009 나의 일정(저장 전 초안) — /trips/draft
 * Figma: editor · 나의 하루 일정 (+ editortime / editorremoved / editoradded 상태)
 * API: 조회 PLAN-002, 조건 수정 PLAN-003 → 다시 추천 PLAN-004, 저장 시 PLAN-008·007·006·005 반영 후 PLAN-009
 * 팝업에서 바꾼 내용은 화면에만 두었다가 "일정 저장"에서 한 번에 서버에 반영한다(UX-004 미저장 이탈 확인).
 */
/**
 * 장소 변경 팝업의 음식 종류 — 바꾸려는 항목 시각이 점심·저녁 시간대면 그 끼니 음식 종류(docs/07 [제안]),
 * 그 밖의 시각(카페 등)은 두 끼 공통 값(다르면 전체)으로 찾는다.
 */
function foodForTime(conditions, time) {
  if (!conditions) return undefined
  if (time && time >= '11:00' && time <= '14:30') return conditions.lunchFoodPreference ?? conditions.foodPreference
  if (time && time >= '17:00' && time <= '20:30') return conditions.dinnerFoodPreference ?? conditions.foodPreference
  return conditions.foodPreference ?? undefined
}

export default function PlanEditorPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const mapRef = useRef(null)
  const generationTokenRef = useRef(0)
  // 추천 요청은 서버에서 되돌릴 수 없어, 끝나기 전에는 새 요청을 받지 않는다
  const generationRunningRef = useRef(false)
  const autoRegenerateRef = useRef(Boolean(location.state?.autoRegenerate))
  const [draftId] = useState(() => resolveDraftId(location.state?.draftId))

  const [status, setStatus] = useState('loading')
  const [loadError, setLoadError] = useState('')
  const [reloadCount, setReloadCount] = useState(0)
  const [draft, setDraft] = useState(null)
  const [title, setTitle] = useState('')
  const [items, setItems] = useState([])
  const [selectedKey, setSelectedKey] = useState(null)
  const [modal, setModal] = useState(null)
  const [saving, setSaving] = useState(false)
  const [generation, setGeneration] = useState(null)
  const [savedPlan, setSavedPlan] = useState(null)

  const applyDraft = useCallback((nextDraft) => {
    const keyedItems = withKeys(sortItems(nextDraft.items))
    setDraft(nextDraft)
    setTitle(nextDraft.title)
    setItems(keyedItems)
    setSelectedKey(keyedItems[0]?.key ?? null)
    setStatus('ready')
  }, [])

  useEffect(() => {
    if (!draftId) {
      setStatus('empty')
      return undefined
    }

    let ignore = false
    setStatus('loading')

    getDraft(draftId)
      .then((loadedDraft) => {
        if (!ignore) {
          applyDraft(loadedDraft)
        }
      })
      .catch((error) => {
        if (ignore) {
          return
        }

        if (error.status === 404) {
          forgetDraftId()
          setStatus('empty')
          return
        }

        setLoadError(error.message)
        setStatus('error')
      })

    return () => {
      ignore = true
    }
  }, [draftId, reloadCount, applyDraft])

  const isDirty = Boolean(draft) && !savedPlan && snapshot(title, items) !== snapshot(draft.title, sortItems(draft.items))

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

  const timeWindow = useMemo(() => ({
    startTime: draft?.conditions?.startTime,
    endTime: draft?.conditions?.endTime,
  }), [draft])

  // 핵심 문화행사: 추천 기준 행사(selectedEvent). 없으면 첫 행사 항목
  const coreEventPlaceId = draft?.selectedEvent?.eventId
    ?? items.find((item) => item.type === 'EVENT')?.placeId
  const coreItem = items.find((item) => item.type === 'EVENT' && item.placeId === coreEventPlaceId)
  const selectedItem = items.find((item) => item.key === selectedKey) ?? items[0]
  const warnings = useMemo(() => getPlanWarnings(items), [items])
  const reasonByItemId = useMemo(
    () => new Map((draft?.recommendationReasons ?? []).map(({ itemId, reason }) => [itemId, reason])),
    [draft],
  )
  const serverItemById = useMemo(
    () => new Map((draft?.items ?? []).map((item) => [item.itemId, item])),
    [draft],
  )
  // 추천 이유에는 식사 시각이 들어가므로, 화면에서 장소나 시간을 바꾼 항목은 이유를 숨긴다 (AI-004·007)
  const reasonFor = (item) => {
    const server = serverItemById.get(item.itemId)
    const unchanged = server && server.placeId === item.placeId
      && server.startTime === item.startTime && server.durationMin === item.durationMin
    return unchanged ? reasonByItemId.get(item.itemId) : undefined
  }

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

  // TRIP-004 방문 시간 변경
  const applyItemTime = (key, { startTime, durationMin }) => {
    const nextItems = items.map((item) => (item.key === key
      ? { ...item, startTime, durationMin, endTime: getEndTime({ startTime, durationMin }) }
      : item))
    const conflict = findTimeConflict(nextItems, timeWindow)

    if (conflict) {
      showTimeError(conflict, { type: 'time', key, startTime, durationMin })
      return
    }

    setItems(sortItems(nextItems))
    setSelectedKey(key)
    closeModal()
  }

  // TRIP-005 장소 변경 — 같은 항목(itemId)의 장소만 바꿔 저장 때 API-PLAN-006 으로 한 번에 반영한다
  const replacePlace = (key, place) => {
    setItems((current) => current.map((item) => (item.key === key
      ? {
        ...toNewItem(place, { key, startTime: item.startTime, durationMin: item.durationMin }),
        itemId: item.itemId,
        sequence: item.sequence,
      }
      : item)))
    setSelectedKey(key)
    closeModal()
  }

  // TRIP-006 장소 추가
  const addPlace = ({ place, startTime, durationMin }) => {
    const newItem = toNewItem(place, { key: `new-${place.placeId}-${Date.now()}`, startTime, durationMin })
    const nextItems = [...items, newItem]
    const conflict = findTimeConflict(nextItems, timeWindow)

    if (conflict) {
      showTimeError(conflict, { type: 'time', mode: 'add', place, startTime, durationMin })
      return
    }

    setItems(sortItems(nextItems))
    setSelectedKey(newItem.key)
    closeModal()
  }

  // TRIP-007 일정 장소 삭제
  const removeItem = (key) => {
    setItems((current) => sortItems(current.filter((item) => item.key !== key)))
    setSelectedKey((current) => (current === key ? null : current))
    closeModal()
  }

  /**
   * 화면에서 바꾼 내용을 API 명세 엔드포인트로 서버 초안에 반영한다.
   * 삭제(008) → 남은 항목 시간·순서(006) → 새 항목 추가(007) → 최종 순서(006, 달라졌을 때만) → 제목(005)
   * 남은 항목 시간을 먼저 맞춰야 새 항목 추가 때 옛 시간과 겹쳤다는 오류가 나지 않는다.
   */
  const syncDraft = async () => {
    const keptItemIds = new Set(items.filter((item) => item.itemId != null).map((item) => item.itemId))

    for (const item of draft.items.filter((candidate) => !keptItemIds.has(candidate.itemId))) {
      await removeDraftItem(draftId, item.itemId)
    }

    const keptItems = sortItems(items.filter((item) => item.itemId != null))
    const serverKept = sortItems(draft.items.filter((item) => keptItemIds.has(item.itemId)))
    let serverItems = serverKept

    if (snapshot('', keptItems) !== snapshot('', serverKept)) {
      serverItems = (await replaceDraftItems(draftId, keptItems)).items
    }

    const finalItems = sortItems(items)
    const addedItemIds = new Map()

    for (const item of finalItems.filter((candidate) => candidate.itemId == null)) {
      const response = await addDraftItem(draftId, {
        placeId: item.placeId,
        type: item.type,
        startTime: item.startTime,
        durationMin: item.durationMin,
        sequence: item.sequence,
      })
      addedItemIds.set(item.key, response.itemId)
      serverItems = response.items
    }

    const orderedItems = finalItems.map((item) => ({ ...item, itemId: item.itemId ?? addedItemIds.get(item.key) }))
    const serverOrder = sortItems(serverItems).map((item) => item.itemId).join()

    if (orderedItems.map((item) => item.itemId).join() !== serverOrder) {
      await replaceDraftItems(draftId, orderedItems)
    }

    if (title.trim() !== draft.title) {
      await updateDraftTitle(draftId, title.trim())
    }
  }

  // TRIP-010 일정 저장: 초안 반영 후 API-PLAN-009로 확정
  const handleSave = async () => {
    const error = validatePlan({ title, items, coreEventPlaceId, ...timeWindow })

    if (error) {
      setModal({ type: 'notice', title: error.title, message: error.message })
      return
    }

    setSaving(true)

    try {
      await syncDraft()
      const saved = await saveDraftAsPlan(draftId)
      forgetDraftId()
      // 저장된 일정은 더 이상 초안이 아니라 초안 조회(002)는 404 — 방금 저장한 화면 내용을 그대로 둔다
      applyDraft({ ...draft, title, items })
      // 저장된 초안은 다시 저장할 수 없다(API-PLAN-009 409). 이후 편집은 저장 일정 상세(SCR-017)에서
      setSavedPlan(saved)
      setModal({ type: 'saved', plan: saved })
    } catch (saveError) {
      // 앞 단계(삭제·수정·추가)는 이미 서버에 반영됐을 수 있다. 편집한 내용은 그대로 두고
      // 서버 상태만 다시 받아, 다시 저장하면 남은 부분만 이어서 반영되게 한다
      const serverDraft = await getDraft(draftId).catch(() => null)

      if (serverDraft) {
        setDraft(serverDraft)
        setItems((current) => reconcileWithServer(current, serverDraft.items))
      }

      setModal({
        type: 'notice',
        title: '저장하지 못했어요',
        message: `${saveError.message}\n편집한 내용은 그대로 두었어요. 확인 후 다시 저장해 주세요.`,
      })
    } finally {
      setSaving(false)
    }
  }

  /**
   * AI-009 조건 수정: API-PLAN-003 이 조건과 일정을 한 번에 바꾼다 → 002 로 다시 불러온다
   * AI-010 다시 추천: API-PLAN-004 → 002
   * 실패하면 서버 초안은 그대로다. 취소해도 이미 보낸 요청은 서버에서 끝까지 처리되므로,
   * 성공했다면 화면을 서버 결과로 맞추고 알린다 (예전 항목 번호로 저장하다 404 가 나지 않게).
   */
  const startGeneration = (conditionChanges) => {
    if (generationRunningRef.current) {
      setModal({ type: 'notice', title: '아직 추천을 만들고 있어요', message: '앞서 요청한 추천이 끝난 뒤 다시 시도해 주세요.' })
      return
    }

    generationRunningRef.current = true
    const token = generationTokenRef.current + 1
    generationTokenRef.current = token
    setGeneration({ status: 'running' })
    setModal({ type: 'generating' })

    const run = async () => {
      if (conditionChanges) {
        await updateConditions(draftId, { ...draft.conditions, visitDate: draft.visitDate, ...conditionChanges })
      } else {
        await regenerateDraft(draftId)
      }

      return getDraft(draftId)
    }

    run()
      .then((result) => {
        if (generationTokenRef.current === token) {
          setGeneration({ status: 'done', result })
          return
        }

        applyDraft(result)
        setModal({
          type: 'notice',
          title: '새 추천 일정이 반영되었어요',
          message: '취소하기 전에 요청이 이미 처리되어 새 일정으로 바뀌었어요.',
        })
      })
      .catch((error) => {
        if (generationTokenRef.current === token) {
          setGeneration({ status: 'error', message: error.message })
        }
      })
      .finally(() => {
        generationRunningRef.current = false
      })
  }

  const cancelGeneration = () => {
    generationTokenRef.current += 1
    setGeneration(null)
    closeModal()
  }

  useEffect(() => {
    if (status === 'ready' && autoRegenerateRef.current) {
      autoRegenerateRef.current = false
      startGeneration(null)
    }
  }, [status])

  // TRIP-009 목록·지도 선택 연동
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
            eventId={coreEventPlaceId}
            usedPlaceIds={items.map((item) => item.placeId)}
            time={target?.startTime}
            durationMin={target?.durationMin}
            foodPreference={foodForTime(draft?.conditions, target?.startTime)}
            mealType={draft?.conditions?.mealType}
            onConfirm={(place) => replacePlace(modal.key, place)}
            onClose={closeModal}
          />
        )
      }
      case 'placeAdd':
        return (
          <PlaceSearchModal
            mode="add"
            eventId={coreEventPlaceId}
            usedPlaceIds={items.map((item) => item.placeId)}
            foodPreference={draft?.conditions?.foodPreference}
            mealType={draft?.conditions?.mealType}
            onConfirm={(place) => setModal({
              type: 'placeChosen',
              place,
              // 빈 시간 안내에서 들어오면 그 빈 시간에, 아니면 처음 비는 시간에
              startTime: modal.startTime ?? findFreeSlot(items, NEW_PLACE_DURATION, timeWindow),
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
              startTime: modal.startTime ?? timeWindow.startTime ?? '12:00',
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
            description={`${modal.plan.title}\n${formatDate(modal.plan.visitDate)} · 당일 여행`}
            confirmLabel="저장한 일정 확인"
            cancelLabel="계속 편집"
            onConfirm={() => navigate('/my-trips')}
            onCancel={() => navigate(`/my-trips/${modal.plan.planId}`)}
            onClose={closeModal}
          />
        )
      case 'regenerate':
        return (
          <ConfirmModal
            title="새로운 일정으로 추천받을까요?"
            description={draft.conditions?.availableDates?.length
              ? '지금 편집한 일정은 새 추천으로 바뀝니다. 조건에 맞는 다른 행사와 맛집을 찾아봅니다.'
              : '지금 편집한 일정은 새 추천으로 바뀝니다. 선택한 행사는 유지됩니다.'}
            confirmLabel="다시 추천받기"
            cancelLabel="현재 일정 유지"
            onConfirm={() => startGeneration(null)}
            onClose={closeModal}
          />
        )
      case 'conditions':
        return (
          <ConditionsModal
            visitDate={draft.visitDate}
            conditions={draft.conditions}
            selectedEvent={draft.selectedEvent}
            coreItem={coreItem}
            onSubmit={({
              mealType, transportMode, lunchFoodPreference, dinnerFoodPreference, includeCafe,
              visitDate, availableDates, categories, district, freeYn,
            }) => startGeneration({
              mealType, transportMode, lunchFoodPreference, dinnerFoodPreference, includeCafe, foodPreference: undefined,
              // 행사를 직접 고른 초안은 방문 날짜만, 조건으로 만든 초안은 날짜들·분야·지역·무료 여부를 보낸다
              ...(visitDate ? { visitDate } : {}),
              ...(availableDates ? { availableDates, categories, district, freeYn } : {}),
            })}
            onClose={closeModal}
          />
        )
      case 'generating':
        return (
          <GeneratingModal
            status={generation?.status ?? 'running'}
            errorMessage={generation?.message}
            onConfirm={() => {
              applyDraft(generation.result)
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
        <PlanHeader />
        <main className="plan-status">
          {status === 'loading' && <p aria-live="polite">일정을 불러오는 중이에요…</p>}
          {status === 'empty' && (
            <>
              <h1>아직 만든 일정 초안이 없어요</h1>
              <p>문화행사를 고르고 AI 추천을 받아 하루 일정을 만들어 보세요.</p>
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
        <PlanFooter />
      </div>
    )
  }

  const routeSummary = items.map((item) => `${item.sequence} ${item.name}`).join(' → ')

  return (
    <div className="plan-page">
      <PlanHeader onNavigate={requestNavigate} />

      <section className="plan-heading">
        <p className="plan-heading__eyebrow">{`나의 일정 · ${savedPlan ? '저장 완료' : '저장 전 초안'} · ${formatDate(draft.visitDate)}`}</p>
        <h1 className="plan-heading__title">{title}</h1>
        <p className="plan-heading__desc">
          {coreItem?.timeFixed
            ? `추천 시간은 조정 가능한 예시입니다. 선택한 행사는 ${coreItem.startTime}에 시작해요.`
            : '추천 시간은 조정 가능한 예시입니다. 실제 행사 운영시간은 정보 없음.'}
        </p>
        <button
          className="plan-heading__rename"
          disabled={Boolean(savedPlan)}
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
          <button className="tp-btn tp-btn--secondary plan-toolbar__button" type="button" disabled={Boolean(savedPlan)} onClick={() => setModal({ type: 'conditions' })}>
            조건 수정
          </button>
          <button className="tp-btn tp-btn--secondary plan-toolbar__button" type="button" disabled={Boolean(savedPlan)} onClick={() => setModal({ type: 'regenerate' })}>
            다시 추천
          </button>
          {savedPlan ? (
            <button className="tp-btn tp-btn--primary plan-toolbar__save" type="button" onClick={() => setModal({ type: 'saved', plan: savedPlan })}>
              저장 완료 · 확인
            </button>
          ) : (
            <button className="tp-btn tp-btn--primary plan-toolbar__save" type="button" disabled={saving} onClick={handleSave}>
              {saving ? '저장 중…' : '일정 저장'}
            </button>
          )}
        </div>

        <div className="plan-layout">
          <div className="plan-timeline">
            {items.map((item, index) => (
              <React.Fragment key={item.key}>
              <TimelineItem
                key={item.key}
                item={item}
                isCore={item === coreItem}
                reason={reasonFor(item)}
                isLast={index === items.length - 1}
                isSelected={item.key === selectedItem?.key}
                distanceToNext={distanceMeters(item, items[index + 1])}
                locked={Boolean(savedPlan)}
                onEditTime={() => setModal({ type: 'time', key: item.key, startTime: item.startTime, durationMin: item.durationMin })}
                onChangePlace={() => setModal({ type: 'placeChange', key: item.key })}
                onRemove={() => setModal({ type: 'remove', key: item.key })}
                onShowOnMap={() => showOnMap(item)}
              />
              {(() => {
                // 다음 일정까지 2시간 이상 비면 알려 주고 그 자리에 바로 장소를 넣을 수 있게 한다
                const next = items[index + 1]
                if (!next || !item.startTime || !next.startTime) return null
                const gapStart = toMinutes(item.startTime) + item.durationMin
                const gap = toMinutes(next.startTime) - gapStart
                if (gap < FREE_GAP_MINUTES) return null
                return (
                  <div className="plan-gap" role="note">
                    <span className="plan-gap__text">
                      {`${toTime(gapStart)}~${next.startTime} · ${Math.floor(gap / 60)}시간${gap % 60 ? ` ${gap % 60}분` : ''} 비어 있어요`}
                    </span>
                    <button
                      className="tp-btn tp-btn--secondary plan-gap__add"
                      type="button"
                      disabled={Boolean(savedPlan)}
                      onClick={() => setModal({ type: 'placeAdd', startTime: toTime(gapStart) })}
                    >
                      + 이 시간에 장소 추가
                    </button>
                  </div>
                )
              })()}
              </React.Fragment>
            ))}
            <button className="tp-btn tp-btn--secondary tp-btn--block" type="button" disabled={Boolean(savedPlan)} onClick={() => setModal({ type: 'placeAdd' })}>
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
              onClick={() => requestNavigate(`/events/${coreEventPlaceId}`)}
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
                <p className="plan-basis__note">모든 맛집이 방문 시간에 영업 중이에요.</p>
              )}
            </div>
          </aside>
        </div>
      </main>

      <PlanFooter />
      {renderModal()}
    </div>
  )
}
