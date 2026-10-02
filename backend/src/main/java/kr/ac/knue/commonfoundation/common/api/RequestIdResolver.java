package kr.ac.knue.commonfoundation.common.api;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Normalizes client supplied request identifiers and generates safe values when a request has no
 * usable identifier.
 */
public final class RequestIdResolver {
    private static final int MAX_REQUEST_ID_LENGTH = 128;
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]*");

    private RequestIdResolver() {
    }

    public static String resolve(String suppliedRequestId) {
        if (suppliedRequestId == null) {
            return UUID.randomUUID().toString();
        }

        String candidate = suppliedRequestId.trim();
        if (candidate.isEmpty()
                || candidate.length() > MAX_REQUEST_ID_LENGTH
                || !REQUEST_ID_PATTERN.matcher(candidate).matches()) {
            return UUID.randomUUID().toString();
        }
        return candidate;
    }
}
