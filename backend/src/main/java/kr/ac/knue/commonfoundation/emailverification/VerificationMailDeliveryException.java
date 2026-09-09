package kr.ac.knue.commonfoundation.emailverification;

public class VerificationMailDeliveryException extends RuntimeException {
    private final String deliveryStatus;
    private final String diagnosticCategory;
    private final boolean retryEligible;

    public VerificationMailDeliveryException(
            String deliveryStatus,
            String diagnosticCategory,
            String message,
            boolean retryEligible
    ) {
        super(message);
        this.deliveryStatus = deliveryStatus;
        this.diagnosticCategory = diagnosticCategory;
        this.retryEligible = retryEligible;
    }

    public String deliveryStatus() {
        return deliveryStatus;
    }

    public String diagnosticCategory() {
        return diagnosticCategory;
    }

    public boolean retryEligible() {
        return retryEligible;
    }
}
