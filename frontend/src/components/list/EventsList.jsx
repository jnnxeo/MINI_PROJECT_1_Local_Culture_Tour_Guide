import EventItem from "../item/EventItem";

const EventsList = ({ ary, onUpdate}) => {
    if(ary.length === 0){
        return (
            <div>
                <p>관심있는 행사가 없습니다.</p>
                <button onClick={() => {}}>
                    행사 추가하기
                </button>
            </div>
        )
    }
  
    return (
    <div>
      {ary.map((event) => (
        <EventItem
            key = {event.eventId}
            event={event}
            onUpdate={onUpdate}
        />
      ))}
    </div>
  );
};

export default EventsList;