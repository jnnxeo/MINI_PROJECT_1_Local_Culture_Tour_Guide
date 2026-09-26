import { useNavigate } from "react-router-dom";

import EventItem from "../item/EventItem";
import '../../styles/myPage.css'

const EventsList = ({ ary, onUpdate}) => {
    const moveUrl = useNavigate();

    if (ary.length === 0) {
        return (
            <div className="empty-state">
                <p className="empty-state-title">아직 관심 있는 행사가 없어요.</p>
                <p>마음에 드는 행사를 저장해보세요.</p>
                <button className="primary-button" onClick={() => moveUrl(`/events/${event.eventId}`)}>행사 둘러보기</button>
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