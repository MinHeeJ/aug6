package kr.ac.knue.commonfoundation.basic83;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

/**
 * Locks down the BASIC-83 shared schema, lifecycle, request-id, and session-menu
 * prerequisites before individual achievement controllers are introduced.
 */
class Basic83FoundationContractTest {
    @Test
    void migrationCreatesEmploymentRateImprovementSourceWithThreeStateFixturesAndMenuSeeds()
            throws Exception {
        String migration = StreamUtils.copyToString(
                new ClassPathResource("db/migration/V65__basic83_education_achievement_scope.sql").getInputStream(),
                StandardCharsets.UTF_8);
        String openApi = StreamUtils.copyToString(
                new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        assertThat(openApi).contains("/api/business/employment-rate-improvements");
        assertThat(migration)
                .contains("CREATE TABLE IF NOT EXISTS employment_rate_improvement_achievements")
                .contains("B83-ERI-001")
                .contains("B83-ERI-002")
                .contains("B83-ERI-003")
                .contains("'DRAFT'")
                .contains("'SUBMITTED'")
                .contains("'CERTIFIED'")
                .contains("/faculty/employment-rate-achievements")
                .contains("'R07'");
    }

    @Test
    void commonLifecycleAllowsEmploymentRateImprovementSubmissionAndPreservesRequestId()
            throws Exception {
        EducationAchievementStatusHistory history = new EducationAchievementStatusTransitionPolicy().transition(
                new EducationAchievementStatusTransitionRequest(
                        "EMPLOYMENT_RATE_IMPROVEMENT",
                        83L,
                        EducationAchievementStatus.DRAFT,
                        EducationAchievementStatus.SUBMITTED,
                        "SUBMIT",
                        null,
                        null,
                        1L,
                        LocalDateTime.of(2026, 10, 2, 9, 0)));
        ApiResponse<String> response = ApiResponse.ok("ready", " basic83-request-id ");

        assertThat(history.achievementType()).isEqualTo("EMPLOYMENT_RATE_IMPROVEMENT");
        assertThat(history.nextStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
        assertThat(response.meta()).containsEntry("requestId", "basic83-request-id");
    }

    @Test
    void commonLifecycleRejectsEmploymentRateImprovementRejectionWithoutReasonOrOpinion() {
        EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();

        assertThatThrownBy(() -> policy.transition(
                new EducationAchievementStatusTransitionRequest(
                        "EMPLOYMENT_RATE_IMPROVEMENT",
                        83L,
                        EducationAchievementStatus.SUBMITTED,
                        EducationAchievementStatus.DEPARTMENT_REJECTED,
                        "REJECT",
                        null,
                        null,
                        2L,
                        LocalDateTime.of(2026, 10, 2, 9, 5))))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("반려 처리에는 사유 또는 의견을 입력하세요.");
    }

    @Test
    void authenticatedBasic83ApiSubresourcesUseTheirScreenMenuRoute() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(
                authService,
                permissionService,
                new ObjectMapper());
        CurrentUser r01 = new CurrentUser(
                101L,
                "basic83-r01",
                "E101",
                "교육실적 사용자",
                List.of("R01"),
                List.of());

        verifyAuthorizedRoute(
                filter,
                authService,
                permissionService,
                r01,
                "/api/business/employment-rate-improvements/83",
                "/faculty/employment-rate-improvement-achievements");
        verifyAuthorizedRoute(
                filter,
                authService,
                permissionService,
                r01,
                "/api/business/course-operations/83",
                "/faculty/course-offering-operation-achievements");
        verifyAuthorizedRoute(
                filter,
                authService,
                permissionService,
                r01,
                "/api/business/lecture-improvements/83",
                "/faculty/teaching-improvement-achievements");
        verifyAuthorizedRoute(
                filter,
                authService,
                permissionService,
                r01,
                "/api/business/employment-rate-achievements/download",
                "/faculty/employment-rate-achievements");
    }

    private void verifyAuthorizedRoute(
            AuthenticationFilter filter,
            AuthService authService,
            EffectivePermissionService permissionService,
            CurrentUser user,
            String apiPath,
            String uiRoute) throws Exception {
        String sessionId = "SESSION-" + apiPath;
        when(authService.currentUser(sessionId)).thenReturn(user);
        when(permissionService.canAccess(eq(user.userId()), eq(user.roles()), eq(uiRoute))).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", apiPath);
        request.setServletPath(apiPath);
        request.setCookies(new Cookie(AuthController.SESSION_COOKIE, sessionId));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());
    }
}
