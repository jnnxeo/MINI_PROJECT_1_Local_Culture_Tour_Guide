import { useCallback, useEffect, useRef, useState } from 'react'
import { addFavorite, getFavorite, removeFavorite } from '../services/favoriteApi.js'

// 관심 행사 목록 API 는 한 번에 최대 20개라서, 하트 표시에 쓸 ID 를 페이지를 넘기며 모은다
const PAGE_SIZE = 20
const MAX_PAGES = 25

async function loadFavoriteIds() {
  const ids = new Set()
  for (let page = 0; page < MAX_PAGES; page += 1) {
    const response = await getFavorite({ page, size: PAGE_SIZE })
    const data = response.data?.data
    const events = data?.events ?? []
    events.forEach((event) => ids.add(event.eventContentId))
    if (events.length < PAGE_SIZE || (data?.totalCount != null && ids.size >= data.totalCount)) break
  }
  return ids
}

/**
 * 메인·검색 결과 카드의 하트 — 서버 관심 행사(API-FAV)와 연결한다.
 * 처음에 내 관심 행사 ID 를 불러와 하트를 채우고, 누르면 저장·해제 API 를 부른 뒤 결과대로 바꾼다.
 * 요청이 실패하면 하트를 원래대로 되돌리고 오류 문구를 알려 준다.
 */
export default function useFavoriteEvents() {
  const [favoriteIds, setFavoriteIds] = useState(() => new Set())
  const [pendingIds, setPendingIds] = useState(() => new Set())
  const [error, setError] = useState('')
  const mounted = useRef(true)

  useEffect(() => {
    mounted.current = true
    loadFavoriteIds()
      .then((ids) => { if (mounted.current) setFavoriteIds(ids) })
      .catch(() => { if (mounted.current) setError('관심 행사를 불러오지 못했어요. 하트 표시가 정확하지 않을 수 있어요.') })
    return () => { mounted.current = false }
  }, [])

  const toggleFavorite = useCallback(async (eventId) => {
    if (!eventId || pendingIds.has(eventId)) return
    const wasFavorite = favoriteIds.has(eventId)
    const update = (favorite) => setFavoriteIds((current) => {
      const next = new Set(current)
      favorite ? next.add(eventId) : next.delete(eventId)
      return next
    })

    setError('')
    update(!wasFavorite)
    setPendingIds((current) => new Set(current).add(eventId))
    try {
      if (wasFavorite) {
        await removeFavorite(eventId)
      } else {
        await addFavorite(eventId)
      }
    } catch (requestError) {
      // 이미 저장됨(409)·이미 해제됨(404)은 서버 상태가 목표와 같으니 그대로 둔다
      const status = requestError.response?.status
      const alreadyDone = (!wasFavorite && status === 409) || (wasFavorite && status === 404)
      if (!alreadyDone && mounted.current) {
        update(wasFavorite)
        setError(requestError.response?.data?.message || '관심 행사를 저장하지 못했어요. 잠시 후 다시 시도해 주세요.')
      }
    } finally {
      if (mounted.current) {
        setPendingIds((current) => {
          const next = new Set(current)
          next.delete(eventId)
          return next
        })
      }
    }
  }, [favoriteIds, pendingIds])

  return { favoriteIds, pendingIds, toggleFavorite, favoriteError: error }
}
