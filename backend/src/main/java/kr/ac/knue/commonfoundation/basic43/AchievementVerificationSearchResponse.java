package kr.ac.knue.commonfoundation.basic43;

import java.util.List;

public record AchievementVerificationSearchResponse(
        List<AchievementVerificationRow> targets,
        int page,
        int size,
        long totalElements) {
}
