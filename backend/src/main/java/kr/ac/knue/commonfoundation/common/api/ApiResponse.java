package kr.ac.knue.commonfoundation.common.api;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ApiResponse<T>(boolean success, T data, ApiError error, Map<String, Object> meta) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, defaultMeta());
    }

    public static ApiResponse<Void> empty() {
        return new ApiResponse<>(true, null, null, defaultMeta());
    }

    public static ApiResponse<Void> fail(ApiError error) {
        return new ApiResponse<>(false, null, error, defaultMeta());
    }

    private static Map<String, Object> defaultMeta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", OffsetDateTime.now().toString());
        meta.put("traceId", UUID.randomUUID().toString());
        return meta;
    }
}
