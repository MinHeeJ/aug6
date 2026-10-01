package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

class EducationAchievementStatusTransitionTest {
    @Test
    void permittedTransitionBuildsCompleteStatusHistoryPayload() {
        EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();
        LocalDateTime processedAt = LocalDateTime.parse("2026-04-10T10:15:00");

        EducationAchievementStatusTransition transition = policy.transition(
                "SUBMITTED",
                "DEPARTMENT_REJECT",
                "INCOMPLETE_EVIDENCE",
                "증빙자료를 보완하세요.",
                101L,
                processedAt
        );

        assertThat(transition.previousStatus()).isEqualTo("SUBMITTED");
        assertThat(transition.nextStatus()).isEqualTo("DEPARTMENT_REJECTED");
        assertThat(transition.actionType()).isEqualTo("DEPARTMENT_REJECT");
        assertThat(transition.reasonCode()).isEqualTo("INCOMPLETE_EVIDENCE");
        assertThat(transition.opinion()).isEqualTo("증빙자료를 보완하세요.");
        assertThat(transition.processedBy()).isEqualTo(101L);
        assertThat(transition.processedAt()).isEqualTo(processedAt);
    }

    @Test
    void rejectionWithoutReasonOrOpinionIsRejectedBeforeHistoryPayloadExists() {
        EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();

        assertThatThrownBy(() -> policy.transition(
                "DEPARTMENT_CONFIRMED",
                "CERTIFICATION_REJECT",
                null,
                " ",
                101L,
                LocalDateTime.parse("2026-04-10T10:15:00")
        )).isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("사유 또는 의견");
    }

    @Test
    void invalidStateTransitionIsRejectedWithoutCreatingHistoryPayload() {
        EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();

        assertThatThrownBy(() -> policy.transition(
                "DRAFT",
                "CERTIFY",
                null,
                null,
                101L,
                LocalDateTime.parse("2026-04-10T10:15:00")
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("INVALID_STATE_TRANSITION");
    }
}
