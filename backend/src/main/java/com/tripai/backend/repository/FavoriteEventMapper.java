package com.tripai.backend.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tripai.backend.domain.dto.EventSummary;

import java.util.List;

@Mapper 
public interface FavoriteEventMapper {
    List<EventSummary> findEventsByUserId(  @Param("userId") Long userId, 
                                            @Param("offset") Integer offset, 
                                            @Param("size") Integer size);
    Integer countEventsByUserId(@Param("userId") Long userId);
    Integer deleteEventByEventId(@Param("eventId") String eventId);
}
