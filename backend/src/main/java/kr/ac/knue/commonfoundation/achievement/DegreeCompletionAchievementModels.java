package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Transport models for the BASIC-79 degree-completion achievement HTTP contract. */
public final class DegreeCompletionAchievementModels {
    private DegreeCompletionAchievementModels() {
    }

    public record SearchCriteria(
            int page,
            int size,
            String managementNo,
            String teacherName,
            String certificationStatus
    ) {
        public int safeSize() {
            return size == 20 || size == 50 || size == 100 ? size : 20;
        }
    }

    public record SearchResponse(List<Row> achievements, int page, int size, long totalElements) {
    }

    public record Student(
            Long degreeCompletionStudentId,
            String degreeType,
            String studentName,
            String thesisTitle,
            LocalDate degreeAwardedDate
    ) {
    }

    public record Header(
            Long achievementId,
            String managementNo,
            Long teacherUserId,
            String teacherName,
            String organizationCode,
            String evaluationYear,
            String managementItemCode,
            LocalDate occurredDate,
            String achievementDetail,
            String certificationStatus,
            String attachmentRef,
            LocalDateTime updatedAt
    ) {
    }

    public record Row(
            Long achievementId,
            String managementNo,
            Long teacherUserId,
            String teacherName,
            String organizationCode,
            String evaluationYear,
            String managementItemCode,
            LocalDate occurredDate,
            String achievementDetail,
            String certificationStatus,
            String attachmentRef,
            List<Student> students,
            boolean occurredDateOutOfRangeWarning,
            LocalDateTime updatedAt
    ) {
        public static Row from(Header header, List<Student> students, boolean warning) {
            return new Row(
                    header.achievementId(),
                    header.managementNo(),
                    header.teacherUserId(),
                    header.teacherName(),
                    header.organizationCode(),
                    header.evaluationYear(),
                    header.managementItemCode(),
                    header.occurredDate(),
                    header.achievementDetail(),
                    header.certificationStatus(),
                    header.attachmentRef(),
                    students,
                    warning,
                    header.updatedAt()
            );
        }
    }
}
