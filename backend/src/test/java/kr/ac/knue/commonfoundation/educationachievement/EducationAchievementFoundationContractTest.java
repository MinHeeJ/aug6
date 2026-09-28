package kr.ac.knue.commonfoundation.educationachievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Defines the Phase 1 foundation contract for the shared education-achievement module.
 *
 * <p>These tests deliberately fail until T001 adds the incremental Flyway migration and T002
 * connects the shared module to the established SessionCookie/menu-permission boundary.</p>
 */
class EducationAchievementFoundationContractTest {
    private static final String EDUCATION_API_PATH = "/api/business/lecture-evaluation-achievements";
    private static final String LECTURE_EVALUATION_ROUTE = "/achievement/lecture-evaluations";
    private static final List<String> REQUIRED_TABLES = List.of(
            "education_achievements",
            "education_achievement_status_histories",
            "student_guidance_achievement_details",
            "graduate_degree_achievement_students");

    /**
     * Removing the common source table, either detail table, state history, audit fields, or varied
     * fixtures would make the four education-achievement flows impossible to build safely.
     */
    @Test
    void phaseOneMigrationCreatesAuditableSharedAndSpecializedEducationAchievementFixtures() throws Exception {
        String migration = educationAchievementMigrationSql().toLowerCase();

        for (String table : REQUIRED_TABLES) {
            assertThat(migration)
                    .as("T001 requires an idempotent education-achievement table: %s", table)
                    .contains("create table if not exists " + table)
                    .contains("comment on table " + table);
        }
        assertThat(migration)
                .contains("achievement_type")
                .contains("management_number")
                .contains("teacher_user_id")
                .contains("management_item_code")
                .contains("achievement_status")
                .contains("deleted_yn")
                .contains("created_at")
                .contains("created_by")
                .contains("updated_at")
                .contains("updated_by")
                .contains("create index if not exists idx_education_achievements_")
                .contains("max(menu_id)")
                .contains("as seed(menu_offset, menu_type, menu_name")
                .doesNotContain("as seed(offset, menu_type, menu_name");
        assertThat(migration)
                .contains("insert into education_achievements")
                .contains("draft")
                .contains("evaluation_confirmed")
                .contains("rejected")
                .doesNotContain("create table if not exists roles")
                .doesNotContain("insert into roles");
    }

    /**
     * A wrong API-to-screen mapping would bypass the existing menu permission gate even when the
     * caller has a valid SessionCookie, exposing a business endpoint outside its authorized route.
     */
    @Test
    void educationAchievementBusinessApiReusesSessionCookieAndLectureEvaluationMenuPermission() throws Exception {
        AuthService authService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(authService, permissionService, new ObjectMapper());
        CurrentUser facultyMember = new CurrentUser(101L, "teacher", "E0101", "교원", List.of("R01"), List.of());
        when(authService.currentUser("B74-SESSION")).thenReturn(facultyMember);
        when(permissionService.canAccess(101L, List.of("R01"), LECTURE_EVALUATION_ROUTE)).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", EDUCATION_API_PATH);
        request.setServletPath(EDUCATION_API_PATH);
        request.setCookies(new Cookie(AuthController.SESSION_COOKIE, "B74-SESSION"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute("currentUser")).isEqualTo(facultyMember);
        verify(permissionService).canAccess(eq(101L), eq(List.of("R01")), eq(LECTURE_EVALUATION_ROUTE));
        verify(chain).doFilter(request, response);
    }

    /**
     * Removing any shared controller/service/mapper layer would prevent the later story slices
     * from using one consistent API and persistence boundary for all four achievement types.
     */
    @Test
    void commonEducationAchievementControllerServiceAndMapperTypesExistForLaterStorySlices() {
        List<String> requiredTypes = List.of(
                "kr.ac.knue.commonfoundation.educationachievement.EducationAchievementController",
                "kr.ac.knue.commonfoundation.educationachievement.EducationAchievementService",
                "kr.ac.knue.commonfoundation.educationachievement.EducationAchievementMapper");

        assertThat(requiredTypes)
                .allSatisfy(type -> assertThat(typeExists(type)).as(type + " is required by T002").isTrue());
    }

    private String educationAchievementMigrationSql() throws Exception {
        return Arrays.stream(migrationResources())
                .filter(resource -> resource.getFilename() != null
                        && resource.getFilename().matches("V\\d+__basic74_.*\\.sql"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("T001 education-achievement incremental migration is missing"))
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private Resource[] migrationResources() throws Exception {
        return new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql");
    }

    private boolean typeExists(String type) {
        try {
            Class.forName(type);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
