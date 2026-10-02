package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;

/**
 * Binds the optional save metadata that supplements the required OpenAPI lecture-evaluation
 * fields. The owner is taken from the authenticated user unless an authorized manager supplies it.
 */
public class LectureEvaluationSaveRequest {
    private Long achievementId;
    private Long teacherUserId;
    private String organizationCode;
    private String evaluationYear;
    private String managementItemCode;
    private LocalDate occurredDate;
    private JsonNode achievementDetail;
    private String attachmentRef;
    private String actionType;
    private String reasonCode;
    private String opinion;

    public Long getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(Long achievementId) {
        this.achievementId = achievementId;
    }

    public Long getTeacherUserId() {
        return teacherUserId;
    }

    public void setTeacherUserId(Long teacherUserId) {
        this.teacherUserId = teacherUserId;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public String getEvaluationYear() {
        return evaluationYear;
    }

    public void setEvaluationYear(String evaluationYear) {
        this.evaluationYear = evaluationYear;
    }

    public String getManagementItemCode() {
        return managementItemCode;
    }

    public void setManagementItemCode(String managementItemCode) {
        this.managementItemCode = managementItemCode;
    }

    public LocalDate getOccurredDate() {
        return occurredDate;
    }

    public void setOccurredDate(LocalDate occurredDate) {
        this.occurredDate = occurredDate;
    }

    public JsonNode getAchievementDetail() {
        return achievementDetail;
    }

    public void setAchievementDetail(JsonNode achievementDetail) {
        this.achievementDetail = achievementDetail;
    }

    public String getAttachmentRef() {
        return attachmentRef;
    }

    public void setAttachmentRef(String attachmentRef) {
        this.attachmentRef = attachmentRef;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }
}
