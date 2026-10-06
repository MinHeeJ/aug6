package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.health.HealthController;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Filter-boundary checks only; these do not claim controller, role, scope or SQL coverage. */
class EducationSecurityBoundaryTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AuthService auth = mock(AuthService.class);
    private final EffectivePermissionService permissions = mock(EffectivePermissionService.class);
    private final AuthenticationFilter filter = new AuthenticationFilter(auth, permissions, json);

    @ParameterizedTest
    @CsvSource({
        "GET,/api/business/employment-rate-improvements",
        "POST,/api/business/employment-rate-improvements",
        "GET,/api/business/employment-rate-improvements/101",
        "PUT,/api/business/employment-rate-improvements/101",
        "GET,/api/business/course-operations",
        "POST,/api/business/course-operations",
        "GET,/api/business/course-operations/101",
        "PUT,/api/business/course-operations/101",
        "GET,/api/business/lecture-improvements",
        "POST,/api/business/lecture-improvements",
        "GET,/api/business/lecture-improvements/101",
        "PUT,/api/business/lecture-improvements/101",
        "GET,/api/business/employment-rate-achievements",
        "POST,/api/business/employment-rate-achievements",
        "GET,/api/business/employment-rate-achievements/101",
        "PUT,/api/business/employment-rate-achievements/101",
        "GET,/api/business/employment-rate-achievements/download",
        "POST,/api/business/employment-rate-achievements/excel-uploads",
        "GET,/api/business/employment-rate-achievements/excel-uploads/template",
        "GET,/api/business/employment-rate-achievements/excel-uploads/histories",
        "POST,/api/business/employment-rate-achievements/excel-uploads/upload-token/commit",
        "GET,/api/business/employment-rate-achievements/excel-uploads/upload-token/errors",
        "GET,/api/business/employment-rate-achievements/excel-uploads/upload-token/errors/download",
        "POST,/api/business/employment-rate-achievements/bulk-jobs",
        "POST,/api/business/employment-rate-achievements/bulk-jobs/preview",
        "GET,/api/business/employment-rate-achievements/bulk-jobs/job-token"
    })
    void anonymousBusinessRequestsStopBeforeControllersOrDatabase(String method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        JsonNode body = json.readTree(response.getContentAsString());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("error").path("code").asText()).isEqualTo("UNAUTHENTICATED");
        assertThat(body.path("error").path("message").asText()).isEqualTo("인증이 필요합니다.");
        assertThat(response.getContentAsString()).doesNotContain("password", "jdbc:", "SQLException", "stackTrace");
        verifyNoInteractions(auth, permissions, chain);
    }

    @Test
    void publicHealthStillUsesTheRealHealthControllerWithTheAuthenticationFilter() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HealthController()).addFilters(filter).build();

        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("common-foundation"));

        verifyNoInteractions(auth, permissions);
    }
}
