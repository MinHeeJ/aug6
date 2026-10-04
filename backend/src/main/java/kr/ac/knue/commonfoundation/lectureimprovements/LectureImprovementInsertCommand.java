package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;

/**
 * Carries a new lecture-improvement header through MyBatis so PostgreSQL's
 * generated achievement identifier can be written back before detail records are inserted.
 */
public class LectureImprovementInsertCommand {
    private Long achievementId;
    private final String managementNo;
    private final Long teacherUserId;
    private final String evaluationYear;
    private final String managementItemCode;
    private final LocalDate achievementDate;
    private final String attachmentIds;
    private final Long createdBy;

    public LectureImprovementInsertCommand(
            String managementNo,
            Long teacherUserId,
            String evaluationYear,
            String managementItemCode,
            LocalDate achievementDate,
            String attachmentIds,
            Long createdBy) {
        this.managementNo = managementNo;
        this.teacherUserId = teacherUserId;
        this.evaluationYear = evaluationYear;
        this.managementItemCode = managementItemCode;
        this.achievementDate = achievementDate;
        this.attachmentIds = attachmentIds;
        this.createdBy = createdBy;
    }

    public Long getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(Long achievementId) {
        this.achievementId = achievementId;
    }

    public String getManagementNo() {
        return managementNo;
    }

    public Long getTeacherUserId() {
        return teacherUserId;
    }

    public String getEvaluationYear() {
        return evaluationYear;
    }

    public String getManagementItemCode() {
        return managementItemCode;
    }

    public LocalDate getAchievementDate() {
        return achievementDate;
    }

    public String getAttachmentIds() {
        return attachmentIds;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
