package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Captures a lecture-evaluation achievement save request while preserving dynamic FR-018 fields as JSON.
 */
public class LectureEvaluationAchievementRequest {
    private Long achievementId;
    @NotBlank(message = "관리항목을 입력하세요.")
    private String managementItemCode;
    @NotNull(message = "업적발생일을 입력하세요.")
    private LocalDate occurredDate;
    private JsonNode achievementDetail;
    private String attachmentRef;

    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long achievementId) { this.achievementId = achievementId; }
    public String getManagementItemCode() { return managementItemCode; }
    public void setManagementItemCode(String managementItemCode) { this.managementItemCode = managementItemCode; }
    public LocalDate getOccurredDate() { return occurredDate; }
    public void setOccurredDate(LocalDate occurredDate) { this.occurredDate = occurredDate; }
    public JsonNode getAchievementDetail() { return achievementDetail; }
    public void setAchievementDetail(JsonNode achievementDetail) { this.achievementDetail = achievementDetail; }
    public String getAttachmentRef() { return attachmentRef; }
    public void setAttachmentRef(String attachmentRef) { this.attachmentRef = attachmentRef; }

    /** Provides the JSON representation bound to PostgreSQL jsonb without exposing storage details. */
    public String getAchievementDetailJson() {
        return achievementDetail == null ? "{}" : achievementDetail.toString();
    }
}
