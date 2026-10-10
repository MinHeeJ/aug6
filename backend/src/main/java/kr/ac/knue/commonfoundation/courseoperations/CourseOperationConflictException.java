package kr.ac.knue.commonfoundation.courseoperations;

/** Typed course business conflict; legacy CONFLICT responses remain unchanged elsewhere. */
public class CourseOperationConflictException extends RuntimeException {
    private final String code;

    public CourseOperationConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
