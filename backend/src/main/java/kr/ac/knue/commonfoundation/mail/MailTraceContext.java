package kr.ac.knue.commonfoundation.mail;

import java.util.UUID;
import kr.ac.knue.commonfoundation.common.request.RequestIdFilter;
import org.slf4j.MDC;

public final class MailTraceContext {
    private MailTraceContext() {
    }

    public static String currentRequestId() {
        return normalizeRequestId(MDC.get(RequestIdFilter.MDC_KEY));
    }

    public static String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.trim().isBlank()) {
            return UUID.randomUUID().toString();
        }
        String trimmed = requestId.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }
}
