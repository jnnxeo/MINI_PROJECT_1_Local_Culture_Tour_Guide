// 카카오맵 JavaScript SDK 를 한 번만 불러온다.
// 키는 frontend/.env 의 VITE_KAKAO_MAP_KEY (카카오 개발자 콘솔 JavaScript 키, 사이트 도메인 등록 필요)
const SDK_URL = 'https://dapi.kakao.com/v2/maps/sdk.js'
const LOAD_TIMEOUT_MS = 8000

let loading = null

export function hasKakaoMapKey() {
  return Boolean(import.meta.env.VITE_KAKAO_MAP_KEY)
}

/** window.kakao 를 돌려준다. 키가 없거나 불러오지 못하면 reject — 화면은 개념도로 대신 보여 준다. */
export function loadKakaoMaps() {
  if (window.kakao?.maps?.LatLng) return Promise.resolve(window.kakao)
  if (!hasKakaoMapKey()) return Promise.reject(new Error('카카오맵 키가 없습니다.'))
  if (loading) return loading

  loading = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    const fail = (message) => {
      clearTimeout(timer)
      script.remove()
      loading = null
      reject(new Error(message))
    }
    const timer = setTimeout(() => fail('카카오맵을 불러오는 시간이 초과되었습니다.'), LOAD_TIMEOUT_MS)

    script.src = `${SDK_URL}?appkey=${encodeURIComponent(import.meta.env.VITE_KAKAO_MAP_KEY)}&autoload=false`
    script.async = true
    // 도메인 미등록·키 오류면 SDK 요청이 401 이라 onerror 로 온다
    script.onerror = () => fail('카카오맵을 불러오지 못했습니다. 키와 사이트 도메인 등록을 확인해 주세요.')
    script.onload = () => {
      if (!window.kakao?.maps?.load) {
        fail('카카오맵 SDK 형식이 올바르지 않습니다.')
        return
      }
      window.kakao.maps.load(() => {
        clearTimeout(timer)
        resolve(window.kakao)
      })
    }
    document.head.appendChild(script)
  })
  return loading
}
