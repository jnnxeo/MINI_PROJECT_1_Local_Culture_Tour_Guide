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
            "(?<!\\d)([01]?\\d|2[0-3]):([0-5]\\d)\\s*(?:~|∼|-|–)\\s*([01]?\\d|2[0-3]):([0-5]\\d)(?!\\d)"
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

        Matcher matcher = TIME_RANGE.matcher(businessHoursText);
        if (!matcher.find()) {
            return Optional.empty();
        }

        TimeRange businessHours = toRange(matcher);
        Matcher breakMatcher = BREAK_TIME_RANGE.matcher(businessHoursText);
        TimeRange breakHours = breakMatcher.find() ? toRange(breakMatcher) : null;

        return Optional.of(new OpeningHours(
                businessHours.start(),
                businessHours.end(),
                breakHours == null ? null : breakHours.start(),
                breakHours == null ? null : breakHours.end()
        ));
    }

    private static TimeRange toRange(Matcher matcher) {
        return new TimeRange(
                LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))),
                LocalTime.of(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)))
        );
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
