package kr.ac.knue.commonfoundation.exceptionperiod;

public record ExceptionPeriodDecision(
        boolean allowed,
        boolean exceptionApplied,
        String reason,
        Long settingId
) {
    public static ExceptionPeriodDecision allow(ExceptionPeriodRow row) {
        return new ExceptionPeriodDecision(true, true, "예외기간이 일반 수정기간보다 우선 적용됩니다.", row.settingId());
    }

    public static ExceptionPeriodDecision allowByGeneralPeriod() {
        return new ExceptionPeriodDecision(true, false, "일반 수정기간 규칙으로 허용됩니다.", null);
    }

    public static ExceptionPeriodDecision deny(String reason) {
        return new ExceptionPeriodDecision(false, false, reason, null);
    }
}
