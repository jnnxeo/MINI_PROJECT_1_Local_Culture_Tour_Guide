import { useNavigate } from "react-router-dom";
import { useState } from "react";
import api from '../../services/api.js'
import galleryFallback from '../../assets/mock/gallery.jpg'
import '../../styles/myPage.css'

const EventItem = ({ event, onUpdate }) => {

    const moveUrl = useNavigate();
    const [removing, setRemoving] = useState(false);
    const [error, setError] = useState('');

    const deleteEvent = async () => {
        if (removing) return;
        setRemoving(true);
        setError('');
        try {
            await api.delete(`/api/favorites/events/${encodeURIComponent(event.eventContentId)}`);
            await onUpdate();
        } catch {
            setError('관심 행사를 해제하지 못했습니다. 다시 시도해 주세요.');
        } finally {
            setRemoving(false);
        }
    };

  return (
    <article className="saved-card">
      <img
        className="saved-card-image"
        src={event.imageUrl || galleryFallback}
        alt={event.title}
      />

      <div className="saved-card-content">
        <p className="saved-card-date">
          {event.eventStartDate} ~ {event.eventEndDate}
        </p>

        <h3>{event.title}</h3>
        <p className="saved-card-address">{event.addr}</p>

        <div className="saved-card-actions">
          <button onClick={() => moveUrl(`/events/${event.eventContentId}`)}>
            상세 보기
          </button>
          <button className="delete-button" onClick={deleteEvent} disabled={removing}>
            {removing ? '해제 중…' : '관심 행사 해제'}
          </button>
        </div>
        {error && <p role="alert">{error}</p>}
      </div>
    </article>
  )
};

export default EventItem;
