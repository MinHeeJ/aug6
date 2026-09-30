package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.List;

/** Mutable MyBatis materialization for a degree-completion header and its guided-student details. */
public class DegreeCompletionAchievementData {
    private Long achievementId;
    private String managementNo;
    private String evaluationYear;
    private String organizationCode;
    private Long teacherUserId;
    private String teacherName;
    private String managementItemCode;
    private LocalDate occurredDate;
    private EducationAchievementStatus certificationStatus;
    private String achievementDetail;
    private Integer attachmentCount;
    private boolean occurrenceDateWarning;
    private List<DegreeCompletionStudentRow> students;

    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long value) { achievementId = value; }
    public String getManagementNo() { return managementNo; }
    public void setManagementNo(String value) { managementNo = value; }
    public String getEvaluationYear() { return evaluationYear; }
    public void setEvaluationYear(String value) { evaluationYear = value; }
    public String getOrganizationCode() { return organizationCode; }
    public void setOrganizationCode(String value) { organizationCode = value; }
    public Long getTeacherUserId() { return teacherUserId; }
    public void setTeacherUserId(Long value) { teacherUserId = value; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String value) { teacherName = value; }
    public String getManagementItemCode() { return managementItemCode; }
    public void setManagementItemCode(String value) { managementItemCode = value; }
    public LocalDate getOccurredDate() { return occurredDate; }
    public void setOccurredDate(LocalDate value) { occurredDate = value; }
    public EducationAchievementStatus getCertificationStatus() { return certificationStatus; }
    public void setCertificationStatus(EducationAchievementStatus value) { certificationStatus = value; }
    public String getAchievementDetail() { return achievementDetail; }
    public void setAchievementDetail(String value) { achievementDetail = value; }
    public Integer getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(Integer value) { attachmentCount = value; }
    public boolean isOccurrenceDateWarning() { return occurrenceDateWarning; }
    public void setOccurrenceDateWarning(boolean value) { occurrenceDateWarning = value; }
    public List<DegreeCompletionStudentRow> getStudents() { return students; }
    public void setStudents(List<DegreeCompletionStudentRow> value) { students = value; }
}
