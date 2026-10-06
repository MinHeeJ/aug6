package kr.ac.knue.commonfoundation.signup;

/** Sanitizes persistence failures so the shared exception logger cannot expose SQL bindings. */
public class SignupPersistenceException extends RuntimeException {
    public SignupPersistenceException() {
        super("회원가입 처리 중 오류가 발생했습니다.");
    }
}
