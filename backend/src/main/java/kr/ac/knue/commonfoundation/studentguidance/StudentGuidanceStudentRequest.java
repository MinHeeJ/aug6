package kr.ac.knue.commonfoundation.studentguidance;

/** A guided student detail that is persisted with its student-guidance achievement header. */
public class StudentGuidanceStudentRequest {
    private String studentNo;
    private String studentName;

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
}
