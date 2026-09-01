package kr.ac.knue.commonfoundation.basic36;

import java.util.List;

public record AchievementDataHistorySearchResponse(
        List<AchievementDataHistoryRow> histories,
        int page,
        int size,
        long totalElements) {
}
