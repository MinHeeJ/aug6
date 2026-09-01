package kr.ac.knue.commonfoundation.basic36;

public record AchievementDataAsOfSearchCriteria(
        int page,
        int size,
        String achievementType,
        String achievementKey,
        String employeeNo,
        String asOfAt) {
    public int safeSize() {
        if (size != 20 && size != 50 && size != 100) return 20;
        return size;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }
}
