package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;

/** Binds the degree-completion header and its required 지도학생 repeated detail rows. */
public class DegreeCompletionSaveRequest {
    private Long achievementId;
    private Long teacherUserId;
    private String organizationCode;
    private String evaluationYear;
    private String managementItemCode;
    private JsonNode achievementDetail;
    private String attachmentRef;
    private String actionType;
    private String reasonCode;
    private String opinion;
    private List<StudentRequest> students;

    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long achievementId) { this.achievementId = achievementId; }
    public Long getTeacherUserId() { return teacherUserId; }
    public void setTeacherUserId(Long teacherUserId) { this.teacherUserId = teacherUserId; }
    public String getOrganizationCode() { return organizationCode; }
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }
    public String getEvaluationYear() { return evaluationYear; }
    public void setEvaluationYear(String evaluationYear) { this.evaluationYear = evaluationYear; }
    public String getManagementItemCode() { return managementItemCode; }
    public void setManagementItemCode(String managementItemCode) { this.managementItemCode = managementItemCode; }
    public JsonNode getAchievementDetail() { return achievementDetail; }
    public void setAchievementDetail(JsonNode achievementDetail) { this.achievementDetail = achievementDetail; }
    public String getAttachmentRef() { return attachmentRef; }
    public void setAttachmentRef(String attachmentRef) { this.attachmentRef = attachmentRef; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
    public List<StudentRequest> getStudents() { return students; }
    public void setStudents(List<StudentRequest> students) { this.students = students; }

    /** Binds one mapped degree recipient; all fields are required by FR-028. */
    public static class StudentRequest {
        private String degreeType;
        private String studentName;
        private String thesisTitle;
        private LocalDate degreeAwardedDate;

        public String getDegreeType() { return degreeType; }
        public void setDegreeType(String degreeType) { this.degreeType = degreeType; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getThesisTitle() { return thesisTitle; }
        public void setThesisTitle(String thesisTitle) { this.thesisTitle = thesisTitle; }
        public LocalDate getDegreeAwardedDate() { return degreeAwardedDate; }
        public void setDegreeAwardedDate(LocalDate degreeAwardedDate) { this.degreeAwardedDate = degreeAwardedDate; }
    }
}
