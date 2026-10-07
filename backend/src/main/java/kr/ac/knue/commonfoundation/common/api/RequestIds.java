package kr.ac.knue.commonfoundation.common.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/** Normalizes one correlation identifier for response metadata, conflict errors and audit writes. */
public final class RequestIds {
    public static final String ATTRIBUTE = RequestIds.class.getName() + ".requestId";

    private RequestIds() {
    }

    /** Preserves trimmed caller identifiers; absent identifiers receive an opaque server UUID. */
    public static String normalize(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }

    /** Bind once at the controller boundary and reuse the return value for service/audit calls. */
    public static String resolve(HttpServletRequest request) {
        Object existing = request.getAttribute(ATTRIBUTE);
        if (existing instanceof String value && !value.isBlank()) {
            return value;
        }
        String value = normalize(request.getHeader("X-Request-Id"));
        request.setAttribute(ATTRIBUTE, value);
        return value;
    }
}
