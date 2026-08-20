package kr.ac.knue.commonfoundation.settings;

import java.util.Arrays;
import java.util.List;

public enum CommonSystemSettingKey {
    SESSION_IDLE_MINUTES("세션 유휴시간"),
    PAGE_SIZE("페이지당 조회건수"),
    DEFAULT_SEARCH_PERIOD("기본 검색기간"),
    BULK_QUERY_THRESHOLD("대량조회 기준건수"),
    LONG_TASK_NOTICE_THRESHOLD("장시간작업 안내 기준");

    private final String label;

    CommonSystemSettingKey(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static boolean isAllowed(String key) {
        if (key == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(value -> value.name().equals(key));
    }

    public static List<String> orderedKeys() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
