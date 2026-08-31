package kr.ac.knue.commonfoundation.basic32;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EvaluationOrganizationMappingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationOrganizationMappingApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationOrganizationMappingService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationOrganizationMappingsReturnsMappingsWithDefaultTwentyRowsForReq768() throws Exception {
        when(service.list(new EvaluationOrganizationMappingSearchCriteria(0, 20, "FACULTY_ACHIEVEMENT", "COLL-EDU", 2L)))
                .thenReturn(new EvaluationOrganizationMappingSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/evaluation-organization-mappings")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("businessType", "FACULTY_ACHIEVEMENT")
                        .param("organizationCode", "COLL-EDU")
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mappings[0].mappingId").value(101))
                .andExpect(jsonPath("$.data.mappings[0].businessType").value("FACULTY_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.mappings[0].dataScope").value("COLLEGE"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void saveEvaluationOrganizationMappingPersistsThenReturnsSavedMappingForReq769() throws Exception {
        when(service.save(any(EvaluationOrganizationMappingSaveRequest.class), eq(1L))).thenReturn(row());

        mockMvc.perform(post("/api/business/evaluation-organization-mappings")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2,"organizationCode":"COLL-EDU","businessType":"FACULTY_ACHIEVEMENT","dataScope":"COLLEGE","changeReason":"평가조직 업무권한 연결"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.organizationCode").value("COLL-EDU"))
                .andExpect(jsonPath("$.data.businessType").value("FACULTY_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.dataScope").value("COLLEGE"));
    }

    @Test
    void saveEvaluationOrganizationMappingRequiresBusinessTypeFieldForReq769() throws Exception {
        mockMvc.perform(post("/api/business/evaluation-organization-mappings")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2,"organizationCode":"COLL-EDU","dataScope":"COLLEGE","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("businessType"));
    }

    @Test
    void saveEvaluationOrganizationMappingRequiresExistingSessionPrincipalForReq770() throws Exception {
        mockMvc.perform(post("/api/business/evaluation-organization-mappings")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2,"organizationCode":"COLL-EDU","businessType":"FACULTY_ACHIEVEMENT","dataScope":"COLLEGE","changeReason":"검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).save(any(), any());
    }

    @Test
    void directR01SaveEvaluationOrganizationMappingIsForbiddenForReq770() throws Exception {
        mockMvc.perform(post("/api/business/evaluation-organization-mappings")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2,"organizationCode":"COLL-EDU","businessType":"FACULTY_ACHIEVEMENT","dataScope":"COLLEGE","changeReason":"검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any());
    }

    @Test
    void serviceRejectsMissingOrganizationWithoutCreatingMappingForReq769() {
        EvaluationOrganizationMappingMapper mapper = org.mockito.Mockito.mock(EvaluationOrganizationMappingMapper.class);
        EvaluationOrganizationMappingService mappingService = new EvaluationOrganizationMappingService(mapper);
        when(mapper.existsUser(2L)).thenReturn(1);
        when(mapper.existsOrganization("UNKNOWN")).thenReturn(0);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mappingService.save(
                        new EvaluationOrganizationMappingSaveRequest(2L, "UNKNOWN", "FACULTY_ACHIEVEMENT", "COLLEGE", "검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("평가조직 매핑");
        verify(mapper, never()).upsertMapping(any(), any(), any(), any(), any(), any());
    }

    private EvaluationOrganizationMappingRow row() {
        return new EvaluationOrganizationMappingRow(101L, 2L, "teacher", "홍길동", "COLL-EDU", "교육대학", "FACULTY_ACHIEVEMENT", "COLLEGE", "평가조직 업무권한 연결", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
