package com.tripai.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import com.tripai.backend.domain.dto.EventListResponse;
import com.tripai.backend.domain.dto.EventSummary;
import com.tripai.backend.repository.FavoriteEventMapper;
import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteEventService {

    private final FavoriteEventMapper favoriteEventMapper;

    public void addFavoriteEvent(Long userId, String eventContentId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        if (eventContentId == null || eventContentId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (!favoriteEventMapper.existsEvent(eventContentId)) {
            throw new CustomException(ErrorCode.EVENT_NOT_FOUND);
        }
        try {
            if (favoriteEventMapper.insertFavorite(userId, eventContentId) == 0) {
                throw new CustomException(ErrorCode.ALREADY_FAVORITED);
            }
        } catch (DuplicateKeyException exception) {
            // 동시에 저장한 요청도 DB의 복합 PK와 같은 409 응답으로 처리한다.
            throw new CustomException(ErrorCode.ALREADY_FAVORITED);
        }
    }

    public EventListResponse getFavoriteEvents(Long userId, Integer page, Integer size){

        int offset = page * size;

        List<EventSummary> events =
            favoriteEventMapper.findEventsByUserId(userId, offset, size);
        long totalCount = favoriteEventMapper.countEventsByUserId(userId);

        return new EventListResponse(events, page, totalCount);
    }

    public void deleteFavoriteEvent(Long userId, String eventContentId){
        Integer deleteRows = favoriteEventMapper.deleteEventByEventId(userId, eventContentId);
        if(deleteRows == 0){
            throw new CustomException(ErrorCode.FAVORITE_NOT_FOUND);
        }
    }
}
