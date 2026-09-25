package com.tripai.backend.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 일정 항목 검증 규칙 — API-PLAN-006·007·008 이 같이 쓴다.
 * 근거: TRIP-004(시간 중복·일정 범위), TRIP-006(중복 장소, 문화행사 최대 2개),
 *       TRIP-007(핵심 문화행사 삭제 불가), DDL CK_TRIP_ITEM_DURATION(duration_min > 0)
 * 어떤 HTTP 상태로 응답할지는 API마다 달라서 여기서는 위반 종류만 돌려준다.
 */
final class PlanItemRules {

    static final int EVENT_LIMIT = 2;

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    enum Violation {
        INVALID_DURATION,
        DUPLICATE,
        CORE_EVENT_MISSING,
        EVENT_LIMIT,
        OUT_OF_RANGE,
        TIME_OVERLAP,
        SEQUENCE_ORDER
    }

    record Candidate(
            String type,
            String contentId,
            String name,
            LocalTime startTime,
            int durationMin,
            Integer sequence
    ) {

        int startMinutes() {
            return startTime.getHour() * 60 + startTime.getMinute();
        }

        int endMinutes() {
            return startMinutes() + durationMin;
        }
    }

    record Result(Violation violation, String message) {
    }

    private PlanItemRules() {
    }

    /**
     * @param coreEventId   추천 기준 문화행사(trip_plan.anchor_content_id)
     * @param windowStart   방문 가능 시작 시각 (없으면 제한 없음)
     * @param windowEnd     방문 가능 종료 시각 (없으면 제한 없음)
     * @param checkSequence true 면 sequence 가 1..n 이고 시간 순서와 같은지도 본다 (API-PLAN-006)
     */
    static Optional<Result> check(
            List<Candidate> items,
            String coreEventId,
            LocalTime windowStart,
            LocalTime windowEnd,
            boolean checkSequence
    ) {
        for (Candidate item : items) {
            if (item.durationMin() <= 0 || item.endMinutes() > MINUTES_PER_DAY) {
                return fail(Violation.INVALID_DURATION, "머무는 시간을 확인해 주세요.");
            }
        }

        Set<String> keys = new HashSet<>();
        for (Candidate item : items) {
            if (!keys.add(item.type() + ":" + item.contentId())) {
                return fail(Violation.DUPLICATE, "이미 일정에 있는 장소입니다.");
            }
        }

        boolean hasCoreEvent = items.stream()
                .anyMatch(item -> "EVENT".equals(item.type()) && Objects.equals(item.contentId(), coreEventId));
        if (!hasCoreEvent) {
            return fail(Violation.CORE_EVENT_MISSING,
                    "핵심 문화행사는 일정에서 뺄 수 없습니다. 다시 추천받거나 다른 행사를 선택해 주세요.");
        }

        long eventCount = items.stream().filter(item -> "EVENT".equals(item.type())).count();
        if (eventCount > EVENT_LIMIT) {
            return fail(Violation.EVENT_LIMIT, "문화행사는 일정당 최대 2개까지 추가할 수 있습니다.");
        }

        if (windowStart != null || windowEnd != null) {
            int from = windowStart == null ? 0 : minutes(windowStart);
            int to = windowEnd == null ? MINUTES_PER_DAY : minutes(windowEnd);

            for (Candidate item : items) {
                if (item.startMinutes() < from || item.endMinutes() > to) {
                    return fail(Violation.OUT_OF_RANGE, String.format(
                            "%s 방문 시간이 가능한 시간(%s–%s)을 벗어났습니다.",
                            item.name(), text(from), text(to)));
                }
            }
        }

        List<Candidate> byTime = items.stream()
                .sorted(Comparator.comparingInt(Candidate::startMinutes))
                .toList();
        for (int i = 1; i < byTime.size(); i++) {
            Candidate previous = byTime.get(i - 1);
            Candidate current = byTime.get(i);
            if (previous.endMinutes() > current.startMinutes()) {
                return fail(Violation.TIME_OVERLAP, String.format(
                        "%s(%s–%s)와 %s 방문 시간이 겹칩니다.",
                        previous.name(), text(previous.startMinutes()), text(previous.endMinutes()), current.name()));
            }
        }

        if (checkSequence) {
            List<Integer> sequences = items.stream().map(Candidate::sequence).toList();
            List<Integer> expected = IntStream.rangeClosed(1, items.size()).boxed().toList();
            if (sequences.contains(null) || !sequences.stream().sorted().toList().equals(expected)) {
                return fail(Violation.SEQUENCE_ORDER, "방문 순서는 1번부터 빠짐없이 매겨야 합니다.");
            }

            List<Integer> sequenceByTime = byTime.stream().map(Candidate::sequence).toList();
            if (!sequenceByTime.equals(expected)) {
                return fail(Violation.SEQUENCE_ORDER, "방문 순서가 시간 순서와 맞지 않습니다.");
            }
        }

        return Optional.empty();
    }

    /** 시작 시각 순으로 1부터 다시 매긴 순번 목록 (TRIP-004: 시간 변경 후 방문 순서 재계산) */
    static List<Candidate> resequenceByTime(List<Candidate> items) {
        List<Candidate> byTime = items.stream()
                .sorted(Comparator.comparingInt(Candidate::startMinutes))
                .toList();
        return IntStream.range(0, byTime.size())
                .mapToObj(index -> {
                    Candidate item = byTime.get(index);
                    return new Candidate(item.type(), item.contentId(), item.name(),
                            item.startTime(), item.durationMin(), index + 1);
                })
                .toList();
    }

    private static Optional<Result> fail(Violation violation, String message) {
        return Optional.of(new Result(violation, message));
    }

    private static int minutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    private static String text(int minutes) {
        if (minutes >= MINUTES_PER_DAY) {
            return "24:00";
        }
        return LocalTime.of(minutes / 60, minutes % 60).format(HH_MM);
    }
}
