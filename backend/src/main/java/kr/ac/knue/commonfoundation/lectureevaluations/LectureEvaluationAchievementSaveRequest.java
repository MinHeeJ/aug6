package kr.ac.knue.commonfoundation.lectureevaluations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Incoming mutable payload for a lecture-evaluation achievement save. */
public class LectureEvaluationAchievementSaveRequest {
    private Long achievementId;
    @NotBlank(message = "관리항목 코드는 필수입니다.")
    private String managementItemCode;
    @NotNull(message = "업적발생일은 필수입니다.")
    private LocalDate occurredDate;
    @NotBlank(message = "소속 조직 코드는 필수입니다.")
    private String organizationCode;
    @NotBlank(message = "업적 내용은 필수입니다.")
    private String achievementDetail;
    private String attachmentRef;
    private String certificationStatus;
    private String changeReason;

    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long achievementId) { this.achievementId = achievementId; }
    public String getManagementItemCode() { return managementItemCode; }
    public void setManagementItemCode(String managementItemCode) { this.managementItemCode = managementItemCode; }
    public LocalDate getOccurredDate() { return occurredDate; }
    public void setOccurredDate(LocalDate occurredDate) { this.occurredDate = occurredDate; }
    public String getOrganizationCode() { return organizationCode; }
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }
    public String getAchievementDetail() { return achievementDetail; }
    public void setAchievementDetail(String achievementDetail) { this.achievementDetail = achievementDetail; }
    public String getAttachmentRef() { return attachmentRef; }
    public void setAttachmentRef(String attachmentRef) { this.attachmentRef = attachmentRef; }
    public String getCertificationStatus() { return certificationStatus; }
    public void setCertificationStatus(String certificationStatus) { this.certificationStatus = certificationStatus; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
