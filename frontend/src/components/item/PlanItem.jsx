import { useNavigate } from "react-router-dom";
import { useState } from "react";
import api from '../../services/api.js'
import galleryFallback from '../../assets/mock/gallery.jpg'
import '../../styles/myPage.css'

const PlanItem = ({ plan, onUpdate }) => {

    const moveUrl = useNavigate();
    const [isEditing, setIsEditing] = useState(false);
    const [title, setTitle] = useState(plan.title);

    const dDay = plan.dDay
    const dDayText = dDay === 0 ? 'D-Day' : dDay > 0 ? `D-${dDay}` : `D+${Math.abs(dDay)}`

    const deletePlan = async () => {
        // 삭제하면 되돌릴 수 없어 한 번 더 확인한다
        if (!window.confirm(`'${plan.title}' 일정을 삭제할까요? 삭제한 일정은 되돌릴 수 없습니다.`)) return

        await api.delete(`/api/plans/${plan.tripPlanId}`)
            .then(response => {
                console.log(`debug >>>> plan item delete planId : `, plan.tripPlanId)
                if(response.status === 204){onUpdate();}
            })
            .catch(error => {
                console.log(`debug >>>> plan item delete error : `, error)
            })
    }

    const updatePlanName = async () => {
        const trimmedTitle = title.trim()

        if (!trimmedTitle) {
            alert('일정 이름을 입력해 주세요.')
            return
        }

        await api.patch(`/api/plans/${plan.tripPlanId}`, {title : trimmedTitle})
            .then(response => {
                if(response.status === 200){
                    setIsEditing(false)
                    onUpdate()
                }
            })
            .catch(error => {
                console.log('debug >>>> error plan item update plan name : ', error)
            })
    }

//   return (
//     <div>
        
//         <button     title = "일정 상세"
//                     onClick = {()=>{
//                         //나의 일정 페이지로 이동 (endPoint는 추후 페이지 추가에 따라 변경)
//                         //나의 일정 페이지로 이동하며 이대 planId를 전달한다.
//                         moveUrl(`/plans/drafts/${plan.planId}/conditions`)
//                     }}>일정 상세</button>

//         <button     title = "일정 삭제"
//                     onClick = {()=>{
//                         deletePlan()
//                     }}>일정 삭제</button>

//         {isEditing ? (
//                 <>
//                 <input    value={title}
//                             onChange={(e) => setTitle(e.target.value)}/>
                
//                 <button   onClick={updatePlanName}>저장</button>
//                 <button   onClick={() => {      setTitle(plan.title);
//                                                 setIsEditing(false);}}>
//                     취소
//                 </button>
//                 </>
//             ) : (
//                 <>
//                 <span>{plan.title}</span>
//                 <button onClick={() => setIsEditing(true)}>이름 변경</button>
//                 </>
//         )}



//     </div>
//   )

    const isPast = dDay < 0
    const badgeText = dDay === 0 ? '오늘' : isPast ? '지난 일정' : dDayText
    const course = plan.course ?? []
    const place = [plan.district, plan.eventPlace].filter(Boolean).join(' · ')
    const openPlan = () => moveUrl(`/my-trips/${plan.tripPlanId}`)

    return (
        <article className={`saved-card plan-card${isPast ? ' plan-card--past' : ''}`}>
        {/* 대표 행사 이미지 — 누르면 저장한 일정 상세 (API-PLAN-011) */}
        <button className="plan-card__visual" type="button" onClick={openPlan} aria-label={`${plan.title} 일정 보기`}>
            <img
                className="saved-card-image plan-card__image"
                src={plan.eventImageUrl || galleryFallback}
                alt=""
                loading="lazy"
                onError={(event) => { event.currentTarget.src = galleryFallback }}
            />
            <span className={`plan-card__badge${dDay === 0 ? ' plan-card__badge--today' : ''}`}>{badgeText}</span>
        </button>

        <div className="saved-card-content plan-card__content">
            <p className="saved-card-date plan-card__meta">
                <span>{formatTripDate(plan.tripDate)}</span>
                {plan.startTime && plan.endTime && <span>{plan.startTime} – {plan.endTime}</span>}
                {plan.eventCategory && <span className="plan-card__category">{plan.eventCategory}</span>}
            </p>

            {isEditing ? (
            <div className="rename-area">
                <input
                value={title}
                aria-label="일정 이름"
                maxLength={100}
                onChange={(event) => setTitle(event.target.value)}
                onKeyDown={(event) => { if (event.key === 'Enter') updatePlanName() }}
                autoFocus
                />
                <button onClick={updatePlanName}>저장</button>
                <button
                onClick={() => {
                    setTitle(plan.title)
                    setIsEditing(false)
                }}
                >
                취소
                </button>
            </div>
            ) : (
            <>
                <h3 className="plan-card__title">
                    <button type="button" onClick={openPlan}>{plan.title}</button>
                </h3>

                {plan.eventName && (
                <p className="plan-card__event">
                    <span className="plan-card__event-name">{plan.eventName}</span>
                    {place && <span className="plan-card__place">{place}</span>}
                </p>
                )}

                {course.length > 0 && (
                <ol className="plan-card__course" aria-label={`방문 코스 ${course.length}곳`}>
                    {course.map((name, index) => (
                    <li key={`${index}-${name}`}>
                        <span className="plan-card__step">{index + 1}</span>
                        <span className="plan-card__course-name">{name}</span>
                    </li>
                    ))}
                </ol>
                )}

                <div className="saved-card-actions">
                <button onClick={openPlan}>
                    일정 보기
                </button>
                <button onClick={() => setIsEditing(true)}>이름 변경</button>
                <button className="delete-button" onClick={deletePlan}>
                    일정 삭제
                </button>
                </div>
            </>
            )}
        </div>
        </article>
    )
};

// "2026-09-30" → "2026.09.30 (수)" — 날짜 문자열 그대로 읽어 기기 시간대와 상관없이 같은 요일
function formatTripDate(tripDate) {
    if (!tripDate) return ''
    const [year, month, day] = tripDate.split('-').map(Number)
    const weekday = '일월화수목금토'[new Date(Date.UTC(year, month - 1, day)).getUTCDay()]
    return `${tripDate.replaceAll('-', '.')} (${weekday})`
}

export default PlanItem;