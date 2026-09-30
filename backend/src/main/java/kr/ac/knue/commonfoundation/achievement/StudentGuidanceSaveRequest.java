package kr.ac.knue.commonfoundation.achievement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
/** Request for an individual student-guidance achievement and its required student details. */
public record StudentGuidanceSaveRequest(@NotBlank String managementItemCode, @NotNull LocalDate guidanceStartDate, @NotNull LocalDate guidanceEndDate, @NotEmpty List<@Valid StudentGuidanceStudentRequest> students, String changeReason) {}
