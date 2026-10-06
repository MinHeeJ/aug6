package kr.ac.knue.commonfoundation.signup;

/** Carries a safe duplicate-field error, never a database exception or submitted value. */
public class SignupConflictException extends RuntimeException {
    private final String field;

    public SignupConflictException(String field) {
        super("userId".equals(field) ? "이미 사용 중인 아이디입니다." : "이미 등록된 이메일입니다.");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
