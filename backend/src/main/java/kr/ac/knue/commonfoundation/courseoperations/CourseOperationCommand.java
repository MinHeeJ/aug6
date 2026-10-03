package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;

/** Internal write model retaining the generated identifier across one transaction. */
public class CourseOperationCommand {
    private Long achievementId;
    private final Long teacherUserId;
    private final Long actorUserId;
    private final String managementItemCode;
    private final LocalDate achievementDate;
    private final String performanceDetails;
    private final String[] attachmentIds;

    public CourseOperationCommand(
            Long achievementId,
            Long teacherUserId,
            Long actorUserId,
            String managementItemCode,
            LocalDate achievementDate,
            String performanceDetails,
            String[] attachmentIds) {
        this.achievementId = achievementId;
        this.teacherUserId = teacherUserId;
        this.actorUserId = actorUserId;
        this.managementItemCode = managementItemCode;
        this.achievementDate = achievementDate;
        this.performanceDetails = performanceDetails;
        this.attachmentIds = attachmentIds;
    }

    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long achievementId) { this.achievementId = achievementId; }
    public Long getTeacherUserId() { return teacherUserId; }
    public Long getActorUserId() { return actorUserId; }
    public String getManagementItemCode() { return managementItemCode; }
    public LocalDate getAchievementDate() { return achievementDate; }
    public String getPerformanceDetails() { return performanceDetails; }
    public String[] getAttachmentIds() { return attachmentIds; }
}
