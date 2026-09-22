import React from "react";
import { useState } from "react";
import { addFavorite, removeFavorite } from "../services/favoriteApi";

function FavoriteButton({eventContentId}) {
    const [isFavorite,setIsFavorite] = useState(false);

    const favoritehandler = async () => {
    // ===== 로직 (유지) =====
    try {
        if (isFavorite) {
            await removeFavorite(eventContentId);   // 해제 성공을 기다림
        } else {
            await addFavorite(eventContentId);      // 저장 성공을 기다림
        }
        setIsFavorite(!isFavorite);   // 성공했을 때만 화면 바꿈
    } catch (error) {
        // TODO(연동): 실패 처리 (401이면 로그인 유도 등)
        console.error("즐겨찾기 처리 실패:", error);
    }
    // =====================
};




    return (
        // TODO(css): 아래 button 마크업·하트 문자는 CSS 담당이 아이콘/스타일로 교체 예정
        <button onClick={favoritehandler}>
            {isFavorite?  "♥" : "♡"} {/* TODO(css): ♥♡ → 실제 하트 아이콘(SVG)로 교체 */}
        </button>
    );
}

export default FavoriteButton;