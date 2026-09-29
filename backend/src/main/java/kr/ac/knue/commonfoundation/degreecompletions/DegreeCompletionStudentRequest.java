package kr.ac.knue.commonfoundation.degreecompletions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Mutable student-detail payload belonging to one degree-completion achievement. */
public class DegreeCompletionStudentRequest {
    @NotBlank(message = "학위구분은 필수입니다.")
    private String degreeType;
    @NotBlank(message = "학생명은 필수입니다.")
    private String studentName;
    @NotBlank(message = "논문명은 필수입니다.")
    private String thesisTitle;
    @NotNull(message = "학위수여일은 필수입니다.")
    private LocalDate degreeAwardedDate;

    public String getDegreeType() { return degreeType; }
    public void setDegreeType(String value) { degreeType = value; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String value) { studentName = value; }
    public String getThesisTitle() { return thesisTitle; }
    public void setThesisTitle(String value) { thesisTitle = value; }
    public LocalDate getDegreeAwardedDate() { return degreeAwardedDate; }
    public void setDegreeAwardedDate(LocalDate value) { degreeAwardedDate = value; }
}
