package com.tripai.backend.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 컨트롤러 @Valid 에서 400 으로 막혀야 하는 요청 (SEC-005) */
class PlanRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 항목_목록에_비어_있는_원소가_있으면_막는다() {
        // GPT 검토 E4/E9: items:[null] 이 검증을 통과한 뒤 서비스에서 NullPointerException 이 났다
        DraftItemsRequest request = new DraftItemsRequest(Arrays.asList((DraftItemRequest) null));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void 머무는_시간은_하루_1440분을_넘을_수_없다() {
        DraftItemRequest tooLong = new DraftItemRequest(1L, "PLACE", "PL-1", "12:30", 1441, 1);
        DraftItemRequest oneDay = new DraftItemRequest(1L, "PLACE", "PL-1", "00:00", 1440, 1);
        DraftItemAddRequest overflow = new DraftItemAddRequest("PL-1", "PLACE", "18:00", Integer.MAX_VALUE, null);

        assertThat(validator.validate(new DraftItemsRequest(List.of(tooLong)))).isNotEmpty();
        assertThat(validator.validate(new DraftItemsRequest(List.of(oneDay)))).isEmpty();
        assertThat(validator.validate(overflow)).isNotEmpty();
    }
}
