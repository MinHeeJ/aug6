package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a course operation source row and its source-controlled detail. */
public class CourseOperationRow {
    private Long achievementId;
    private Long teacherUserId;
    private String teacherName;
    private String managementItemCode;
    private LocalDate achievementDate;
    private String performanceDetails;
    private String achievementStatus;
    private String[] attachmentIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CourseOperationRow() {
    }

    public CourseOperationRow(Long achievementId, Long teacherUserId, String teacherName,
            String managementItemCode, LocalDate achievementDate, String performanceDetails,
            String achievementStatus, String[] attachmentIds, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.achievementId = achievementId;
        this.teacherUserId = teacherUserId;
        this.teacherName = teacherName;
        this.managementItemCode = managementItemCode;
        this.achievementDate = achievementDate;
        this.performanceDetails = performanceDetails;
        this.achievementStatus = achievementStatus;
        this.attachmentIds = attachmentIds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long achievementId) { this.achievementId = achievementId; }
    public Long getTeacherUserId() { return teacherUserId; }
    public void setTeacherUserId(Long teacherUserId) { this.teacherUserId = teacherUserId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getManagementItemCode() { return managementItemCode; }
    public void setManagementItemCode(String managementItemCode) { this.managementItemCode = managementItemCode; }
    public LocalDate getAchievementDate() { return achievementDate; }
    public void setAchievementDate(LocalDate achievementDate) { this.achievementDate = achievementDate; }
    public String getPerformanceDetails() { return performanceDetails; }
    public void setPerformanceDetails(String performanceDetails) { this.performanceDetails = performanceDetails; }
    public String getAchievementStatus() { return achievementStatus; }
    public void setAchievementStatus(String achievementStatus) { this.achievementStatus = achievementStatus; }
    public String[] getAttachmentIds() { return attachmentIds; }
    public void setAttachmentIds(String[] attachmentIds) { this.attachmentIds = attachmentIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
