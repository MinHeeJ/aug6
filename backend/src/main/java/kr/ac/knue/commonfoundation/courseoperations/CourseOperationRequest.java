package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;

/** Approved wire fields; PUT identity is supplied exclusively by its path. */
public record CourseOperationRequest(
        @NotBlank @Size(max = 50) String managementItemCode,
        @NotNull LocalDate achievementDate,
        @NotBlank String performanceDetails,
        @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @Size(max = 300) String attachmentRef,
        List<@NotBlank @Size(max = 200) String> attachmentIds,
        EducationAchievementStatus achievementStatus) {
}
