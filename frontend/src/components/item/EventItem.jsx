import { useNavigate } from "react-router-dom";
import api from '../../services/api.js'
import '../../styles/myPage.css'

const EventItem = ({ event, onUpdate }) => {

    const moveUrl = useNavigate();

    const deleteEvent = async () => {

        await api.delete(`/api/favorites/events/${event.eventContentId}`)
            .then(response => {
                console.log(`debug >>>> event item delete response : `, response)
                if(response.status === 204){
                    onUpdate()
                }
            })
            .catch(error => {
                console.log(`debug >>>> event item delete error : `, error)
            })
    };

//   return (
//     <div>
        
//         <button     onClick = {()=>{
//                         //행사 검색 결과 페이지로 이동 (endPoint는 추후 페이지 추가에 따라 변경)
//                         //행사 검색 결과 페이지로 이동하며 이때 eventId를 전달한다.
//                         moveUrl(`/favorites/events/${event.eventId}`)
//                     }}>
//             문화행사 상세 조회
//         </button>

//         <button     onClick = {()=>{
//                         deleteEvent()
//                     }}>
//             관심행사 해제
//         </button>

//     </div>
//   );

  return (
    <article className="saved-card">
      <img
        className="saved-card-image"
        src={event.imageUrl || '/src/assets/mock/gallery.jpg'}
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
          <button className="delete-button" onClick={deleteEvent}>
            관심 행사 해제
          </button>
        </div>
      </div>
    </article>
  )
};

export default EventItem;