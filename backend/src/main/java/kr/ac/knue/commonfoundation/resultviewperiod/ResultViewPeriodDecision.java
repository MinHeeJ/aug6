package kr.ac.knue.commonfoundation.resultviewperiod;

public record ResultViewPeriodDecision(
        boolean allowed,
        String reason,
        Long settingId,
        String visibilityScope
) {
    public static ResultViewPeriodDecision allow(ResultViewPeriodRow row) {
        return new ResultViewPeriodDecision(true, "결과조회 공개기간과 공개범위 안에서 조회할 수 있습니다.",
                row.settingId(), row.visibilityScope());
    }

    public static ResultViewPeriodDecision deny(String reason) {
        return new ResultViewPeriodDecision(false, reason, null, null);
    }
}
