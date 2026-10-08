package kr.ac.knue.commonfoundation.common.education;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** New physical header types reuse, rather than widen, the existing transition graph. */
class EducationAchievementLifecycleExtensionTest {
    private final EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();
    private final LocalDateTime processedAt = LocalDateTime.parse("2026-04-11T09:30:00");

    @ParameterizedTest
    @ValueSource(strings = {
            "LECTURE_EVALUATION", "LECTURE", "STUDENT_GUIDANCE", "DEGREE_COMPLETION",
            "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION",
            "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void acceptsBothExistingAndNewTypesWithoutChangingHistoryMetadata(String type) {
        EducationAchievementStatusHistory history = policy.transition(request(
                type, EducationAchievementStatus.DRAFT, EducationAchievementStatus.SUBMITTED, null));
        assertThat(history.achievementType()).isEqualTo(type);
        assertThat(history.achievementId()).isEqualTo(81L);
        assertThat(history.previousStatus()).isEqualTo(EducationAchievementStatus.DRAFT);
        assertThat(history.nextStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
        assertThat(history.processedBy()).isEqualTo(2L);
        assertThat(history.processedAt()).isEqualTo(processedAt);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION",
            "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void rejectsShortcutOutsideUnchangedGraph(String type) {
        assertThatThrownBy(() -> policy.transition(request(
                type, EducationAchievementStatus.DRAFT, EducationAchievementStatus.CERTIFIED, null)))
                .isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION",
            "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void departmentRejectionStillRequiresReasonOrOpinion(String type) {
        assertThatThrownBy(() -> policy.transition(request(
                type, EducationAchievementStatus.SUBMITTED, EducationAchievementStatus.DEPARTMENT_REJECTED, " ")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("반려 처리");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION",
            "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void certificationRejectionStillRequiresReasonOrOpinion(String type) {
        assertThatThrownBy(() -> policy.transition(request(
                type, EducationAchievementStatus.DEPARTMENT_CONFIRMED,
                EducationAchievementStatus.CERTIFICATION_REJECTED, null)))
                .isInstanceOf(BusinessValidationException.class);
        assertThat(policy.transition(request(
                type, EducationAchievementStatus.DEPARTMENT_CONFIRMED,
                EducationAchievementStatus.CERTIFICATION_REJECTED, " 자료 보완 ")).opinion())
                .isEqualTo("자료 보완");
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "EMPLOYMENT_RATE_BULK", " "})
    void rejectsTypesWithoutSchemaContract(String type) {
        assertThatThrownBy(() -> policy.transition(request(
                type, EducationAchievementStatus.DRAFT, EducationAchievementStatus.SUBMITTED, null)))
                .isInstanceOf(BusinessValidationException.class);
    }

    private EducationAchievementStatusTransitionRequest request(
            String type,
            EducationAchievementStatus current,
            EducationAchievementStatus next,
            String opinion) {
        return new EducationAchievementStatusTransitionRequest(
                type, 81L, current, next, "TRANSITION", null, opinion, 2L, processedAt);
    }
}
