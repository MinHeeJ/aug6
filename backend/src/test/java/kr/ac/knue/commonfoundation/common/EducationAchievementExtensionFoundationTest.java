package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.education.EducationAchievementFoundationContract;
import kr.ac.knue.commonfoundation.common.education.EducationAchievementTypes;
import kr.ac.knue.commonfoundation.common.education.EducationAchievementValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Behavior tests for type extension, lifecycle preservation, routes and API/SQL value conversion. */
class EducationAchievementExtensionFoundationTest {
    private final EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();

    @ParameterizedTest
    @ValueSource(strings = {
        "LECTURE_EVALUATION", "LECTURE", "STUDENT_GUIDANCE", "DEGREE_COMPLETION",
        "EMPLOYMENT_RATE_IMPROVEMENT", "COURSE_OPERATION", "LECTURE_IMPROVEMENT", "EMPLOYMENT_RATE_ACHIEVEMENT"
    })
    void legacyAndNewTypesProduceCompleteHistory(String type) {
        var history = policy.transition(transition(type, EducationAchievementStatus.DEPARTMENT_CONFIRMED, null));
        assertThat(history.achievementType()).isEqualTo(type);
        assertThat(history.achievementId()).isEqualTo(101L);
        assertThat(history.processedBy()).isEqualTo(102L);
        assertThat(history.processedAt()).isEqualTo(EducationAchievementTestSupport.PROCESSED_AT);
    }

    @Test
    void rejectionStillRequiresReasonAndInvalidLifecycleRemainsRejected() {
        for (String type : EducationAchievementTypes.EXTENSION_TYPES) {
            assertThatThrownBy(() -> policy.transition(
                    transition(type, EducationAchievementStatus.DEPARTMENT_REJECTED, null)))
                    .isInstanceOf(BusinessValidationException.class);
            var history = policy.transition(transition(type, EducationAchievementStatus.DEPARTMENT_REJECTED, "근거 부족"));
            assertThat(history.opinion()).isEqualTo("근거 부족");
            assertThatThrownBy(() -> policy.transition(
                    transition(type, EducationAchievementStatus.EVALUATION_CONFIRMED, null)))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "employment-rate-improvements,employment-rate-improvement-achievements",
        "course-operations,course-offering-operation-achievements",
        "lecture-improvements,teaching-improvement-achievements",
        "employment-rate-achievements,employment-rate-achievements"
    })
    void resourceAndAllChildrenResolveWithoutPrefixCollisions(String apiResource, String uiResource) {
        String api = "/api/business/" + apiResource;
        for (String suffix : new String[] {"", "/101", "/download", "/excel-uploads", "/bulk-jobs/job"}) {
            assertThat(EducationAchievementFoundationContract.uiRouteForApiPath(api + suffix))
                    .isEqualTo("/faculty/" + uiResource);
        }
        assertThat(EducationAchievementFoundationContract.uiRouteForApiPath(api + "-unrelated")).isNull();
        assertThat(EducationAchievementFoundationContract.uiRouteForApiPath(null)).isNull();
    }

    @Test
    void semesterRoundTripPreservesIntegerContractAndRejectsInvalidInputs() {
        for (int semester : new int[] {1, 2}) {
            String code = EducationAchievementValidation.semesterCode(semester);
            assertThat(code).isEqualTo(String.valueOf(semester));
            assertThat(EducationAchievementValidation.semester(code)).isEqualTo(semester);
        }
        for (Integer invalid : new Integer[] {null, 0, 3}) {
            assertThatThrownBy(() -> EducationAchievementValidation.semesterCode(invalid))
                    .isInstanceOfSatisfying(BusinessValidationException.class,
                            ex -> assertThat(ex.fields().get(0).field()).isEqualTo("semester"));
        }
    }

    @Test
    void optionalDateRangePermitsAbsentAndSameDayButRejectsPartialOrReversedRange() {
        LocalDate date = EducationAchievementTestSupport.ACHIEVEMENT_DATE;
        EducationAchievementValidation.validateSpecialLectureDates(null, null);
        EducationAchievementValidation.validateSpecialLectureDates(date, date);
        assertThatThrownBy(() -> EducationAchievementValidation.validateSpecialLectureDates(date, null))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> EducationAchievementValidation.validateSpecialLectureDates(null, date))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> EducationAchievementValidation.validateSpecialLectureDates(date, date.minusDays(1)))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void roleFixturesDoNotSilentlyGrantAdministratorAndPreserveUnionRoles() {
        assertThat(EducationAchievementTestSupport.principal(101L, "R01", "R02").roles())
                .containsExactly("R01", "R02");
        for (String role : new String[] {"R01", "R02", "R04", "R07", "R09"}) {
            assertThat(EducationAchievementTestSupport.principal(101L, role).roles()).containsExactly(role);
        }
    }

    private EducationAchievementStatusTransitionRequest transition(
            String type, EducationAchievementStatus next, String opinion) {
        return new EducationAchievementStatusTransitionRequest(
                type, 101L, EducationAchievementStatus.SUBMITTED, next, "DEPARTMENT_CONFIRM",
                null, opinion, 102L, EducationAchievementTestSupport.PROCESSED_AT);
    }
}
