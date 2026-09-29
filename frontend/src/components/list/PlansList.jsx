import { useNavigate } from "react-router-dom";

import PlanItem from "../item/PlanItem";
import '../../styles/myPage.css'

const PlansList = ({ary, onUpdate}) => {
    const moveUrl = useNavigate();
    
    if (ary.length === 0) {
        return (
            <div className="empty-state">
                <p className="empty-state-title">저장한 일정이 없습니다.</p>
                <p>행사를 골라 일정을 만들고 저장해 보세요.</p>
                <button className="primary-button" onClick={() => moveUrl('/', { state: { guide: 'plan' } })}>메인 페이지로 이동</button>
            </div>
        )
    }
  
    return (
    <div>
      {ary.map((plan) => (
        <PlanItem
            key = {plan.tripPlanId}
            plan={plan}
            onUpdate={onUpdate}
        />
      ))}
    </div>
  );
};

export default PlansList;