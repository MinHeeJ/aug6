package kr.ac.knue.commonfoundation.faculty.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EducationAchievementStatusTransitionTest {
    private final EducationAchievementGuardChain guardChain = new EducationAchievementGuardChain();

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void transitionAllowsOnlyDefinedEducationAchievementStatusPaths(
            String fromStatus,
            String nextStatus,
            String expectedActionType
    ) {
        EducationAchievementGuardChain.EducationAchievementStatusHistory history = guardChain.transition(
                "LECTURE_EVALUATION",
                810001L,
                fromStatus,
                nextStatus,
                "B77-STATE",
                rejectionOpinion(nextStatus),
                1L
        );

        assertThat(history.previousStatus()).isEqualTo(fromStatus);
        assertThat(history.nextStatus()).isEqualTo(nextStatus);
        assertThat(history.actionType()).isEqualTo(expectedActionType);
        assertThat(history.processedBy()).isEqualTo(1L);
        assertThat(history.processedAt()).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void transitionRejectsPathsOutsideTheEducationAchievementStateGraph(
            String fromStatus,
            String nextStatus
    ) {
        assertThatThrownBy(() -> guardChain.transition(
                "LECTURE_EVALUATION",
                810001L,
                fromStatus,
                nextStatus,
                "B77-STATE",
                null,
                1L
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INVALID_STATE_TRANSITION");
    }

    @ParameterizedTest
    @MethodSource("rejectionStatuses")
    void transitionRequiresOpinionForRejectedStatuses(String nextStatus) {
        assertThatThrownBy(() -> guardChain.transition(
                "LECTURE_EVALUATION",
                810001L,
                previousStatusFor(nextStatus),
                nextStatus,
                "B77-REJECTION",
                " ",
                1L
        ))
                .isInstanceOf(BusinessValidationException.class)
                .extracting(exception -> ((BusinessValidationException) exception).fields().get(0).field())
                .isEqualTo("opinion");
    }

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void transitionCreatesHistoryWithReasonAndTrimmedOpinion(
            String fromStatus,
            String nextStatus,
            String expectedActionType
    ) {
        EducationAchievementGuardChain.EducationAchievementStatusHistory history = guardChain.transition(
                "STUDENT_GUIDANCE",
                810002L,
                fromStatus,
                nextStatus,
                "  B77-HISTORY  ",
                "  처리 의견  ",
                7L
        );

        assertThat(history.achievementType()).isEqualTo("STUDENT_GUIDANCE");
        assertThat(history.achievementId()).isEqualTo(810002L);
        assertThat(history.reasonCode()).isEqualTo("B77-HISTORY");
        assertThat(history.opinion()).isEqualTo("처리 의견");
        assertThat(history.actionType()).isEqualTo(expectedActionType);
        assertThat(history.processedBy()).isEqualTo(7L);
        assertThat(history.processedAt()).isNotNull();
    }

    @org.junit.jupiter.api.Test
    void validateWriteRejectsUnauthorizedScopeAndLockedPreconditionsBeforePersistence() {
        CurrentUser teacher = new CurrentUser(2L, "teacher", "E1001", "교원", List.of("R01"), List.of());

        assertThatThrownBy(() -> guardChain.validateWrite(
                teacher,
                3L,
                true,
                true,
                false,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        )).isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> guardChain.validateWrite(
                teacher,
                2L,
                true,
                false,
                false,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PERIOD_NOT_ACTIVE");

        assertThatThrownBy(() -> guardChain.validateWrite(
                teacher,
                2L,
                true,
                true,
                true,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");
    }

    @org.junit.jupiter.api.Test
    void validateWriteAllowsOccurredDateOutsideEvaluationPeriodWithWarning() {
        CurrentUser teacher = new CurrentUser(2L, "teacher", "E1001", "교원", List.of("R01"), List.of());

        EducationAchievementGuardChain.EducationAchievementGuardResult result = guardChain.validateWrite(
                teacher,
                2L,
                true,
                true,
                false,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );

        assertThat(result.occurredDateWarning()).isTrue();
        assertThat(result.evaluatedAt()).isNotNull();
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of("DRAFTING", "SUBMITTED", "SUBMIT"),
                Arguments.of("SUBMITTED", "DEPARTMENT_CONFIRMED", "DEPARTMENT_CONFIRM"),
                Arguments.of("SUBMITTED", "DEPARTMENT_REJECTED", "DEPARTMENT_REJECT"),
                Arguments.of("DEPARTMENT_REJECTED", "SUBMITTED", "SUBMIT"),
                Arguments.of("DEPARTMENT_CONFIRMED", "CERTIFIED", "CERTIFY"),
                Arguments.of("DEPARTMENT_CONFIRMED", "CERTIFICATION_RETURNED", "CERTIFICATION_RETURN"),
                Arguments.of("CERTIFICATION_RETURNED", "SUBMITTED", "SUBMIT"),
                Arguments.of("CERTIFIED", "EVALUATION_CONFIRMED", "EVALUATION_CONFIRM"),
                Arguments.of("EVALUATION_CONFIRMED", "CERTIFIED", "EVALUATION_CANCEL")
        );
    }

    private static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of("DRAFTING", "CERTIFIED"),
                Arguments.of("SUBMITTED", "EVALUATION_CONFIRMED"),
                Arguments.of("DELETED", "SUBMITTED")
        );
    }

    private static Stream<String> rejectionStatuses() {
        return Stream.of("DEPARTMENT_REJECTED", "CERTIFICATION_RETURNED");
    }

    private static String previousStatusFor(String nextStatus) {
        return "DEPARTMENT_REJECTED".equals(nextStatus) ? "SUBMITTED" : "DEPARTMENT_CONFIRMED";
    }

    private static String rejectionOpinion(String nextStatus) {
        return "DEPARTMENT_REJECTED".equals(nextStatus) || "CERTIFICATION_RETURNED".equals(nextStatus)
                ? "반려 사유"
                : null;
    }
}
