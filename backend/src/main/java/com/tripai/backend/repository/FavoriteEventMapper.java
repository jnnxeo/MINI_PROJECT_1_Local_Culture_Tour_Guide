package com.tripai.backend.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tripai.backend.domain.dto.EventSummary;

import java.util.List;

@Mapper 
public interface FavoriteEventMapper {
    public List<EventSummary> findEventsByUserId(  @Param("userId") Long userId, 
                                            @Param("offset") Integer offset, 
                                            @Param("size") Integer size);
    public long countEventsByUserId(@Param("userId") Long userId);
    public Integer deleteEventByEventId(   @Param("userId") Long userId,
                                    @Param("eventContentId") String eventContentId);
}
