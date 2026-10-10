package kr.ac.knue.commonfoundation.basic81;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifies shared lifecycle admission without changing legacy transition or rejection rules. */
class EducationAchievementStatusPolicyTest {
    private final EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();

    @ParameterizedTest
    @ValueSource(strings = {
        "LECTURE_EVALUATION", "LECTURE", "STUDENT_GUIDANCE", "DEGREE_COMPLETION",
        "COURSE_OPERATION", "EMPLOYMENT_RATE_IMPROVEMENT", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void submissionRetainsTypeIdentityAndAuditActor(String type) {
        LocalDateTime processedAt = LocalDateTime.parse("2026-04-10T12:00:00");
        EducationAchievementStatusHistory history = policy.transition(new EducationAchievementStatusTransitionRequest(
                type,
                82L,
                EducationAchievementStatus.DRAFT,
                EducationAchievementStatus.SUBMITTED,
                "SUBMIT",
                null,
                null,
                101L,
                processedAt));

        assertThat(history.achievementType()).isEqualTo(type);
        assertThat(history.achievementId()).isEqualTo(82L);
        assertThat(history.previousStatus()).isEqualTo(EducationAchievementStatus.DRAFT);
        assertThat(history.nextStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
        assertThat(history.processedBy()).isEqualTo(101L);
        assertThat(history.processedAt()).isEqualTo(processedAt);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "COURSE_OPERATION", "EMPLOYMENT_RATE_IMPROVEMENT", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void newTypesCannotSkipSubmissionAndCertification(String type) {
        assertThatThrownBy(() -> policy.transition(request(
                type, EducationAchievementStatus.DRAFT, EducationAchievementStatus.EVALUATION_CONFIRMED, null)))
                .isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "COURSE_OPERATION", "EMPLOYMENT_RATE_IMPROVEMENT", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void rejectionStillRequiresReasonOrOpinion(String type) {
        assertThatThrownBy(() -> policy.transition(request(
                type, EducationAchievementStatus.SUBMITTED, EducationAchievementStatus.DEPARTMENT_REJECTED, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("사유 또는 의견");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "COURSE_OPERATION", "EMPLOYMENT_RATE_IMPROVEMENT", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void rejectionWithOpinionRetainsTheHistoryContext(String type) {
        EducationAchievementStatusHistory history = policy.transition(request(
                type, EducationAchievementStatus.SUBMITTED, EducationAchievementStatus.DEPARTMENT_REJECTED,
                "  증빙 보완 필요  "));
        assertThat(history.opinion()).isEqualTo("증빙 보완 필요");
        assertThat(history.nextStatus()).isEqualTo(EducationAchievementStatus.DEPARTMENT_REJECTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "COURSE_OPERATION", "EMPLOYMENT_RATE_IMPROVEMENT", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void rejectedAchievementMayBeResubmitted(String type) {
        EducationAchievementStatusHistory history = policy.transition(request(
                type, EducationAchievementStatus.CERTIFICATION_REJECTED, EducationAchievementStatus.SUBMITTED, null));
        assertThat(history.previousStatus()).isEqualTo(EducationAchievementStatus.CERTIFICATION_REJECTED);
        assertThat(history.nextStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
    }

    private EducationAchievementStatusTransitionRequest request(
            String type,
            EducationAchievementStatus current,
            EducationAchievementStatus next,
            String opinion) {
        return new EducationAchievementStatusTransitionRequest(
                type, 82L, current, next, "TRANSITION", null, opinion, 101L,
                LocalDateTime.parse("2026-04-10T12:00:00"));
    }
}
