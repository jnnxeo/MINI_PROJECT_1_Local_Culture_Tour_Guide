import React, { useMemo } from 'react'
import mapImage from '../../assets/plan/route-concept-map.svg'
import ringImage from '../../assets/plan/marker-ring.svg'

// Figma 개념도 크기 (642 × 470)
const MAP_WIDTH = 642
const MAP_HEIGHT = 470
const PADDING = 60

/**
 * 방문 순서 개념도 — 좌표를 개념도 안에 비율로 배치한다.
 * 실제 지도(카카오맵)는 별도 이슈에서 이 컴포넌트를 교체한다.
 */
function toPoints(items) {
  const located = items.filter((item) => item.lat != null && item.lng != null)

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

export default function RouteMap({ items, selectedKey, onSelect }) {
  const points = useMemo(() => toPoints(items), [items])

  return (
    <div className="plan-map" role="group" aria-label="일정 순서 안내 지도">
      <img className="plan-map__background" src={mapImage} alt="" />
      <svg className="plan-map__route" viewBox={`0 0 ${MAP_WIDTH} ${MAP_HEIGHT}`} preserveAspectRatio="none" aria-hidden="true">
        <polyline
          points={points.map(({ x, y }) => `${x},${y}`).join(' ')}
          fill="none"
          stroke="#2563eb"
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
          aria-label={`${item.seq}번 ${item.name}`}
          aria-pressed={item.key === selectedKey}
          onClick={() => onSelect(item.key)}
        >
          {item.key === selectedKey && <img className="plan-map__ring" src={ringImage} alt="" />}
          {item.seq}
        </button>
      ))}
      {points.length === 0 && <p className="plan-map__empty">좌표 정보가 있는 장소가 없습니다.</p>}
    </div>
  )
}
