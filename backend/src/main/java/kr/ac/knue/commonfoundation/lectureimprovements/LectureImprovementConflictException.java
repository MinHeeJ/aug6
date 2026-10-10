package kr.ac.knue.commonfoundation.lectureimprovements;

/** Resource-specific conflict carries a stable code without changing legacy CONFLICT errors. */
public class LectureImprovementConflictException extends RuntimeException {
    private final String code;

    public LectureImprovementConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
