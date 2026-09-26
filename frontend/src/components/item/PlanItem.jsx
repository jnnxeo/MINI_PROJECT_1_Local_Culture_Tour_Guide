import { useNavigate } from "react-router-dom";
import { useState } from "react";
import api from '../../services/api.js'
import '../../styles/myPage.css'

const PlanItem = ({ plan, onUpdate }) => {

    const moveUrl = useNavigate();
    const [isEditing, setIsEditing] = useState(false);
    const [title, setTitle] = useState(plan.title);

    const dDay = plan.dday
    const dDayText = dDay === 0 ? 'D-Day' : dDay > 0 ? `D-${dDay}` : `D+${Math.abs(dDay)}`

    const deletePlan = async () => {

        await api.delete(`/api/plans/drafts/${plan.planId}`)
            .then(response => {
                console.log(`debug >>>> plan item delete planId : `, plan.planId)
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

        await api.patch(`/api/plans/drafts/${plan.planId}/title`, {title : trimmedTitle})
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

    return (
        <article className="saved-card">

        <div className="saved-card-content">
            <p className="saved-card-date">
            {plan.visitDate} · {dDayText}
            </p>

            {isEditing ? (
            <div className="rename-area">
                <input
                value={title}
                onChange={(event) => setTitle(event.target.value)}
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
                <h3>{plan.title}</h3>

                <div className="saved-card-actions">
                <button
                    onClick={() =>
                        moveUrl(`/plans/drafts/${plan.planId}/conditions`)
                    }
                >
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

export default PlanItem;