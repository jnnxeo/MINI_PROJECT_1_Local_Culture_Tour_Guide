package com.tripai.backend.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** API-PLAN-006 요청 — 초안의 모든 항목을 보낸다 (추가·삭제는 API-PLAN-007·008) */
public record DraftItemsRequest(
		@NotEmpty(message = "일정 항목이 비어 있습니다.")
		List<@NotNull(message = "비어 있는 일정 항목이 있습니다.") @Valid DraftItemRequest> items
) {
}
