package com.tripai.backend.domain.dto.plan;

public record EventPlanItemResponse(
        Long itemId,
        int seq,
        String type,
        String contentId,
        String name,
        String category,
        String address,
        Double lat,
        Double lng,
        String imageUrl,
        String startTime,
        int durationMin,
        String endTime,
        boolean timeFixed,
        String aiReason,
        String openTime,
        String closeTime
) {}
