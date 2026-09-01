package kr.ac.knue.commonfoundation.businessperiod;

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

class Basic35BusinessPeriodFoundationContractTest {
    @Test
    void phase2AddsIncrementalBusinessPeriodTablesIndexesConstraintsAndSeedFixtures() throws Exception {
        String sql = new ClassPathResource("db/migration/V34__basic35_business_period_foundation.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String lowerSql = sql.toLowerCase();

        for (String table : BusinessPeriodFoundationContract.TABLES) {
            assertThat(lowerSql).contains("create table if not exists " + table);
            assertThat(lowerSql).contains("comment on table " + table);
            assertThat(lowerSql).contains("idx_" + table + "_search");
            assertThat(lowerSql).contains("ex_" + table + "_active_period");
            assertThat(lowerSql).contains("ck_" + table + "_period");
            assertThat(lowerSql).contains("ck_" + table + "_active");
            assertThat(lowerSql).contains("insert into " + table);
        }
        assertThat(lowerSql)
                .contains("create extension if not exists btree_gist")
                .contains("tsrange(start_at, end_at, '[]') with &&")
                .contains("comment on column evaluation_date_settings.active_yn is 'y:사용|n:미사용'")
                .contains("comment on column input_period_settings.active_yn is 'y:사용|n:미사용'")
                .doesNotContain(" is null or ")
                .doesNotContain("? is null")
                .doesNotContain("coalesce(:");
    }

    @Test
    void phase2MapperCoversAllListOperationsWithDynamicOptionalFiltersAndQuotedAliases() throws Exception {
        String mapperXml = new ClassPathResource("mapper/businessperiod/BusinessPeriodMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(mapperXml)
                .contains("id=\"listEvaluationDates\"")
                .contains("id=\"listInputPeriods\"")
                .contains("id=\"listModificationPeriods\"")
                .contains("id=\"listDepartmentChairConfirmPeriods\"")
                .contains("id=\"listBusinessPeriods\"")
                .contains("id=\"countOverlappingEvaluationDates\"")
                .contains("id=\"countOverlappingBusinessPeriods\"")
                .contains("as \"settingId\"")
                .contains("as \"evaluationYear\"")
                .contains("as \"organizationCode\"")
                .contains("as \"userTypeCode\"")
                .contains("<if test=\"criteria.normalizedEvaluationYear != null\">")
                .contains("<if test=\"criteria.normalizedAreaCode != null\">")
                .contains("<if test=\"criteria.normalizedKeyword != null\">")
                .contains("limit #{criteria.safeSize} offset #{criteria.offset}")
                .doesNotContain("IS NULL OR", "is null or", "? IS NULL", "COALESCE(:", "coalesce(:");
    }

    @Test
    void phase2RouteMappingsReuseExistingRoleCodesAndSessionCookieMenuGuard() throws Exception {
        assertThat(BusinessPeriodFoundationContract.ALLOWED_ROLE_CODES).containsExactly("R03", "R04", "R09");
        assertThat(BusinessPeriodFoundationContract.API_ROUTE_BY_PREFIX)
                .containsEntry("/api/admin/evaluation-dates", "/admin/evaluation-dates")
                .containsEntry("/api/admin/input-periods", "/admin/input-periods")
                .containsEntry("/api/admin/modification-periods", "/admin/modification-periods")
                .containsEntry("/api/admin/department-chair-confirm-periods", "/admin/department-chair-confirm-periods")
                .containsEntry("/api/admin/business-periods", "/admin/business-periods");

        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
        CurrentUser user = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
        when(authService.currentUser("SESSION-B35")).thenReturn(user);
        for (String route : BusinessPeriodFoundationContract.API_ROUTE_BY_PREFIX.values()) {
            when(permissionService.canAccess(eq(9L), eq(List.of("R09")), eq(route))).thenReturn(true);
        }

        for (String apiPrefix : BusinessPeriodFoundationContract.API_ROUTE_BY_PREFIX.keySet()) {
            MockHttpServletRequest request = request("GET", apiPrefix);
            request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "SESSION-B35"));
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).as(apiPrefix).isEqualTo(200);
            verify(chain).doFilter(any(), any());
        }
        for (String route : BusinessPeriodFoundationContract.API_ROUTE_BY_PREFIX.values()) {
            verify(permissionService, atLeastOnce()).canAccess(9L, List.of("R09"), route);
        }
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
