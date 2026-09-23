package com.tripai.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tripai.backend.domain.dto.favorite.FavoriteEventResponse;
// import com.tripai.backend.global.exception.CustomException;
// import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.repository.FavoriteEventMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class FavoriteEventService {
    
    private final FavoriteEventMapper favoriteEventMapper;

    public void addFavorite(Long userId, String eventContentId) {

        boolean exists = favoriteEventMapper.existsFavorite(userId, eventContentId);

   

        if (exists) {
        // throw new CustomException(ErrorCode.ALREADY_FAVORITED);  // TODO: 예외처리 머지 후 복원
        throw new RuntimeException("이미 저장된 행사입니다.");
    }
        favoriteEventMapper.insertFavorite(userId, eventContentId);
    }


    public void deleteFavorite(Long userId, String eventContentId) {

        boolean exists = favoriteEventMapper.existsFavorite(userId, eventContentId);

         // deleteFavorite
        if (!exists) {
            // throw new CustomException(ErrorCode.FAVORITE_NOT_FOUND);  // TODO: 예외처리 머지 후 복원
            throw new RuntimeException("없는 행사입니다.");
        }

            favoriteEventMapper.deleteFavorite(userId, eventContentId) ;  
        }

    public List<FavoriteEventResponse> getFavorite(Long userId) {
        return favoriteEventMapper.selectFavorite(userId);
    }
}
