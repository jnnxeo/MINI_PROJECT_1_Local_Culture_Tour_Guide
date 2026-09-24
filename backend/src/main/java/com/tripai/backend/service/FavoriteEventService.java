package com.tripai.backend.service;

import org.springframework.stereotype.Service;

import com.tripai.backend.domain.dto.EventListResponse;
import com.tripai.backend.domain.dto.EventSummary;
import com.tripai.backend.repository.FavoriteEventMapper;

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

        Integer totalCount = favoriteEventMapper.countEventsByUserId(userId);

        return new EventListResponse(events, page, totalCount);
    }

    public Integer deleteFavoriteEvent(String eventId){
        Integer response = favoriteEventMapper.deleteEventByEventId(eventId);
        
        return response;
    }

}
