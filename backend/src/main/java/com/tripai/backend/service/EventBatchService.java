package com.tripai.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.tripai.backend.domain.dto.event.EventUpsertRow;
import com.tripai.backend.domain.dto.event.SeoulEventApiItem;
import com.tripai.backend.external.SeoulEventApiClient;
import com.tripai.backend.repository.EventBatchMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서울시 API 데이터를 받아 event 테이블에 upsert 한다.
 * 흐름: (트랜잭션 밖) API 전체 수신 -> (트랜잭션 안) 행마다 변환 후 upsert
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventBatchService {

    // 컬럼 길이 (database/schema/01_tripai_ddl_v3.2.1.sql 기준)
    private static final int LEN_EVENT_TYPE = 20;
    private static final int LEN_DISTRICT = 20;
    private static final int LEN_EVENT_NAME = 200;
    private static final int LEN_DATE_TEXT = 200;
    private static final int LEN_PLACE = 300;
    private static final int LEN_TEL_NO = 50;
    private static final int LEN_USE_FEE = 500;
    private static final int LEN_IMAGE_URL = 1000;
    private static final int LEN_HOMEPAGE = 500;

    private static final Pattern CULTCODE = Pattern.compile("cultcode=(\\d+)");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})");

    // 온라인 행사 판별 (서울시 API에는 온라인 여부 필드가 없어 제목·장소 문자열로 판단한다)
    private static final List<String> ONLINE_KEYWORDS = List.of("온라인", "비대면", "유튜브", "youtube", "zoom");
    private static final List<String> HYBRID_KEYWORDS = List.of("오프라인", "현장", "병행", "하이브리드");

    private final SeoulEventApiClient apiClient;
    private final EventBatchMapper eventBatchMapper;
    private final TransactionTemplate transactionTemplate;

    public void collectAndSave() {
        // 네트워크 호출은 트랜잭션 밖에서 (DB 커넥션을 오래 잡지 않기 위해)
        List<SeoulEventApiItem> items = apiClient.fetchAll();
        log.info("서울시 API에서 총 {}건 수신", items.size());

        Stats stats = new Stats();
        transactionTemplate.executeWithoutResult(status -> {
            for (SeoulEventApiItem item : items) {
                saveOne(item, stats);
            }
        });

        log.info("배치 완료: 저장 {}건, 건너뜀 {}건 (좌표 이상 {}건, 해시 ID 대체 {}건, 온라인 숨김 {}건)",
                stats.saved, stats.skipped, stats.badCoordinate, stats.hashIdFallback, stats.hiddenOnline);
    }

    private void saveOne(SeoulEventApiItem item, Stats stats) {
        try {
            EventUpsertRow row = toRow(item, stats);
            String problem = validate(row);
            if (problem != null) {
                stats.skipped++;
                log.warn("건너뜀 ({}): {}", problem, item.getTitle());
                return;
            }
            eventBatchMapper.upsertEvent(row);
            stats.saved++;
        } catch (Exception e) {
            // 한 건이 실패해도 배치 전체는 계속한다.
            stats.skipped++;
            log.warn("적재 실패, 건너뜀 (title={}): {}", item.getTitle(), e.getMessage());
        }
    }

    private EventUpsertRow toRow(SeoulEventApiItem item, Stats stats) {
        EventUpsertRow row = new EventUpsertRow();

        row.setEventContentId(resolveContentId(item, stats));
        row.setEventType(clean(item.getCategory()));            // 원문 그대로 (EventCategory가 원문으로 검색)
        row.setDistrictName(truncate(item.getDistrictName(), LEN_DISTRICT));
        row.setEventName(truncate(item.getTitle(), LEN_EVENT_NAME));
        row.setDateText(truncate(item.getDateText(), LEN_DATE_TEXT));
        row.setEventPlace(truncate(item.getPlace(), LEN_PLACE));
        row.setTelNo(truncate(item.getInquiry(), LEN_TEL_NO));
        row.setUseFee(truncate(item.getUseFeeText(), LEN_USE_FEE));
        row.setOverview(firstNonBlank(item.getProgram(), item.getEtcDesc()));
        row.setImageUrl(urlOrNull(item.getImageUrl(), LEN_IMAGE_URL));
        row.setHomepage(resolveHomepage(item));

        boolean online = isOnline(item);
        row.setDisplayYn(!online);
        if (online) {
            stats.hiddenOnline++;
        }

        LocalDate start = parseDate(item.getStartDate());
        LocalDate end = parseDate(item.getEndDate());
        row.setEventStartDate(start);
        row.setEventEndDate(end != null ? end : start);          // 종료일이 없으면 하루짜리로 본다
        row.setEventStartTime(parseStartTime(item.getProTime()));

        resolveCoordinates(item, row, stats);
        row.setFreeYn(resolveFreeYn(item.getIsFree(), stats));

        return row;
    }

    private String validate(EventUpsertRow row) {
        if (row.getEventName() == null) return "행사명 없음";
        if (row.getEventType() == null) return "분류(CODENAME) 없음";
        if (row.getEventType().length() > LEN_EVENT_TYPE) return "분류가 " + LEN_EVENT_TYPE + "자 초과";
        if (row.getEventStartDate() == null) return "시작일 없음 또는 파싱 실패";
        if (row.getEventEndDate().isBefore(row.getEventStartDate())) return "종료일이 시작일보다 빠름";
        return null;
    }

    /** HMPG_ADDR의 cultcode가 행사 고유번호. 없으면 제목+시작일+장소 해시로 대체. */
    private String resolveContentId(SeoulEventApiItem item, Stats stats) {
        String link = item.getHomepage();
        if (link != null) {
            Matcher m = CULTCODE.matcher(link);
            if (m.find()) {
                return "SEOUL-" + m.group(1);
            }
        }
        stats.hashIdFallback++;
        String raw = String.join("|", safe(item.getTitle()), safe(item.getStartDate()), safe(item.getPlace()));
        return "SEOUL-H-" + sha256Short(raw);
    }

    /**
     * 온라인 행사면 true. 제목이나 장소에 온라인 관련 단어가 있고, 현장·병행 표시가 없을 때만 온라인으로 본다.
     * 온라인과 오프라인을 병행하는 행사는 현장 참여가 가능하므로 숨기지 않는다.
     * 규칙을 바꿀 때는 이 메서드와 위의 키워드 목록만 고치면 된다.
     */
    private boolean isOnline(SeoulEventApiItem item) {
        String text = (safe(item.getTitle()) + " " + safe(item.getPlace())).toLowerCase();
        boolean online = ONLINE_KEYWORDS.stream().anyMatch(text::contains);
        boolean hybrid = HYBRID_KEYWORDS.stream().anyMatch(text::contains);
        return online && !hybrid;
    }

    /** 원본 주최 페이지(ORG_LINK)를 우선, 없으면 서울문화포털 상세(HMPG_ADDR). */
    private String resolveHomepage(SeoulEventApiItem item) {
        String url = urlOrNull(item.getOrgLink(), LEN_HOMEPAGE);
        return url != null ? url : urlOrNull(item.getHomepage(), LEN_HOMEPAGE);
    }

    /** "무료"/"유료"를 boolean으로. 그 외 값은 종류별로 한 번만 경고하고 유료로 처리. */
    private Boolean resolveFreeYn(String isFree, Stats stats) {
        String v = clean(isFree);
        if (v == null) return null;
        if ("무료".equals(v)) return true;
        if ("유료".equals(v)) return false;
        if (stats.unknownFree.add(v)) {
            log.warn("알 수 없는 IS_FREE 값 '{}' -> 유료(false)로 처리. 원본 확인 필요", v);
        }
        return false;
    }

    /** "2026-12-24 00:00:00.0" -> 2026-12-24 (앞 10자만 사용) */
    private LocalDate parseDate(String raw) {
        String t = clean(raw);
        if (t == null || t.length() < 10) return null;
        try {
            return LocalDate.parse(t.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** "19:30", "(일) 16:00" 같은 텍스트에서 첫 시각만 뽑는다. 종료 시각은 API에 없다. */
    private LocalTime parseStartTime(String proTime) {
        String t = clean(proTime);
        if (t == null) return null;
        Matcher m = TIME.matcher(t);
        if (!m.find()) return null;
        int hour = Integer.parseInt(m.group(1));
        int minute = Integer.parseInt(m.group(2));
        return (hour < 24 && minute < 60) ? LocalTime.of(hour, minute) : null;
    }

    /** LOT=경도, LAT=위도. 한국 범위를 벗어나면(0, 뒤바뀜 등) 보정하거나 null 처리. */
    private void resolveCoordinates(SeoulEventApiItem item, EventUpsertRow row, Stats stats) {
        BigDecimal lng = parseDecimal(item.getLng());
        BigDecimal lat = parseDecimal(item.getLat());
        if (lng == null || lat == null) {
            return;
        }
        if (!inKorea(lat, lng) && inKorea(lng, lat)) {
            BigDecimal tmp = lat;      // 위도/경도가 뒤바뀐 데이터 보정
            lat = lng;
            lng = tmp;
        }
        if (!inKorea(lat, lng)) {
            stats.badCoordinate++;
            return;
        }
        row.setMapx(lng.setScale(7, RoundingMode.HALF_UP));
        row.setMapy(lat.setScale(7, RoundingMode.HALF_UP));
    }

    private boolean inKorea(BigDecimal lat, BigDecimal lng) {
        return lat.doubleValue() >= 33 && lat.doubleValue() <= 39
                && lng.doubleValue() >= 124 && lng.doubleValue() <= 132;
    }

    private BigDecimal parseDecimal(String raw) {
        String t = clean(raw);
        if (t == null) return null;
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String clean(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private String truncate(String value, int max) {
        String t = clean(value);
        if (t == null) return null;
        return t.length() <= max ? t : t.substring(0, max);
    }

    /** URL은 잘라내면 깨지므로 길이를 넘거나 http(s)가 아니면 null. */
    private String urlOrNull(String value, int max) {
        String t = clean(value);
        if (t == null || t.length() > max) return null;
        return (t.startsWith("http://") || t.startsWith("https://")) ? t : null;
    }

    private String firstNonBlank(String a, String b) {
        String x = clean(a);
        return x != null ? x : clean(b);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String sha256Short(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다", e);
        }
    }

    private static class Stats {
        int saved;
        int skipped;
        int badCoordinate;
        int hashIdFallback;
        int hiddenOnline;
        final Set<String> unknownFree = new HashSet<>();
    }
}