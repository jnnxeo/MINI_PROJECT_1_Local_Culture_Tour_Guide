import PlanItem from "../item/PlanItem";
import '../../styles/myPage.css'

const PlansList = ({ary, onUpdate}) => {
    if (ary.length === 0) {
        return (
            <div className="empty-state">
            <p className="empty-state-title">아직 저장한 일정이 없어요.</p>
            <p>마음에 드는 여행 코스를 만들어보세요.</p>
            <button className="primary-button">일정 만들기</button>
            </div>
        )
    }
  
    return (
    <div>
      {ary.map((plan) => (
        <PlanItem
            key = {plan.planId}
            plan={plan}
            onUpdate={onUpdate}
        />
      ))}
    </div>
  );
};

export default PlansList;