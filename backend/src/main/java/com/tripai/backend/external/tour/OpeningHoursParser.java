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

        LocalTime openTime = LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        LocalTime closeTime = LocalTime.of(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
        return Optional.of(new OpeningHours(openTime, closeTime));
    }
}
