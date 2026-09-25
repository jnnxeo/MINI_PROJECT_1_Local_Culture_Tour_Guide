package com.tripai.backend.repository;

import com.tripai.backend.domain.entity.ItemTargetView;
import com.tripai.backend.domain.entity.PlanItemView;
import com.tripai.backend.domain.entity.TripItem;
import com.tripai.backend.domain.entity.TripPlan;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlanDraftMapper {

	Optional<TripPlan> findPlanById(Long tripPlanId);

	/** 수정·저장 전에 초안 행을 잠근다. 동시 요청은 앞 요청이 끝난 뒤 최신 값으로 진행된다 */
	Optional<TripPlan> findPlanByIdForUpdate(Long tripPlanId);

	List<PlanItemView> findItemsByPlanId(Long tripPlanId);

	int updateTitle(@Param("tripPlanId") Long tripPlanId, @Param("title") String title);

	Optional<ItemTargetView> findPlaceTarget(String contentId);

	Optional<ItemTargetView> findEventTarget(String eventContentId);

	/** seq_order UNIQUE 충돌을 피하려고 순번을 잠깐 뒤로 미룬다 (CHECK seq_order > 0 이라 음수는 못 씀) */
	int shiftSeqOrders(Long tripPlanId);

	/** 장소를 바꾸는 항목의 참조를 잠깐 비운다 (UNIQUE(plan, event/place) 충돌 방지) */
	int clearContentIds(@Param("tripPlanId") Long tripPlanId, @Param("itemIds") List<Long> itemIds);

	int updateItem(TripItem item);

	/** useGeneratedKeys 로 tripItemId 가 채워진다 */
	int insertItem(TripItem item);

	int updateSeqOrder(@Param("tripPlanId") Long tripPlanId, @Param("tripItemId") Long tripItemId,
			@Param("seqOrder") Integer seqOrder);

	int deleteItem(@Param("tripPlanId") Long tripPlanId, @Param("tripItemId") Long tripItemId);
}
