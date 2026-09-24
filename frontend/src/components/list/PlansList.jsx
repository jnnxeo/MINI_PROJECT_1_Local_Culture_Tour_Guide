import PlanItem from "../item/PlansItem";

const PlansList = ({ary, onUpdate}) => {
    if(ary.length === 0){
        return (
            <div>
                <p>저장한 일정이 없습니다.</p>
                <button onClick={() => {}}>
                    일정 만들기
                </button>
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