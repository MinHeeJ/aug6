package kr.ac.knue.commonfoundation.common.api;

import java.util.Set;

/** A typed education mutation conflict; legacy ConflictException retains its generic CONFLICT code. */
public final class EducationAchievementConflictException extends RuntimeException {
    public static final String PERIOD_NOT_ACTIVE = "PERIOD_NOT_ACTIVE";
    public static final String CONFIRMED_DATA_LOCKED = "CONFIRMED_DATA_LOCKED";
    private static final Set<String> CODES = Set.of(PERIOD_NOT_ACTIVE, CONFIRMED_DATA_LOCKED);
    private final String code;
    private final String requestId;

    /** Carries only an approved user-facing code/message and the same identifier used for the command. */
    public EducationAchievementConflictException(String code, String message, String requestId) {
        super(message);
        if (!CODES.contains(code)) {
            throw new IllegalArgumentException("Unsupported education conflict code");
        }
        this.code = code;
        this.requestId = RequestIds.normalize(requestId);
    }

    public String code() {
        return code;
    }

    public String requestId() {
        return requestId;
    }
}
