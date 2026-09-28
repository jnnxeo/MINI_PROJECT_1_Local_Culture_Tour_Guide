import { useNavigate } from "react-router-dom";

import EventItem from "../item/EventItem";
import '../../styles/myPage.css'

const EventsList = ({ ary, onUpdate}) => {
    const moveUrl = useNavigate();

    if (ary.length === 0) {
        return (
            <div className="empty-state">
                <p className="empty-state-title">관심 행사가 없습니다.</p>
                <p>마음에 드는 행사에 하트를 눌러 저장해 보세요.</p>
                <button className="primary-button" onClick={() => moveUrl('/', { state: { guide: 'favorite' } })}>메인 페이지로 이동</button>
            </div>
        )
    }
  
    return (
    <div>
      {ary.map((event) => (
        <EventItem
            key = {event.eventContentId}
            event={event}
            onUpdate={onUpdate}
        />
      ))}
    </div>
  );
};

export default EventsList;