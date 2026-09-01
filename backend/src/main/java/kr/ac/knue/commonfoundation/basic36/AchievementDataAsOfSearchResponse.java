package kr.ac.knue.commonfoundation.basic36;

import java.util.List;

public record AchievementDataAsOfSearchResponse(
        List<AchievementDataAsOfRow> snapshots,
        int page,
        int size,
        long totalElements) {
}
