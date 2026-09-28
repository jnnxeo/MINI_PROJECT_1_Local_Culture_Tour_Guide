package com.tripai.backend.repository;

import java.time.LocalTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tripai.backend.domain.dto.plan.EventPlaceCandidateRow;
import com.tripai.backend.domain.dto.plan.EventPlanItemRow;
import com.tripai.backend.domain.dto.plan.EventPlanRow;

@Mapper
public interface EventPlanMapper {
    EventPlanRow findPlan(@Param("planId") long planId);

    EventPlanRow findSavedPlanForUpdate(@Param("planId") long planId, @Param("userId") long userId);

    List<EventPlanItemRow> findItems(@Param("planId") long planId);

    List<String> findInterests(@Param("planId") long planId);

    EventPlaceCandidateRow findNearbyRestaurant(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("mealStart") LocalTime mealStart,
            @Param("mealEnd") LocalTime mealEnd
    );

    int deleteExistingDraft(@Param("userId") long userId);

    int insertPlan(EventPlanRow plan);

    int insertInterest(@Param("planId") long planId, @Param("interest") String interest);

    int insertItem(
            @Param("planId") long planId,
            @Param("seq") int seq,
            @Param("type") String type,
            @Param("contentId") String contentId,
            @Param("startTime") LocalTime startTime,
            @Param("durationMin") int durationMin,
            @Param("aiReason") String aiReason,
            @Param("timeFixed") boolean timeFixed
    );

    int updateItemStartTime(@Param("planId") long planId, @Param("itemId") long itemId,
                            @Param("startTime") LocalTime startTime);

    int offsetItemSeq(@Param("planId") long planId, @Param("offset") int offset);

    int updateItemSeq(@Param("planId") long planId, @Param("itemId") long itemId,
                      @Param("seq") int seq);
}
