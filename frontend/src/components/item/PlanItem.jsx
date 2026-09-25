import { useNavigate } from "react-router-dom";
import { useState } from "react";
import api from '../../services/api.js'

const PlanItem = ({ plan, onUpdate }) => {

    const moveUrl = useNavigate();

    const [isEditing, setIsEditing] = useState(false);
    const [title, setTitle] = useState(plan.title);

    const deletePlan = async () => {

        await api.delete(`/api/plans/drafts/${plan.planId}`)
            .then(response => {
                console.log(`debug >>>> plan item delete planId : `, plan.planId)
                if(response.status === 204){
                    onUpdate();
                }
            })
            .catch(error => {
                console.log(`debug >>>> plan item delete error : `, error)
            })
    }

    const updatePlanName = async () => {
        await api.patch(`/api/plans/drafts/${plan.planId}/title`, {title : title})
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

  return (
    <div>
        
        <button     title = "일정 상세"
                    onClick = {()=>{
                        //나의 일정 페이지로 이동 (endPoint는 추후 페이지 추가에 따라 변경)
                        //나의 일정 페이지로 이동하며 이대 planId를 전달한다.
                        moveUrl(`/plans/drafts/${plan.planId}/conditions`)
                    }}>일정 상세</button>

        <button     title = "일정 삭제"
                    onClick = {()=>{
                        deletePlan()
                    }}>일정 삭제</button>

        {isEditing ? (
                <>
                <input    value={title}
                            onChange={(e) => setTitle(e.target.value)}/>
                
                <button   onClick={updatePlanName}>저장</button>
                <button   onClick={() => {      setTitle(plan.title);
                                                setIsEditing(false);}}>
                    취소
                </button>
                </>
            ) : (
                <>
                <span>{plan.title}</span>
                <button onClick={() => setIsEditing(true)}>이름 변경</button>
                </>
        )}



    </div>
  );
};

export default PlanItem;