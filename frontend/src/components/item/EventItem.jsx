import { useNavigate } from "react-router-dom";
import { useState } from "react";
import api from '../../services/api.js'

const EventItem = ({ event, onUpdate }) => {

    const moveUrl = useNavigate();

    const deleteEvent = async () => {

        await api.delete(`/api/favorites/events/${event.eventId}`)
            .then(response => {
                console.log(`debug >>>> event item delete response : `, response)
                if(response.status === 200){
                    onUpdate()
                }
            })
            .catch(error => {
                console.log(`debug >>>> event item delete error : `, error)
            })
    };

  return (
    <div>
        
        <button     onClick = {()=>{
                        //행사 검색 결과 페이지로 이동 (endPoint는 추후 페이지 추가에 따라 변경)
                        //행사 검색 결과 페이지로 이동하며 이때 eventId를 전달한다.
                        moveUrl(`/favorites/events/${event.eventId}`)
                    }}>
            문화행사 상세 조회
        </button>

        <button     onClick = {()=>{
                        deleteEvent()
                    }}>
            관심행사 해제
        </button>

    </div>
  );
};

export default EventItem;