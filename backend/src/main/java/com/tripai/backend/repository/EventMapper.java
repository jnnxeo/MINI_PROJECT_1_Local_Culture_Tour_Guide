package com.tripai.backend.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tripai.backend.domain.dto.event.EventDetailResponse;

@Mapper
public interface EventMapper {

    EventDetailResponse selectEventDetail(@Param("eventId") String eventId);


    EventDetailResponse selectEventForRecommendation(
        @Param("eventId") String eventId
    );
}
