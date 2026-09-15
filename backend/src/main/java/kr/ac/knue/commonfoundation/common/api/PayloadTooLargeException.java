package kr.ac.knue.commonfoundation.common.api;

/** Raised when an uploaded attachment exceeds the documented request size. */
public class PayloadTooLargeException extends RuntimeException {
    public PayloadTooLargeException(String message) {
        super(message);
    }
}
