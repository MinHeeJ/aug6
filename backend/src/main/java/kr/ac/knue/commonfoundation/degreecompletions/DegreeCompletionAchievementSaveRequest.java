package kr.ac.knue.commonfoundation.degreecompletions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/** Mutable request for atomically saving a degree-completion header and its student details. */
public class DegreeCompletionAchievementSaveRequest {
    private Long achievementId;
    @NotBlank(message = "관리항목 코드는 필수입니다.")
    private String managementItemCode;
    @NotBlank(message = "소속 조직 코드는 필수입니다.")
    private String organizationCode;
    @NotNull(message = "업적발생일은 필수입니다.")
    private LocalDate occurredDate;
    private String achievementDetail;
    private String attachmentRef;
    private String certificationStatus;
    private String changeReason;
    @NotEmpty(message = "지도학생은 한 명 이상 필요합니다.")
    @Valid
    private List<DegreeCompletionStudentRequest> students;

    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long value) { achievementId = value; }
    public String getManagementItemCode() { return managementItemCode; }
    public void setManagementItemCode(String value) { managementItemCode = value; }
    public String getOrganizationCode() { return organizationCode; }
    public void setOrganizationCode(String value) { organizationCode = value; }
    public LocalDate getOccurredDate() { return occurredDate; }
    public void setOccurredDate(LocalDate value) { occurredDate = value; }
    public String getAchievementDetail() { return achievementDetail; }
    public void setAchievementDetail(String value) { achievementDetail = value; }
    public String getAttachmentRef() { return attachmentRef; }
    public void setAttachmentRef(String value) { attachmentRef = value; }
    public String getCertificationStatus() { return certificationStatus; }
    public void setCertificationStatus(String value) { certificationStatus = value; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String value) { changeReason = value; }
    public List<DegreeCompletionStudentRequest> getStudents() { return students; }
    public void setStudents(List<DegreeCompletionStudentRequest> value) { students = value; }
}
