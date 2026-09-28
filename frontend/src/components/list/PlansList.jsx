import { useNavigate } from "react-router-dom";

import PlanItem from "../item/PlanItem";
import '../../styles/myPage.css'

const PlansList = ({ary, onUpdate}) => {
    const moveUrl = useNavigate();
    
    if (ary.length === 0) {
        return (
            <div className="empty-state">
                <p className="empty-state-title">아직 저장한 일정이 없어요.</p>
                <p>마음에 드는 여행 코스를 만들어보세요.</p>
                <button className="primary-button" onClick={() => moveUrl(`/`)}>메인 페이지로 이동</button>
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