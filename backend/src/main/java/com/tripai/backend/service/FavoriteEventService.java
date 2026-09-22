package com.tripai.backend.service;

import org.springframework.stereotype.Service;

import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;
import com.tripai.backend.repository.FavoriteEventMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class FavoriteEventService {
    
    private final FavoriteEventMapper favoriteEventMapper;

    public void addFavorite(Long userId, String eventContentId) {

        boolean exists = favoriteEventMapper.existsFavorite(userId, eventContentId);

   

    if (exists) {
        throw new CustomException(ErrorCode.ALREADY_FAVORITED);
        }

        favoriteEventMapper.insertFavorite(userId, eventContentId);
    }


    public void deleteFavorite(Long userId, String eventContentId) {

        boolean exists = favoriteEventMapper.existsFavorite(userId, eventContentId);

    if (!exists) { 
        throw new CustomException(ErrorCode.FAVORITE_NOT_FOUND);
    }

    favoriteEventMapper.deleteFavorite(userId, eventContentId) ;  
    }
}
