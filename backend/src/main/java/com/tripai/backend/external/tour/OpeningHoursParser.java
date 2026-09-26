package com.tripai.backend.external.tour;

import java.time.LocalTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TourAPI opentimefood는 자유 형식 문자열이라 확실한 HH:mm~HH:mm 한 구간만 TIME으로 저장한다.
 * 파싱하지 못한 값은 원문만 business_hours_text에 보존하고 자동 추천 후보에서는 제외한다.
 */
public final class OpeningHoursParser {

    private static final Pattern TIME_RANGE = Pattern.compile(
            "(?<!\\d)([01]?\\d|2[0-3]|24):([0-5]\\d)\\s*(?:~|∼|-|–)\\s*([01]?\\d|2[0-3]|24):([0-5]\\d)(?!\\d)"
    );
    private static final Pattern BREAK_TIME_RANGE = Pattern.compile(
            "(?:브레이크\\s*타임|브레이크|준비시간|휴게시간|휴식시간)\\s*[:：]?\\s*"
                    + "([01]?\\d|2[0-3]):([0-5]\\d)\\s*(?:~|∼|-|–)\\s*([01]?\\d|2[0-3]):([0-5]\\d)",
            Pattern.CASE_INSENSITIVE
    );

    private OpeningHoursParser() {
    }

    public static Optional<OpeningHours> parse(String businessHoursText) {
        if (businessHoursText == null || businessHoursText.isBlank()) {
            return Optional.empty();
        }

        Matcher breakMatcher = BREAK_TIME_RANGE.matcher(businessHoursText);
        boolean hasBreak = breakMatcher.find();
        TimeRange breakHours = hasBreak ? toRange(breakMatcher) : null;

        TimeRange businessHours = findBusinessHours(
                businessHoursText, hasBreak ? breakMatcher.start() : -1, hasBreak ? breakMatcher.end() : -1
        );
        if (businessHours == null) {
            return Optional.empty();
        }

        return Optional.of(new OpeningHours(
                businessHours.start(),
                businessHours.end(),
                breakHours == null ? null : breakHours.start(),
                breakHours == null ? null : breakHours.end()
        ));
    }

    /**
     * 준비시간·브레이크타임 표현 안에 있는 시간 구간은 영업시간 후보에서 제외한다.
     * (예: "12:00~24:00<br>준비시간 15:00~17:30"에서 첫 구간이 자정 표기·오타 등으로
     * 매칭에 실패하면, 뒤의 준비시간 구간을 영업시간으로 잘못 읽을 수 있다.)
     */
    private static TimeRange findBusinessHours(String text, int breakStart, int breakEnd) {
        Matcher matcher = TIME_RANGE.matcher(text);
        while (matcher.find()) {
            boolean isBreakRange = breakStart >= 0 && matcher.start() >= breakStart && matcher.end() <= breakEnd;
            if (!isBreakRange) {
                return toRange(matcher);
            }
        }
        return null;
    }

    private static TimeRange toRange(Matcher matcher) {
        return new TimeRange(
                toTime(matcher.group(1), matcher.group(2)),
                toTime(matcher.group(3), matcher.group(4))
        );
    }

    /** TourAPI 원문은 자정을 종종 "24:00"으로 적는다 — TIME 컬럼에 맞춰 00:00(자정)으로 정규화한다. */
    private static LocalTime toTime(String hourText, String minuteText) {
        int hour = Integer.parseInt(hourText);
        int minute = Integer.parseInt(minuteText);
        return hour == 24 ? LocalTime.MIDNIGHT : LocalTime.of(hour, minute);
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
