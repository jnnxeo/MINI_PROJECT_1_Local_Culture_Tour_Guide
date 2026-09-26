package com.tripai.backend.repository;

import com.tripai.backend.external.tour.TourRestaurantRecord;
import org.apache.ibatis.annotations.Mapper;

/** TourAPI 수집 전용 저장소. 일정 화면의 PlaceMapper와 역할을 분리한다. */
@Mapper
public interface TourRestaurantMapper {

    void upsertRestaurant(TourRestaurantRecord restaurant);
}
