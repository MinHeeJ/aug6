package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/** Internal mutable MyBatis materialization for a persisted lecture achievement. */
public class LectureAchievementData {
    private Long achievementId; private String managementNo; private String evaluationYear; private String organizationCode;
    private Long teacherUserId; private String teacherName; private String managementItemCode; private LocalDate occurredDate;
    private EducationAchievementStatus certificationStatus; private String achievementDetail; private Integer attachmentCount; private boolean occurrenceDateWarning;
    public Long getAchievementId(){return achievementId;} public void setAchievementId(Long v){achievementId=v;} public String getManagementNo(){return managementNo;} public void setManagementNo(String v){managementNo=v;} public String getEvaluationYear(){return evaluationYear;} public void setEvaluationYear(String v){evaluationYear=v;} public String getOrganizationCode(){return organizationCode;} public void setOrganizationCode(String v){organizationCode=v;} public Long getTeacherUserId(){return teacherUserId;} public void setTeacherUserId(Long v){teacherUserId=v;} public String getTeacherName(){return teacherName;} public void setTeacherName(String v){teacherName=v;} public String getManagementItemCode(){return managementItemCode;} public void setManagementItemCode(String v){managementItemCode=v;} public LocalDate getOccurredDate(){return occurredDate;} public void setOccurredDate(LocalDate v){occurredDate=v;} public EducationAchievementStatus getCertificationStatus(){return certificationStatus;} public void setCertificationStatus(EducationAchievementStatus v){certificationStatus=v;} public String getAchievementDetail(){return achievementDetail;} public void setAchievementDetail(String v){achievementDetail=v;} public Integer getAttachmentCount(){return attachmentCount;} public void setAttachmentCount(Integer v){attachmentCount=v;} public boolean isOccurrenceDateWarning(){return occurrenceDateWarning;} public void setOccurrenceDateWarning(boolean v){occurrenceDateWarning=v;}
}
