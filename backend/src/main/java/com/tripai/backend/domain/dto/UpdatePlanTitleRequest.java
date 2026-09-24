package com.tripai.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePlanTitleRequest {

    @NotBlank(message = "일정 제목은 비어 있을 수 없습니다.")
    private String title;
}
