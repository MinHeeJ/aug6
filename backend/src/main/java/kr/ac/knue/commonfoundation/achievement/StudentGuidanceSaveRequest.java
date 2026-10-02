package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.List;

/** Binds the FR-027 student-guidance header and its required student detail rows. */
public class StudentGuidanceSaveRequest {
    private Long achievementId;
    private String managementItemCode;
    private LocalDate guidanceStartDate;
    private LocalDate guidanceEndDate;
    private String attachmentRef;
    private List<Object> students;

    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long achievementId) { this.achievementId = achievementId; }
    public String getManagementItemCode() { return managementItemCode; }
    public void setManagementItemCode(String managementItemCode) { this.managementItemCode = managementItemCode; }
    public LocalDate getGuidanceStartDate() { return guidanceStartDate; }
    public void setGuidanceStartDate(LocalDate guidanceStartDate) { this.guidanceStartDate = guidanceStartDate; }
    public LocalDate getGuidanceEndDate() { return guidanceEndDate; }
    public void setGuidanceEndDate(LocalDate guidanceEndDate) { this.guidanceEndDate = guidanceEndDate; }
    public String getAttachmentRef() { return attachmentRef; }
    public void setAttachmentRef(String attachmentRef) { this.attachmentRef = attachmentRef; }
    public List<Object> getStudents() { return students; }
    public void setStudents(List<Object> students) { this.students = students; }
}
