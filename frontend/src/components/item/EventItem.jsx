import { useNavigate } from "react-router-dom";
import { useState } from "react";
import axios from "axios";

const EventItem = ({ event, onUpdate }) => {

    const moveUrl = useNavigate();
    const at = localStorage.getItem("at");

    const deleteEvent = async () => {

        try {
            await axios.delete(
                `http://localhost:8080/api/favorites/events/${event.eventId}`,
                {headers : { Authorization : at ? at : ""}});
            onUpdate()
        } 
        catch (error) {
            console.error("일정 삭제 실패:", error);
        }
    };

  return (
    <div>
        
        <button     onClick = {()=>{
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

export default PlanItem;