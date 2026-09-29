import React from 'react'
import { useParams } from 'react-router-dom'
import PlanEditorPage from './PlanEditorPage.jsx'

/** 저장 일정도 전체 일정 편집 화면에서 확인하고 수정한다. */
export default function EventSavedPlanPage() {
  const { planId } = useParams()
  return <PlanEditorPage savedPlanId={planId} />
}
