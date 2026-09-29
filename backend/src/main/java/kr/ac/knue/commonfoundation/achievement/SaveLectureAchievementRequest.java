package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;

/** Input accepted when a teacher or authorized evaluator creates a lecture achievement. */
public record SaveLectureAchievementRequest(
        String managementItemCode,
        LocalDate occurredDate,
        JsonNode achievementDetail) {
}
