package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Guards the existing lifecycle reused by the new education slices, without requiring a database. */
class EducationLifecycleRegressionTest {
    private final EducationAchievementStatusTransitionPolicy policy =
            new EducationAchievementStatusTransitionPolicy();
    private final LocalDateTime processedAt = LocalDateTime.of(2025, 4, 10, 9, 30);

    @ParameterizedTest
    @CsvSource({
        "DRAFT,SUBMITTED",
        "SUBMITTED,DEPARTMENT_CONFIRMED",
        "SUBMITTED,DEPARTMENT_REJECTED",
        "DEPARTMENT_REJECTED,SUBMITTED",
        "DEPARTMENT_CONFIRMED,CERTIFIED",
        "DEPARTMENT_CONFIRMED,CERTIFICATION_REJECTED",
        "CERTIFICATION_REJECTED,SUBMITTED",
        "CERTIFIED,EVALUATION_CONFIRMED",
        "EVALUATION_CONFIRMED,CERTIFIED"
    })
    void permittedTransitionsPreserveHistoryIdentityActorTimeAndReason(
            EducationAchievementStatus previous,
            EducationAchievementStatus next) {
        for (String type : List.of("LECTURE_EVALUATION", "LECTURE", "STUDENT_GUIDANCE", "DEGREE_COMPLETION")) {
            EducationAchievementStatusHistory history = policy.transition(request(type, previous, next, " REJECT "));
            assertThat(history.achievementType()).isEqualTo(type);
            assertThat(history.achievementId()).isEqualTo(101L);
            assertThat(history.previousStatus()).isEqualTo(previous);
            assertThat(history.nextStatus()).isEqualTo(next);
            assertThat(history.processedBy()).isEqualTo(201L);
            assertThat(history.processedAt()).isEqualTo(processedAt);
            assertThat(history.actionType()).isEqualTo("WORKFLOW");
            assertThat(history.reasonCode()).isEqualTo("REJECT");
        }
    }

    @ParameterizedTest
    @CsvSource({
        "DRAFT,CERTIFIED",
        "DRAFT,EVALUATION_CONFIRMED",
        "SUBMITTED,CERTIFIED",
        "CERTIFIED,DRAFT",
        "EVALUATION_CONFIRMED,DRAFT",
        "DELETED,SUBMITTED",
        "DRAFT,DELETED"
    })
    void illegalTransitionsCannotProduceHistory(
            EducationAchievementStatus previous,
            EducationAchievementStatus next) {
        assertThatThrownBy(() -> policy.transition(request("LECTURE", previous, next, "reason")))
                .isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest
    @CsvSource({"SUBMITTED,DEPARTMENT_REJECTED", "DEPARTMENT_CONFIRMED,CERTIFICATION_REJECTED"})
    void rejectionRequiresReasonOrOpinion(
            EducationAchievementStatus previous,
            EducationAchievementStatus next) {
        assertThatThrownBy(() -> policy.transition(request("LECTURE", previous, next, "  ")))
                .isInstanceOf(BusinessValidationException.class);
        EducationAchievementStatusHistory history = policy.transition(new EducationAchievementStatusTransitionRequest(
                "LECTURE", 101L, previous, next, "WORKFLOW", null, " 수정 요청 ", 201L, processedAt));
        assertThat(history.reasonCode()).isNull();
        assertThat(history.opinion()).isEqualTo("수정 요청");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void missingHistoryIdentityIsRejected(long id) {
        assertThatThrownBy(() -> policy.transition(new EducationAchievementStatusTransitionRequest(
                "LECTURE", id, EducationAchievementStatus.DRAFT, EducationAchievementStatus.SUBMITTED,
                "WORKFLOW", null, null, 201L, processedAt)))
                .isInstanceOf(BusinessValidationException.class);
    }

    private EducationAchievementStatusTransitionRequest request(
            String type,
            EducationAchievementStatus previous,
            EducationAchievementStatus next,
            String reason) {
        return new EducationAchievementStatusTransitionRequest(
                type, 101L, previous, next, " WORKFLOW ", reason, null, 201L, processedAt);
    }
}
