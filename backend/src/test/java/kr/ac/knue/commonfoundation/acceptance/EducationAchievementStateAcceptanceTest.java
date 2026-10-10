package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Exhaustive legacy/new lifecycle regression; this validates history values, not persisted history rows. */
class EducationAchievementStateAcceptanceTest {
    private final EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();
    private static final List<String> TYPES = List.of("LECTURE_EVALUATION", "LECTURE", "STUDENT_GUIDANCE",
            "DEGREE_COMPLETION", "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION",
            "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT");
    private static final LocalDateTime PROCESSED_AT = LocalDateTime.parse("2026-04-10T12:00:00");
    private static final Map<String, Set<String>> EDGES = Map.of(
            "DRAFT", Set.of("SUBMITTED"),
            "SUBMITTED", Set.of("DEPARTMENT_CONFIRMED", "DEPARTMENT_REJECTED"),
            "DEPARTMENT_REJECTED", Set.of("SUBMITTED"),
            "DEPARTMENT_CONFIRMED", Set.of("CERTIFIED", "CERTIFICATION_REJECTED"),
            "CERTIFICATION_REJECTED", Set.of("SUBMITTED"),
            "CERTIFIED", Set.of("EVALUATION_CONFIRMED"),
            "EVALUATION_CONFIRMED", Set.of("CERTIFIED"));

    private static Stream<Arguments> statePairs() {
        return TYPES.stream().flatMap(type -> Stream.of(EducationAchievementStatus.values())
                .flatMap(before -> Stream.of(EducationAchievementStatus.values())
                        .map(after -> Arguments.of(type, before, after))));
    }

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("statePairs")
    void onlyApprovedEdgesProduceCompleteImmutableHistory(
            String type, EducationAchievementStatus before, EducationAchievementStatus after) {
        var request = new EducationAchievementStatusTransitionRequest(type, 82L, before, after,
                "TRANSITION", "  EVIDENCE_REQUIRED  ", "  증빙 확인  ", 101L, PROCESSED_AT);
        if (!EDGES.getOrDefault(before.name(), Set.of()).contains(after.name())) {
            assertThatThrownBy(() -> policy.transition(request)).isInstanceOf(ConflictException.class);
            return;
        }
        EducationAchievementStatusHistory history = policy.transition(request);
        assertThat(history).isEqualTo(new EducationAchievementStatusHistory(
                type, 82L, before, after, "TRANSITION", "EVIDENCE_REQUIRED", "증빙 확인", 101L, PROCESSED_AT));
    }

    private static Stream<Arguments> rejections() {
        return TYPES.stream().flatMap(type -> Stream.of(
                Arguments.of(type, EducationAchievementStatus.SUBMITTED,
                        EducationAchievementStatus.DEPARTMENT_REJECTED),
                Arguments.of(type, EducationAchievementStatus.DEPARTMENT_CONFIRMED,
                        EducationAchievementStatus.CERTIFICATION_REJECTED)));
    }

    @ParameterizedTest
    @MethodSource("rejections")
    void rejectionCannotLoseReasonAndOpinion(
            String type, EducationAchievementStatus before, EducationAchievementStatus after) {
        var request = new EducationAchievementStatusTransitionRequest(type, 82L, before, after,
                "REJECT", " ", " ", 101L, PROCESSED_AT);
        assertThatThrownBy(() -> policy.transition(request))
                .isInstanceOfSatisfying(BusinessValidationException.class, error -> {
                    assertThat(error.fields()).anyMatch(field -> field.field().equals("reasonCode"));
                });
    }
}
