package kr.ac.knue.commonfoundation.common.api;

public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("인증이 필요합니다.");
    }
}
