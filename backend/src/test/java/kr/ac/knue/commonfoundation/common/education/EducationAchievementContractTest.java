package kr.ac.knue.commonfoundation.common.education;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EducationAchievementContractTest {
    @Test
    void eachCollectionAndDetailKeepsItsCanonicalMenuRoute() {
        for (EducationAchievementContract.Surface surface : EducationAchievementContract.SURFACES) {
            assertThat(EducationAchievementContract.uiRouteForApiPath(surface.apiPath()))
                    .isEqualTo(surface.uiRoute());
            assertThat(EducationAchievementContract.uiRouteForApiPath(surface.apiPath() + "/23"))
                    .isEqualTo(surface.uiRoute());
            assertThat(EducationAchievementContract.uiRouteForApiPath(surface.apiPath() + "-unrelated"))
                    .isNull();
        }
        assertThat(EducationAchievementContract.SURFACES)
                .extracting(EducationAchievementContract.Surface::achievementType)
                .containsExactly("FR-029", "FR-030", "FR-031", "FR-032");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/download", "/excel-uploads", "/excel-uploads/template", "/excel-uploads/histories",
        "/excel-uploads/example-upload/commit", "/excel-uploads/example-upload/errors",
        "/excel-uploads/example-upload/errors/download", "/bulk-jobs", "/bulk-jobs/preview",
        "/bulk-jobs/example-job"
    })
    void everyEmploymentDescendantUsesTheSameScreenPermission(String suffix) {
        assertThat(EducationAchievementContract.uiRouteForApiPath(
                "/api/business/employment-rate-achievements" + suffix))
                .isEqualTo("/faculty/employment-rate-achievements");
    }

    @Test
    void unknownAndExistingEducationPathsRemainOwnedByExistingMappings() {
        assertThat(EducationAchievementContract.uiRouteForApiPath(null)).isNull();
        assertThat(EducationAchievementContract.uiRouteForApiPath("/api/health")).isNull();
        assertThat(EducationAchievementContract.uiRouteForApiPath(
                "/api/business/lecture-achievements")).isNull();
        assertThat(EducationAchievementContract.uiRouteForApiPath(
                "/api/business/student-guidance-achievements/excel-uploads")).isNull();
    }
}
