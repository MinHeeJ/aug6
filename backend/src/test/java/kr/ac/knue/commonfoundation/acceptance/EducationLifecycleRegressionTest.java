package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Exhaustively checks legacy and new types against the unchanged eight-state graph.
 * This is a policy-value test, not evidence that a database history was committed.
 */
class EducationLifecycleRegressionTest {
    private final EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();
    private static final LocalDateTime AT = LocalDateTime.parse("2026-04-10T09:00:00");
    private static final Map<EducationAchievementStatus, Set<EducationAchievementStatus>> GRAPH = Map.of(
            EducationAchievementStatus.DRAFT, Set.of(EducationAchievementStatus.SUBMITTED),
            EducationAchievementStatus.SUBMITTED, Set.of(
                    EducationAchievementStatus.DEPARTMENT_CONFIRMED, EducationAchievementStatus.DEPARTMENT_REJECTED),
            EducationAchievementStatus.DEPARTMENT_REJECTED, Set.of(EducationAchievementStatus.SUBMITTED),
            EducationAchievementStatus.DEPARTMENT_CONFIRMED, Set.of(
                    EducationAchievementStatus.CERTIFIED, EducationAchievementStatus.CERTIFICATION_REJECTED),
            EducationAchievementStatus.CERTIFICATION_REJECTED, Set.of(EducationAchievementStatus.SUBMITTED),
            EducationAchievementStatus.CERTIFIED, Set.of(EducationAchievementStatus.EVALUATION_CONFIRMED),
            EducationAchievementStatus.EVALUATION_CONFIRMED, Set.of(EducationAchievementStatus.CERTIFIED));

    static Stream<String> types() {
        return Stream.of("LECTURE_EVALUATION", "LECTURE", "STUDENT_GUIDANCE", "DEGREE_COMPLETION",
                "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION",
                "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT");
    }

    static Stream<Arguments> transitions() {
        return types().flatMap(type -> Stream.of(EducationAchievementStatus.values()).flatMap(from ->
                Stream.of(EducationAchievementStatus.values()).map(to -> Arguments.of(type, from, to))));
    }

    static Stream<Arguments> rejections() {
        return types().flatMap(type -> Stream.of(
                Arguments.of(type, EducationAchievementStatus.SUBMITTED,
                        EducationAchievementStatus.DEPARTMENT_REJECTED),
                Arguments.of(type, EducationAchievementStatus.DEPARTMENT_CONFIRMED,
                        EducationAchievementStatus.CERTIFICATION_REJECTED)));
    }

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("transitions")
    void everyStatePairObeysGraphAndPreservesHistoryMetadata(
            String type, EducationAchievementStatus from, EducationAchievementStatus to) {
        var request = request(type, from, to, "자료 보완");
        if (!GRAPH.getOrDefault(from, Set.of()).contains(to)) {
            assertThatThrownBy(() -> policy.transition(request)).isInstanceOf(ConflictException.class);
            return;
        }
        var history = policy.transition(request);
        assertThat(history.achievementType()).isEqualTo(type);
        assertThat(history.achievementId()).isEqualTo(42L);
        assertThat(history.previousStatus()).isEqualTo(from);
        assertThat(history.nextStatus()).isEqualTo(to);
        assertThat(history.processedBy()).isEqualTo(101L);
        assertThat(history.processedAt()).isEqualTo(AT);
        assertThat(history.actionType()).isEqualTo("TRANSITION");
        assertThat(history.opinion()).isEqualTo("자료 보완");
    }

    @ParameterizedTest
    @MethodSource("rejections")
    void eachRejectionRequiresReasonOrOpinion(
            String type, EducationAchievementStatus from, EducationAchievementStatus to) {
        assertThatThrownBy(() -> policy.transition(request(type, from, to, " ")))
                .isInstanceOf(BusinessValidationException.class);
    }

    private EducationAchievementStatusTransitionRequest request(
            String type, EducationAchievementStatus from, EducationAchievementStatus to, String opinion) {
        return new EducationAchievementStatusTransitionRequest(
                type, 42L, from, to, "TRANSITION", null, opinion, 101L, AT);
    }
}
