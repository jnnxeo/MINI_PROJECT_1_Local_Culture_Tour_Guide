import { useNavigate } from "react-router-dom";
import { useState } from "react";
import axios from "axios";

const PlanItem = ({ plan, onUpdate }) => {

    const moveUrl = useNavigate();

    const [isEditing, setIsEditing] = useState(false);
    const [title, setTitle] = useState(plan.title);
    const at = localStorage.getItem("at");

    const deletePlan = async () => {

        try {
            await axios.delete(
                `http://localhost:8080/api/plans/drafts/${plan.planId}`,
                {headers : { Authorization : at ? at : ""}});
            
            onUpdate()
        } 
        catch (error) {
            console.error("일정 삭제 실패:", error);
        }
    };

    const updatePlanName = async () => {
        try {
            await axios.patch(
                `http://localhost:8080/api/plans/drafts/${plan.planId}/title`,
                    {title : title},
                    {headers : { Authorization : at ? at : ""}});

            setIsEditing(false);
            onUpdate(); // List → MyPage가 가진 목록 조회 함수를 실행
        } 
        catch (error) {
            console.error("일정 이름 변경 실패:", error);
        }
    }

  return (
    <div>
        
        <button     title = "일정 상세"
                    onClick = {()=>{
                        moveUrl(`/plans/drafts/${plan.planId}/conditions`)
                    }}/>

        <button     title = "일정 삭제"
                    onClick = {()=>{
                        deletePlan()
                    }}/>

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