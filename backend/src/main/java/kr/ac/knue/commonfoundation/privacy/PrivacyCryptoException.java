package kr.ac.knue.commonfoundation.privacy;

public class PrivacyCryptoException extends RuntimeException {
    public PrivacyCryptoException(String message) {
        super(message);
    }

    public PrivacyCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
