package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** Mutable generated-key carrier; the detail is inserted only after this header has its ID. */
public class EmploymentRateImprovementWrite {
    private Long achievementId;
    private final EmploymentRateImprovementRequest request;
    private final Long teacherUserId;
    private final String evaluationYear;
    private final Long actorId;
    private final String requestId;
    private final String detail;
    private final String attachments;

    public EmploymentRateImprovementWrite(
            Long achievementId, EmploymentRateImprovementRequest request, Long teacherUserId,
            String evaluationYear, Long actorId, String requestId, String detail, String attachments) {
        this.achievementId = achievementId;
        this.request = request;
        this.teacherUserId = teacherUserId;
        this.evaluationYear = evaluationYear;
        this.actorId = actorId;
        this.requestId = requestId;
        this.detail = detail;
        this.attachments = attachments;
    }

    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long achievementId) { this.achievementId = achievementId; }
    public EmploymentRateImprovementRequest getRequest() { return request; }
    public Long getTeacherUserId() { return teacherUserId; }
    public String getEvaluationYear() { return evaluationYear; }
    public Long getActorId() { return actorId; }
    public String getRequestId() { return requestId; }
    public String getDetail() { return detail; }
    public String getAttachments() { return attachments; }
}
