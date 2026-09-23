import React, { useEffect, useState } from "react";
import { getFavorite } from "../services/favoriteApi";
import FavoriteButton from "./FavoriteButton";

function FavoriteList() {

    const [favorite,SetFavorite] = useState([]);

    useEffect(()=> {
        getFavorite()
            .then((respose) => {
                SetFavorite(respose.data); // 받은 목록 상태 저장
            })
            .catch((error) => {
                console.error("목록 불러오기 실패",error);
            });
    },[]);//화면 열릴때 한번 호출

    const handleRemoved = (removeId) => {
       SetFavorite(favorite.filter((event) => event.eventContentId !== removeId));
    }


    return(
        <div>
            {favorite.map((event)=>(
                <div key={event.eventContentId}>
                    {event.eventName} - {event.eventStartDate}
                    <FavoriteButton 
                        eventContentId={event.eventContentId} 
                        initialFavorite={true}   
                        onRemoved={handleRemoved}   
                    />
                </div>
            ))}
        </div>
    )
}

export default FavoriteList;