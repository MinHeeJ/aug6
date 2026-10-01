package kr.ac.knue.commonfoundation.basic81;

/**
 * Returns the non-blocking occurred-date result so callers can persist a
 * valid achievement while showing the required evaluation-period warning.
 */
public record OccurredDateValidation(boolean warning, String message) {
    public static OccurredDateValidation accepted() {
        return new OccurredDateValidation(false, null);
    }

    public static OccurredDateValidation outsideEvaluationPeriod() {
        return new OccurredDateValidation(
                true,
                "업적발생일이 평가대상 기간 밖입니다. 경고를 확인한 후 저장할 수 있습니다.");
    }
}
