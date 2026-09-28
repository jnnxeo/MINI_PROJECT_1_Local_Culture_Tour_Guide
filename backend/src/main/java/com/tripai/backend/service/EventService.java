package com.tripai.backend.service;

import org.springframework.stereotype.Service;

import com.tripai.backend.domain.dto.event.EventDetailResponse;
import com.tripai.backend.repository.EventMapper;

import com.tripai.backend.global.exception.CustomException;
import com.tripai.backend.global.exception.ErrorCode;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {


    private final EventMapper eventMapper;

    // Event-001 행사 상세 조회
    // 예외 : 누락 항목은 "정보없음"으로 표시 -> 프론트에서 null 체크로 처리 (DTO는 NULL 그대로 내려줌)
    public EventDetailResponse getEventDetail(String eventId, Long userId) {

        EventDetailResponse event = eventMapper.selectEventDetail(eventId, userId);

        if(event == null) {
            throw new CustomException(ErrorCode.EVENT_NOT_FOUND);
        }
        return event;
    }
}