package com.tripai.backend.domain.dto.plan;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RecommendPlanRequest(
        @NotBlank(message = "행사를 선택해주세요.")
        String eventId,

        @NotNull(message = "여행 날짜를 선택해주세요.")
        LocalDate visitDate,

        // 4는 피그마의 '4명 이상' 선택값이다.
        @NotNull(message = "인원을 선택해주세요.")
        @Min(value = 1, message = "인원은 1명 이상이어야 합니다.")
        @Max(value = 4, message = "인원 선택값을 확인해주세요.")
        Integer headcount,

        @NotBlank(message = "이동 방법을 선택해주세요.")
        @Pattern(
                regexp = "WALK_TRANSIT|WALK_ONLY",
                message = "이동 방법을 확인해주세요."
        )
        String transportMode,

        @NotEmpty(message = "관심사를 하나 이상 선택해주세요.")
        Set<
                @Pattern(
                        regexp = "CULTURE_HISTORY|MUSIC_PERFORMANCE|ART_EXHIBITION",
                        message = "관심사 값을 확인해주세요."
                )
                String
        > interests,

        @NotNull(message = "AI 사용 여부를 선택해주세요.")
        Boolean useAi
) {}