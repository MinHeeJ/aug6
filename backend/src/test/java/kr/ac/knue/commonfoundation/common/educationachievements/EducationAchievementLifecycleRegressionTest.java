package kr.ac.knue.commonfoundation.common.educationachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Proves new types reuse the lifecycle without enabling finalization cancellation. */
class EducationAchievementLifecycleRegressionTest {
    private final EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();

    @ParameterizedTest
    @ValueSource(strings = {
        "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT",
        "LECTURE_EVALUATION", "LECTURE", "STUDENT_GUIDANCE", "DEGREE_COMPLETION"
    })
    void submissionPreservesTypeActorAndTimestamp(String type) {
        var result = policy.transition(request(type, EducationAchievementStatus.DRAFT,
                EducationAchievementStatus.SUBMITTED, "제출"));
        assertThat(result.achievementType()).isEqualTo(type);
        assertThat(result.achievementId()).isEqualTo(81L);
        assertThat(result.previousStatus()).isEqualTo(EducationAchievementStatus.DRAFT);
        assertThat(result.nextStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
        assertThat(result.processedBy()).isEqualTo(101L);
        assertThat(result.processedAt()).isEqualTo(LocalDateTime.parse("2026-04-11T09:30:00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void departmentRejectionRequiresReasonOrOpinion(String type) {
        assertThatThrownBy(() -> policy.transition(request(type, EducationAchievementStatus.SUBMITTED,
                EducationAchievementStatus.DEPARTMENT_REJECTED, null)))
                .isInstanceOf(BusinessValidationException.class).hasMessageContaining("반려 처리");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void certificationRejectionRequiresReasonOrOpinion(String type) {
        assertThatThrownBy(() -> policy.transition(request(type, EducationAchievementStatus.DEPARTMENT_CONFIRMED,
                EducationAchievementStatus.CERTIFICATION_REJECTED, " ")))
                .isInstanceOf(BusinessValidationException.class).hasMessageContaining("반려 처리");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void rejectionWithOpinionPreservesHistory(String type) {
        var result = policy.transition(request(type, EducationAchievementStatus.SUBMITTED,
                EducationAchievementStatus.DEPARTMENT_REJECTED, "  증빙 확인 필요  "));
        assertThat(result.opinion()).isEqualTo("증빙 확인 필요");
        assertThat(result.nextStatus()).isEqualTo(EducationAchievementStatus.DEPARTMENT_REJECTED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void arbitraryCertificationIsRejected(String type) {
        assertThatThrownBy(() -> policy.transition(request(type, EducationAchievementStatus.DRAFT,
                EducationAchievementStatus.CERTIFIED, null)))
                .isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void unapprovedDeleteTransitionIsRejected(String type) {
        assertThatThrownBy(() -> policy.transition(request(type, EducationAchievementStatus.DRAFT,
                EducationAchievementStatus.DELETED, "삭제")))
                .isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void confirmedNewTypeCannotBeReopened(String type) {
        assertThatThrownBy(() -> policy.transition(request(type, EducationAchievementStatus.EVALUATION_CONFIRMED,
                EducationAchievementStatus.CERTIFIED, "확정 취소")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("CONFIRMED_DATA_LOCKED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"LECTURE_EVALUATION", "LECTURE", "STUDENT_GUIDANCE", "DEGREE_COMPLETION"})
    void legacyFinalizationCancellationRemainsAvailable(String type) {
        var result = policy.transition(request(type, EducationAchievementStatus.EVALUATION_CONFIRMED,
                EducationAchievementStatus.CERTIFIED, "기존 확정 취소"));
        assertThat(result.nextStatus()).isEqualTo(EducationAchievementStatus.CERTIFIED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "", "EMPLOYMENT"})
    void unsupportedTypesAreValidationErrors(String type) {
        assertThatThrownBy(() -> policy.transition(request(type, EducationAchievementStatus.DRAFT,
                EducationAchievementStatus.SUBMITTED, null)))
                .isInstanceOf(BusinessValidationException.class);
    }

    private EducationAchievementStatusTransitionRequest request(
            String type,
            EducationAchievementStatus current,
            EducationAchievementStatus next,
            String opinion) {
        return new EducationAchievementStatusTransitionRequest(
                type, 81L, current, next, "TRANSITION", null, opinion,
                101L, LocalDateTime.parse("2026-04-11T09:30:00"));
    }
}
