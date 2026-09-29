import React, { useEffect, useMemo, useRef, useState } from 'react'
import mapImage from '../../assets/plan/route-concept-map.svg'
import ringImage from '../../assets/plan/marker-ring.svg'
import { loadKakaoMaps } from '../../utils/kakaoMap.js'
import '../../styles/route-map.css'

// Figma 개념도 크기 (642 × 470)
const MAP_WIDTH = 642
const MAP_HEIGHT = 470
const PADDING = 60
const ROUTE_COLOR = '#2563eb'
// 지도 가장자리와 마커 사이 여백 (px) — 번호 마커가 잘리지 않게
const BOUNDS_PADDING = 48

const NOTES = {
  kakao: '선은 방문 순서를 직선으로 이은 것으로 실제 이동 경로와 다를 수 있습니다. 거리는 직선거리 기준입니다.',
  concept: '방문 순서 개념도 · 실제 지도/경로가 아닙니다. 거리는 직선거리 기준입니다.',
}

function locatedItems(items) {
  return items.filter((item) => item.lat != null && item.lng != null)
}

/** 방문 순서 개념도 — 카카오맵을 쓸 수 없을 때(키 없음·불러오기 실패) 좌표를 개념도 안에 비율로 배치한다. */
function toPoints(items) {
  const located = locatedItems(items)

  if (located.length === 0) {
    return []
  }

  const lats = located.map((item) => item.lat)
  const lngs = located.map((item) => item.lng)
  const minLat = Math.min(...lats)
  const maxLat = Math.max(...lats)
  const minLng = Math.min(...lngs)
  const maxLng = Math.max(...lngs)
  const spanLat = maxLat - minLat || 1
  const spanLng = maxLng - minLng || 1

  return located.map((item) => ({
    item,
    x: located.length === 1 ? MAP_WIDTH / 2 : PADDING + ((item.lng - minLng) / spanLng) * (MAP_WIDTH - PADDING * 2),
    y: located.length === 1 ? MAP_HEIGHT / 2 : PADDING + ((maxLat - item.lat) / spanLat) * (MAP_HEIGHT - PADDING * 2),
  }))
}

function ConceptMap({ items, selectedKey, onSelect }) {
  const points = useMemo(() => toPoints(items), [items])

  return (
    <div className="plan-map" role="group" aria-label="일정 순서 안내 지도">
      <img className="plan-map__background" src={mapImage} alt="" />
      <svg className="plan-map__route" viewBox={`0 0 ${MAP_WIDTH} ${MAP_HEIGHT}`} preserveAspectRatio="none" aria-hidden="true">
        <polyline
          points={points.map(({ x, y }) => `${x},${y}`).join(' ')}
          fill="none"
          stroke={ROUTE_COLOR}
          strokeWidth="4"
          strokeDasharray="8 7"
          vectorEffect="non-scaling-stroke"
        />
      </svg>
      {points.map(({ item, x, y }) => (
        <button
          key={item.key}
          className={`tp-marker plan-map__marker${item.key === selectedKey ? ' is-selected' : ''}`}
          type="button"
          style={{ left: `${(x / MAP_WIDTH) * 100}%`, top: `${(y / MAP_HEIGHT) * 100}%` }}
          aria-label={`${item.sequence}번 ${item.name}`}
          aria-pressed={item.key === selectedKey}
          onClick={() => onSelect(item.key)}
        >
          {item.key === selectedKey && <img className="plan-map__ring" src={ringImage} alt="" />}
          {item.sequence}
        </button>
      ))}
      {points.length === 0 && <p className="plan-map__empty">좌표 정보가 있는 장소가 없습니다.</p>}
    </div>
  )
}

