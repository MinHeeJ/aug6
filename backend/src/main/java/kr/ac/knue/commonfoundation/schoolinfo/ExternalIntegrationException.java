package kr.ac.knue.commonfoundation.schoolinfo;

public class ExternalIntegrationException extends RuntimeException {
    public ExternalIntegrationException(String message) {
        super(message);
    }

    public ExternalIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
