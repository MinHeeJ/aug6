package kr.ac.knue.commonfoundation.basic60;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.health.HealthController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regression contract for Phase 5 cross-cutting behavior around the BASIC-60 setting screens.
 *
 * <p>The health request-id assertion is intentionally the focused Red observation: every
 * operational request must remain traceable through its response envelope, including the
 * deployment health probe used by this change's Docker smoke check.</p>
 */
@WebMvcTest({Basic60Controller.class, HealthController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic60QualityRegressionApiTest {
    private static final CurrentUser SETTINGS_ADMIN = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Basic60Service service;

    @Test
    void healthSmokePreservesTheSuppliedRequestIdentifierInItsApiEnvelope() throws Exception {
        mockMvc.perform(get("/api/health").header("X-Request-Id", "REQ-B60-HEALTH-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B60-HEALTH-001"));
    }

    @ParameterizedTest
    @MethodSource("protectedListPaths")
    void protectedSettingListsRejectUnauthenticatedRequestsWithoutCallingPersistence(String path)
            throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        verify(service, never()).listElementSettings(any());
        verify(service, never()).listParticipationSettings(any());
        verify(service, never()).listScoreSettings(any());
    }

    @ParameterizedTest
    @MethodSource("protectedListPaths")
    void invalidPageSizeReturnsAValidationEnvelopeWithoutSensitiveInternalDetails(String path)
            throws Exception {
        mockMvc.perform(get(path)
                        .requestAttr("currentUser", SETTINGS_ADMIN)
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'pageSize')]").isNotEmpty())
                .andExpect(jsonPath("$.error.message").value("목록 표시 건수가 올바르지 않습니다."));
    }

    private static Stream<String> protectedListPaths() {
        return Stream.of(
                "/api/admin/evaluation-element-management-item-settings",
                "/api/admin/participation-allocation-rate-settings",
                "/api/admin/management-item-evaluation-score-settings");
    }
}
