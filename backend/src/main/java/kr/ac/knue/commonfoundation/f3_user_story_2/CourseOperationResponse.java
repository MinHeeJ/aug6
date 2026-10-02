package kr.ac.knue.commonfoundation.f3_user_story_2;

import java.time.LocalDate;
import java.util.List;

/** API read model that exposes course-operation details without storage implementation data. */
public record CourseOperationResponse(
        Long achievementId,
        String managementNo,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String performanceDetails,
        String certificationStatus,
        List<String> attachmentIds) {
}
