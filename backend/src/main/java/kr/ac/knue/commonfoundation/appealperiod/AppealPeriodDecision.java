package kr.ac.knue.commonfoundation.appealperiod;

public record AppealPeriodDecision(
        boolean allowed,
        String reason,
        Long appealPeriodSettingId,
        Long handlerUserId
) {
    public static AppealPeriodDecision allow(AppealPeriodRow row) {
        return new AppealPeriodDecision(true, "이의신청 제출 가능 기간입니다.", row.settingId(), row.handlerUserId());
    }

    public static AppealPeriodDecision deny(String reason) {
        return new AppealPeriodDecision(false, reason, null, null);
    }
}
