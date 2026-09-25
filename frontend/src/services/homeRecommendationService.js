import { getDraft } from './planService.js'
import { MOCK_DRAFT_ID } from './mock/planMock.js'

const USE_PLAN_MOCK_API = import.meta.env.VITE_USE_PLAN_MOCK_API
  ? import.meta.env.VITE_USE_PLAN_MOCK_API !== 'false'
  : import.meta.env.VITE_USE_MOCK_API !== 'false'

// 홈에는 방문 날짜와 행사 ID가 없어 API-PLAN-001을 아직 호출할 수 없다.
// 목업에서는 PR #26의 예시 초안을 열고, 실제 모드에서는 계약 확정 전 이동을 막는다.
export async function startHomeRecommendation() {
  if (!USE_PLAN_MOCK_API) {
    throw new Error('AI 일정 생성 조건이 확정되면 이용할 수 있습니다.')
  }

  const draft = await getDraft(MOCK_DRAFT_ID)
  return { draftId: draft.draftId }
}