/** 카카오 지도 — 방문 순서 번호 마커와 순서대로 이은 점선, 전체 동선이 한 화면에 들어오게 맞춘다. */
function KakaoMap({ kakao, items, selectedKey, onSelect }) {
  const containerRef = useRef(null)
  const mapRef = useRef(null)
  const markersRef = useRef(new Map())
  const onSelectRef = useRef(onSelect)
  const fitRef = useRef(() => {})
  const located = useMemo(() => locatedItems(items), [items])

  useEffect(() => { onSelectRef.current = onSelect }, [onSelect])

  // 지도는 한 번만 만든다. 컨테이너 크기가 바뀌면 다시 맞춘다.
  useEffect(() => {
    const map = new kakao.maps.Map(containerRef.current, {
      center: new kakao.maps.LatLng(37.5665, 126.978),
      level: 6,
      scrollwheel: false, // 페이지 스크롤이 지도 확대로 바뀌지 않게 — 확대·축소는 버튼으로
    })
    map.addControl(new kakao.maps.ZoomControl(), kakao.maps.ControlPosition.RIGHT)
    mapRef.current = map

    const observer = new ResizeObserver(() => {
      map.relayout()
      fitRef.current()
    })
    observer.observe(containerRef.current)
    return () => {
      observer.disconnect()
      mapRef.current = null
    }
  }, [kakao])

  // 항목이 바뀌면 마커·선을 다시 그린다
  useEffect(() => {
    const map = mapRef.current
    if (!map) return undefined

    const path = located.map((item) => new kakao.maps.LatLng(item.lat, item.lng))
    const route = new kakao.maps.Polyline({
      path,
      strokeWeight: 4,
      strokeColor: ROUTE_COLOR,
      strokeOpacity: 0.85,
      strokeStyle: 'dash',
    })
    route.setMap(map)

    const overlays = located.map((item, index) => {
      const button = document.createElement('button')
      button.type = 'button'
      button.className = 'tp-marker plan-kakao-map__marker'
      button.textContent = String(item.sequence)
      button.setAttribute('aria-label', `${item.sequence}번 ${item.name}`)
      button.addEventListener('click', () => onSelectRef.current(item.key))
      const overlay = new kakao.maps.CustomOverlay({
        position: path[index],
        content: button,
        xAnchor: 0.5,
        yAnchor: 0.5,
        zIndex: 1,
      })
      overlay.setMap(map)
      markersRef.current.set(item.key, { button, overlay, position: path[index] })
      return overlay
    })

    fitRef.current = () => {
      if (path.length === 1) {
        map.setLevel(4)
        map.setCenter(path[0])
      } else if (path.length > 1) {
        const bounds = new kakao.maps.LatLngBounds()
        path.forEach((point) => bounds.extend(point))
        map.setBounds(bounds, BOUNDS_PADDING, BOUNDS_PADDING, BOUNDS_PADDING, BOUNDS_PADDING)
      }
    }
    fitRef.current()

    return () => {
      route.setMap(null)
      overlays.forEach((overlay) => overlay.setMap(null))
      markersRef.current.clear()
      fitRef.current = () => {}
    }
  }, [kakao, located])

  // 선택한 장소 강조 — 목록에서 고르면 지도도 그 장소로 옮긴다
  useEffect(() => {
    markersRef.current.forEach(({ button, overlay }, key) => {
      const selected = key === selectedKey
      button.classList.toggle('is-selected', selected)
      button.setAttribute('aria-pressed', String(selected))
      overlay.setZIndex(selected ? 3 : 1)
    })
    const selected = markersRef.current.get(selectedKey)
    const map = mapRef.current
    if (selected && map && !map.getBounds().contain(selected.position)) {
      map.panTo(selected.position)
    }
  }, [selectedKey, located])

  return (
    <div className="plan-map plan-kakao-map" role="group" aria-label="일정 순서 안내 지도">
      <div className="plan-kakao-map__canvas" ref={containerRef} />
      {located.length === 0 && <p className="plan-map__empty">좌표 정보가 있는 장소가 없습니다.</p>}
    </div>
  )
}

/**
 * 오늘의 이동 동선 지도. 카카오맵 키(VITE_KAKAO_MAP_KEY)가 있으면 실제 지도,
 * 없거나 불러오지 못하면 기존 개념도를 보여 준다. 안내 문구도 지도 종류에 맞춘다.
 */
export default function RouteMap({ items, selectedKey, onSelect }) {
  const [kakao, setKakao] = useState(() => (window.kakao?.maps?.LatLng ? window.kakao : null))
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    if (kakao) return undefined
    let ignore = false
    loadKakaoMaps()
      .then((loaded) => { if (!ignore) setKakao(loaded) })
      .catch(() => { if (!ignore) setFailed(true) })
    return () => { ignore = true }
  }, [kakao])

  const mode = kakao ? 'kakao' : 'concept'

  return (
    <>
      {kakao && <KakaoMap kakao={kakao} items={items} selectedKey={selectedKey} onSelect={onSelect} />}
      {!kakao && failed && <ConceptMap items={items} selectedKey={selectedKey} onSelect={onSelect} />}
      {!kakao && !failed && <div className="plan-map plan-map--loading" aria-busy="true" aria-label="지도를 불러오는 중" />}
      <p className="plan-route__note">{NOTES[mode]}</p>
    </>
  )
}
