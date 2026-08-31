package kr.ac.knue.commonfoundation.basic33;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class Basic33FoundationContractTest {
    @Test
    void basic33UsesIncrementalMigrationForEvaluationRuleClassificationTablesAndSeedStatusesForReq795Req845() throws Exception {
        String sql = new ClassPathResource("db/migration/V27__basic33_evaluation_rule_foundation.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String lowerSql = sql.toLowerCase();

        assertThat(lowerSql).contains("create table if not exists evaluation_rule_versions");
        assertThat(lowerSql).contains("create table if not exists evaluation_areas");
        assertThat(lowerSql).contains("create table if not exists evaluation_items");
        assertThat(lowerSql).contains("create table if not exists evaluation_elements");
        assertThat(lowerSql).contains("create table if not exists evaluation_management_items");
        assertThat(lowerSql).contains("create table if not exists area_element_system_settings");
        assertThat(lowerSql).contains("comment on table evaluation_rule_versions");
        assertThat(lowerSql).contains("comment on column evaluation_rule_versions.version_status is 'draft:작성중|confirmed:확정|discarded:폐기'");
        assertThat(sql).contains("('B33-DRAFT-2026', DATE '2026-01-01', DATE '2026-12-31', 'DRAFT'");
        assertThat(sql).contains("('B33-CONFIRMED-2026', DATE '2026-01-01', DATE '2026-12-31', 'CONFIRMED'");
        assertThat(sql).contains("('B33-DISCARDED-2025', DATE '2025-01-01', DATE '2025-12-31', 'DISCARDED'");
        assertThat(lowerSql).doesNotContain(" is null or ").doesNotContain("coalesce(:").doesNotContain("? is null");
    }

    @Test
    void evaluationRuleFoundationExposesStableDomainPackageSkeletonForFutureSlices() {
        assertThat(EvaluationRuleFoundationContract.ALLOWED_ROLE_CODES)
                .containsExactly("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
        assertThat(EvaluationRuleFoundationContract.API_ROUTE_BY_PREFIX)
                .containsEntry("/api/admin/evaluation-areas", "/admin/evaluation-areas")
                .containsEntry("/api/admin/evaluation-items", "/admin/evaluation-items")
                .containsEntry("/api/admin/evaluation-elements", "/admin/evaluation-elements")
                .containsEntry("/api/admin/evaluation-management-items", "/admin/evaluation-management-items")
                .containsEntry("/api/admin/area-element-systems", "/admin/area-element-systems");
    }

    @Test
    void basic33AdminApisReuseSessionCookieR01ThroughR09AndExistingMenuPermissionGuardForReq797() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
        CurrentUser user = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
        when(authService.currentUser("SESSION-B33")).thenReturn(user);
        for (String route : EvaluationRuleFoundationContract.API_ROUTE_BY_PREFIX.values()) {
            when(permissionService.canAccess(eq(9L), eq(List.of("R09")), eq(route))).thenReturn(true);
        }

        for (String apiPrefix : EvaluationRuleFoundationContract.API_ROUTE_BY_PREFIX.keySet()) {
            for (String path : List.of(apiPrefix, apiPrefix + "/save")) {
                MockHttpServletRequest request = request("GET", path);
                request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "SESSION-B33"));
                MockHttpServletResponse response = new MockHttpServletResponse();
                FilterChain chain = mock(FilterChain.class);

                filter.doFilter(request, response, chain);

                assertThat(response.getStatus()).as(path).isEqualTo(200);
                verify(chain).doFilter(any(), any());
            }
        }

        for (String route : EvaluationRuleFoundationContract.API_ROUTE_BY_PREFIX.values()) {
            verify(permissionService, atLeastOnce()).canAccess(9L, List.of("R09"), route);
        }
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
