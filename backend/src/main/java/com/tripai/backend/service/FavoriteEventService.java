package com.tripai.backend.service;

import org.springframework.stereotype.Service;

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

    public EventListResponse getFavoriteEvents(Long userId, Integer page, Integer size){
        
        int offset = page * size;

        List<EventSummary> events =
            favoriteEventMapper.findEventsByUserId(userId, offset, size);
        System.out.println("debug >>>> favorite event service get list");

        long totalCount = favoriteEventMapper.countEventsByUserId(userId);

        System.out.println("debug >>>> favorite event service get totalCount");

        return new EventListResponse(events, page, totalCount);
    }

    public void deleteFavoriteEvent(Long userId, String eventId){
        Integer deleteRows = favoriteEventMapper.deleteEventByEventId(userId, eventId);
        if(deleteRows == 0){
            throw new CustomException(ErrorCode.EVENT_NOT_FOUND);
        }

    }

}
