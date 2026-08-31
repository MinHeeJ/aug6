package kr.ac.knue.commonfoundation.basic34;

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

class Basic34FoundationContractTest {
    @Test
    void verifiesExistingBasic33RuleVersionSeedsAndCommonSessionRoleMenuFixturesForReq897Req900() throws Exception {
        String basic33Sql = new ClassPathResource("db/migration/V27__basic33_evaluation_rule_foundation.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String foundationSeedSql = new ClassPathResource("db/migration/V2__common_foundation_seed.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(basic33Sql).contains("'B33-DRAFT-2026'")
                .contains("'B33-CONFIRMED-2026'")
                .contains("'B33-DISCARDED-2025'")
                .contains("version_status varchar(20) NOT NULL")
                .contains("CONSTRAINT ck_evaluation_rule_versions_status CHECK (version_status IN ('DRAFT','CONFIRMED','DISCARDED'))");
        assertThat(foundationSeedSql).contains("INSERT INTO roles")
                .contains("('R01','교원'")
                .contains("('R02','학과장'")
                .contains("('R03','단과대학(원) 행정실'")
                .contains("('R04','교수지원과'")
                .contains("('R05','산학협력단'")
                .contains("('R06','입학인재관리과'")
                .contains("('R07','실적부서'")
                .contains("('R08','점수산출 감사자'")
                .contains("('R09','시스템관리자'")
                .contains("INSERT INTO menu_permissions");
        assertThat(AuthController.SESSION_COOKIE).isEqualTo("COMMON_FOUNDATION_SESSION");
    }

    @Test
    void basic34AddsIncrementalBusinessRuleMigrationForEvaluationScoresRatesFormulasRuleSetsAndJournalsForReq897Req974() throws Exception {
        String sql = new ClassPathResource("db/migration/V32__basic34_evaluation_rule_business_foundation.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String lowerSql = sql.toLowerCase();

        assertThat(lowerSql).contains("alter table evaluation_rule_versions")
                .contains("classification_rule_version_id")
                .contains("create table if not exists evaluation_score_rules")
                .contains("create table if not exists participation_rate_rules")
                .contains("create table if not exists calculation_formula_versions")
                .contains("create table if not exists evaluation_rule_sets")
                .contains("create table if not exists journal_indexing_infos");
        assertThat(lowerSql).contains("foreign key (rule_version_id) references evaluation_rule_versions(rule_version_id)")
                .contains("foreign key (management_item_id) references evaluation_management_items(management_item_id)")
                .contains("foreign key (created_by) references users(user_id)")
                .contains("comment on table evaluation_score_rules")
                .contains("comment on table participation_rate_rules")
                .contains("comment on table calculation_formula_versions")
                .contains("comment on table evaluation_rule_sets")
                .contains("comment on table journal_indexing_infos")
                .contains("comment on column evaluation_rule_versions.version_status is 'draft:작성중|confirmed:확정|discarded:폐기'")
                .contains("comment on column evaluation_score_rules.active_yn is 'y:사용|n:미사용'")
                .contains("comment on column participation_rate_rules.active_yn is 'y:사용|n:미사용'")
                .contains("comment on column calculation_formula_versions.calculation_type is 'fixed_score:정액배점|distribution_rate:배분율적용|cap:상한적용|ladder:구간별배점'")
                .contains("comment on column journal_indexing_infos.indexing_type is 'kci:등재지|candidate:후보지|international:국제등재|other:기타'");
        assertThat(lowerSql).contains("create index if not exists idx_evaluation_score_rules_search")
                .contains("create index if not exists idx_participation_rate_rules_search")
                .contains("create index if not exists idx_calculation_formula_versions_search")
                .contains("create index if not exists idx_evaluation_rule_sets_search")
                .contains("create index if not exists idx_journal_indexing_infos_search")
                .doesNotContain(" is null or ")
                .doesNotContain("coalesce(:")
                .doesNotContain("? is null");
    }

    @Test
    void basic34FoundationExposesOnlyBusinessRuleRouteMappingsAndReusesExistingRoleCodes() {
        assertThat(EvaluationRuleBusinessFoundationContract.ALLOWED_ROLE_CODES)
                .containsExactly("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
        assertThat(EvaluationRuleBusinessFoundationContract.WRITE_ROLE_CODES)
                .containsExactly("R04", "R08", "R09");
        assertThat(EvaluationRuleBusinessFoundationContract.API_ROUTE_BY_PREFIX)
                .containsEntry("/api/admin/evaluation-scores", "/admin/evaluation-scores")
                .containsEntry("/api/admin/participation-rates", "/admin/participation-rates")
                .containsEntry("/api/admin/calculation-formulas", "/admin/calculation-formulas")
                .containsEntry("/api/admin/evaluation-rule-sets", "/admin/evaluation-rule-sets")
                .containsEntry("/api/admin/journal-indexing-infos", "/admin/journal-indexing-infos");
    }

    @Test
    void basic34AdminApisReuseSessionCookieAndExistingMenuPermissionGuardForReq914Req919() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
        CurrentUser user = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
        when(authService.currentUser("SESSION-B34")).thenReturn(user);
        for (String route : EvaluationRuleBusinessFoundationContract.API_ROUTE_BY_PREFIX.values()) {
            when(permissionService.canAccess(eq(9L), eq(List.of("R09")), eq(route))).thenReturn(true);
        }

        for (String apiPrefix : EvaluationRuleBusinessFoundationContract.API_ROUTE_BY_PREFIX.keySet()) {
            for (String path : List.of(apiPrefix, apiPrefix + "/save")) {
                MockHttpServletRequest request = request("GET", path);
                request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "SESSION-B34"));
                MockHttpServletResponse response = new MockHttpServletResponse();
                FilterChain chain = mock(FilterChain.class);

                filter.doFilter(request, response, chain);

                assertThat(response.getStatus()).as(path).isEqualTo(200);
                verify(chain).doFilter(any(), any());
            }
        }

        for (String route : EvaluationRuleBusinessFoundationContract.API_ROUTE_BY_PREFIX.values()) {
            verify(permissionService, atLeastOnce()).canAccess(9L, List.of("R09"), route);
        }
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
