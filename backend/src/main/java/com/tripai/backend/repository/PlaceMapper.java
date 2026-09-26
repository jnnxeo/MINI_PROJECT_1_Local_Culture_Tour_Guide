package com.tripai.backend.repository;

import com.tripai.backend.domain.entity.EventLocationView;
import com.tripai.backend.domain.entity.RestaurantView;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlaceMapper {

	Optional<EventLocationView> findEventLocation(String eventContentId);

	/**
	 * 기준 좌표에서 radius(m) 안의 표출 중인 음식점을 가까운 순으로 가져온다.
	 * minLat~maxLng 는 IX_PLACE_COORD 를 타도록 먼저 거르는 사각형 범위.
	 * cuisineType/mealTime 은 둘 다 선택값이다 (null 이면 필터링 안 함, 기존 호출과 동일하게 동작).
	 */
	List<RestaurantView> findRestaurantsNear(
			@Param("lat") double lat,
			@Param("lng") double lng,
			@Param("radius") int radius,
			@Param("minLat") double minLat,
			@Param("maxLat") double maxLat,
			@Param("minLng") double minLng,
			@Param("maxLng") double maxLng,
			@Param("cuisineType") String cuisineType,
			@Param("mealTime") LocalTime mealTime,
			@Param("limit") int limit,
			@Param("offset") int offset
	);
}
