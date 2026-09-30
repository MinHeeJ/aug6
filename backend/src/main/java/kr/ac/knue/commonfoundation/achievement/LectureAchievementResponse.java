package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;

/** API response objects for lecture list and save operations. */
public final class LectureAchievementResponse {
    private LectureAchievementResponse() { }
    public record Row(Long achievementId, String managementNo, String evaluationYear, String organizationCode,
            Long teacherUserId, String teacherName, String managementItemCode, LocalDate occurredDate,
            EducationAchievementStatus certificationStatus, JsonNode achievementDetail, Integer attachmentCount,
            boolean occurrenceDateWarning) { }
    public record Search(List<Row> items, int page, int pageSize, long totalCount) { }
}
