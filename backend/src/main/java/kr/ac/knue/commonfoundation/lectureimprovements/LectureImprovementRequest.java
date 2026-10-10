package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;

/** Typed lecture improvement input; PUT identity comes exclusively from its path. */
public record LectureImprovementRequest(
        @NotBlank String managementItemCode,
        @NotNull LocalDate achievementDate,
        @Pattern(regexp = "[0-9]{4}") String evaluationYear,
        @JsonAlias("achievementContent") @NotBlank @Size(max = 20000) String performanceContent,
        @NotBlank @Pattern(regexp = "[0-9]{4}") String academicYear,
        @NotBlank String semester,
        @Size(max = 300) String attachmentRef,
        EducationAchievementStatus achievementStatus) {
}
