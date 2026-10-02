package kr.ac.knue.commonfoundation.common.api;

/**
 * Holds the request identifier for the active servlet thread so response envelopes and logs share
 * one traceable value.
 */
public final class RequestIdContext {
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestIdContext() {
    }

    public static String get() {
        return REQUEST_ID.get();
    }

    public static void set(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
