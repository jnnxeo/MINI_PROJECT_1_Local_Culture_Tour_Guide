package com.tripai.backend.repository;

import org.apache.ibatis.annotations.Mapper;

import com.tripai.backend.domain.dto.event.EventUpsertRow;

@Mapper
public interface EventBatchMapper {

    /** event_content_id 기준으로 있으면 갱신, 없으면 삽입 */
    int upsertEvent(EventUpsertRow row);
}