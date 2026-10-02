package kr.ac.knue.commonfoundation.basic83;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * Regression coverage for the shared BASIC-83 lifecycle and persistence
 * guarantees that are not owned by an individual achievement endpoint.
 */
class Basic83RegressionTest {
    @Test
    void acceptedTransitionProducesCompleteHistoryAndRejectsBypassedLifecycle() {
        EducationAchievementStatusTransitionPolicy policy =
                new EducationAchievementStatusTransitionPolicy();
        LocalDateTime processedAt = LocalDateTime.of(2026, 10, 2, 10, 15);

        EducationAchievementStatusHistory history = policy.transition(
                new EducationAchievementStatusTransitionRequest(
                        "EMPLOYMENT_RATE_IMPROVEMENT",
                        83L,
                        EducationAchievementStatus.SUBMITTED,
                        EducationAchievementStatus.DEPARTMENT_REJECTED,
                        "REJECT",
                        "EVIDENCE_MISSING",
                        "증빙자료가 부족합니다.",
                        7L,
                        processedAt));

        assertThat(history.achievementType()).isEqualTo("EMPLOYMENT_RATE_IMPROVEMENT");
        assertThat(history.achievementId()).isEqualTo(83L);
        assertThat(history.previousStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
        assertThat(history.nextStatus()).isEqualTo(EducationAchievementStatus.DEPARTMENT_REJECTED);
        assertThat(history.processedBy()).isEqualTo(7L);
        assertThat(history.processedAt()).isEqualTo(processedAt);
        assertThat(history.reasonCode()).isEqualTo("EVIDENCE_MISSING");

        assertThatThrownBy(() -> policy.transition(
                new EducationAchievementStatusTransitionRequest(
                        "EMPLOYMENT_RATE_IMPROVEMENT",
                        83L,
                        EducationAchievementStatus.DRAFT,
                        EducationAchievementStatus.CERTIFIED,
                        "CERTIFY",
                        null,
                        null,
                        7L,
                        processedAt)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("허용되지 않은 교육영역 실적 상태 전이입니다.");
    }

    @Test
    void migrationRetainsAuditAndSoftDeleteContractForBasic83SourceRows() throws Exception {
        String migration = StreamUtils.copyToString(
                new ClassPathResource("db/migration/V65__basic83_education_achievement_scope.sql")
                        .getInputStream(),
                StandardCharsets.UTF_8);
        String openApi = StreamUtils.copyToString(
                new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        assertThat(migration)
                .contains("deleted_yn char(1) NOT NULL DEFAULT 'N'")
                .contains("CHECK (deleted_yn IN ('Y', 'N'))")
                .contains("created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP")
                .contains("updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP")
                .contains("created_by bigint")
                .contains("updated_by bigint")
                .contains("education_achievement_status_histories")
                .contains("EMPLOYMENT_RATE_IMPROVEMENT");
        assertThat(openApi).contains("/api/business/employment-rate-improvements");
    }
}
