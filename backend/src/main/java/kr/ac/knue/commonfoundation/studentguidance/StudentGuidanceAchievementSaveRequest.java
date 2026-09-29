package kr.ac.knue.commonfoundation.studentguidance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Mutable request for atomically saving a student-guidance header and its students. */
public class StudentGuidanceAchievementSaveRequest {
    private Long achievementId;
    @NotBlank(message = "관리항목 코드는 필수입니다.") private String managementItemCode;
    @NotBlank(message = "소속 조직 코드는 필수입니다.") private String organizationCode;
    @NotNull(message = "업적발생일은 필수입니다.") private LocalDate occurredDate;
    @NotNull(message = "지도 시작일은 필수입니다.") private LocalDate guidanceStartDate;
    @NotNull(message = "지도 종료일은 필수입니다.") private LocalDate guidanceEndDate;
    private String achievementDetail;
    private String attachmentRef;
    private String certificationStatus;
    private String changeReason;
    private List<StudentGuidanceStudentRequest> students;

    public Long getAchievementId() { return achievementId; } public void setAchievementId(Long value) { achievementId = value; }
    public String getManagementItemCode() { return managementItemCode; } public void setManagementItemCode(String value) { managementItemCode = value; }
    public String getOrganizationCode() { return organizationCode; } public void setOrganizationCode(String value) { organizationCode = value; }
    public LocalDate getOccurredDate() { return occurredDate; } public void setOccurredDate(LocalDate value) { occurredDate = value; }
    public LocalDate getGuidanceStartDate() { return guidanceStartDate; } public void setGuidanceStartDate(LocalDate value) { guidanceStartDate = value; }
    public LocalDate getGuidanceEndDate() { return guidanceEndDate; } public void setGuidanceEndDate(LocalDate value) { guidanceEndDate = value; }
    public String getAchievementDetail() { return achievementDetail; } public void setAchievementDetail(String value) { achievementDetail = value; }
    public String getAttachmentRef() { return attachmentRef; } public void setAttachmentRef(String value) { attachmentRef = value; }
    public String getCertificationStatus() { return certificationStatus; } public void setCertificationStatus(String value) { certificationStatus = value; }
    public String getChangeReason() { return changeReason; } public void setChangeReason(String value) { changeReason = value; }
    public List<StudentGuidanceStudentRequest> getStudents() { return students; } public void setStudents(List<StudentGuidanceStudentRequest> value) { students = value; }
}
