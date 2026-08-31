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
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BusinessStatusTransitionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BusinessStatusTransitionApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean BusinessStatusTransitionService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listBusinessStatusTransitionsReturnsDefaultTwentyAndFiltersForReq774() throws Exception {
        when(service.list(new BusinessStatusTransitionSearchCriteria(0, 20, "FACULTY_ACHIEVEMENT", "SUBMITTED", "R02")))
                .thenReturn(new BusinessStatusTransitionSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/business-status-transitions")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("businessType", "FACULTY_ACHIEVEMENT")
                        .param("fromStatusCode", "SUBMITTED")
                        .param("executorRoleCode", "R02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transitions[0].businessType").value("FACULTY_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.transitions[0].fromStatusCode").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.transitions[0].toStatusCode").value("DEPARTMENT_CONFIRMED"))
                .andExpect(jsonPath("$.data.transitions[0].executorRoleCode").value("R02"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void saveBusinessStatusTransitionPersistsDraftRuleForReq775AndReq776() throws Exception {
        when(service.save(any(BusinessStatusTransitionSaveRequest.class), eq(1L))).thenReturn(row());

        mockMvc.perform(post("/api/admin/business-status-transitions")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"definitionVersion":"DRAFT","businessType":"FACULTY_ACHIEVEMENT","fromStatusCode":"SUBMITTED","toStatusCode":"DEPARTMENT_CONFIRMED","executorRoleCode":"R02","opinionRequiredYn":"N","attachmentRequiredYn":"N","cancellableYn":"Y","changeReason":"전이규칙 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.definitionVersion").value("DRAFT"))
                .andExpect(jsonPath("$.data.toStatusCode").value("DEPARTMENT_CONFIRMED"))
                .andExpect(jsonPath("$.data.cancellableYn").value("Y"));
    }

    @Test
    void saveBusinessStatusTransitionRequiresToStatusCodeForReq775() throws Exception {
        mockMvc.perform(post("/api/admin/business-status-transitions")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"definitionVersion":"DRAFT","businessType":"FACULTY_ACHIEVEMENT","fromStatusCode":"SUBMITTED","executorRoleCode":"R02","opinionRequiredYn":"N","attachmentRequiredYn":"N","cancellableYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("toStatusCode"));
    }

    @Test
    void directR01SaveBusinessStatusTransitionIsForbiddenForReq775() throws Exception {
        mockMvc.perform(post("/api/admin/business-status-transitions")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"definitionVersion":"DRAFT","businessType":"FACULTY_ACHIEVEMENT","fromStatusCode":"SUBMITTED","toStatusCode":"DEPARTMENT_CONFIRMED","executorRoleCode":"R02","opinionRequiredYn":"N","attachmentRequiredYn":"N","cancellableYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any());
    }

    @Test
    void saveBusinessStatusTransitionRejectsConfirmedRuleChangeForReq775() throws Exception {
        when(service.save(any(BusinessStatusTransitionSaveRequest.class), eq(1L)))
                .thenThrow(new ConflictException("확정된 전이규칙은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/business-status-transitions")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"definitionVersion":"CONFIRMED","businessType":"FACULTY_ACHIEVEMENT","fromStatusCode":"SUBMITTED","toStatusCode":"DEPARTMENT_REJECTED","executorRoleCode":"R02","opinionRequiredYn":"Y","attachmentRequiredYn":"N","cancellableYn":"N","changeReason":"검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void serviceRejectsInvalidBusinessTypeWithoutChangingTransitionsForReq775() {
        BusinessStatusTransitionMapper mapper = org.mockito.Mockito.mock(BusinessStatusTransitionMapper.class);
        BusinessStatusTransitionService transitionService = new BusinessStatusTransitionService(mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> transitionService.save(
                        new BusinessStatusTransitionSaveRequest("DRAFT", "UNKNOWN", "SUBMITTED", "DEPARTMENT_CONFIRMED", "R02", "N", "N", "Y", "검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("상태 전이");
        verify(mapper, never()).upsertDraftTransition(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void serviceRequiresYnFlagsForTransitionRequiredConditionsForReq776() {
        BusinessStatusTransitionMapper mapper = org.mockito.Mockito.mock(BusinessStatusTransitionMapper.class);
        BusinessStatusTransitionService transitionService = new BusinessStatusTransitionService(mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> transitionService.save(
                        new BusinessStatusTransitionSaveRequest("DRAFT", "FACULTY_ACHIEVEMENT", "SUBMITTED", "DEPARTMENT_REJECTED", "R02", "INVALID", "N", "N", "검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("상태 전이");
        verify(mapper, never()).upsertDraftTransition(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void serviceRecordsTransitionChangeHistoryForReq776() {
        BusinessStatusTransitionMapper mapper = org.mockito.Mockito.mock(BusinessStatusTransitionMapper.class);
        BusinessStatusTransitionService transitionService = new BusinessStatusTransitionService(mapper);
        BusinessStatusTransitionRow before = new BusinessStatusTransitionRow(10L, "DRAFT", "FACULTY_ACHIEVEMENT", "SUBMITTED", "DEPARTMENT_CONFIRMED", "R02", "N", "N", "Y", "기존", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
        BusinessStatusTransitionRow after = new BusinessStatusTransitionRow(10L, "DRAFT", "FACULTY_ACHIEVEMENT", "SUBMITTED", "DEPARTMENT_CONFIRMED", "R02", "Y", "N", "Y", "필수의견 적용", 1L, LocalDateTime.parse("2026-08-31T09:05:00"));
        when(mapper.statusCodeExists("FACULTY_ACHIEVEMENT", "DRAFT", "SUBMITTED")).thenReturn(1);
        when(mapper.statusCodeExists("FACULTY_ACHIEVEMENT", "DRAFT", "DEPARTMENT_CONFIRMED")).thenReturn(1);
        when(mapper.roleExists("R02")).thenReturn(1);
        when(mapper.findByKey("FACULTY_ACHIEVEMENT", "DRAFT", "SUBMITTED", "DEPARTMENT_CONFIRMED", "R02")).thenReturn(before, after);

        transitionService.save(new BusinessStatusTransitionSaveRequest("DRAFT", "FACULTY_ACHIEVEMENT", "SUBMITTED", "DEPARTMENT_CONFIRMED", "R02", "Y", "N", "Y", "필수의견 적용"), 1L);

        verify(mapper).insertChangeHistory("business_status_transitions", "FACULTY_ACHIEVEMENT:SUBMITTED->DEPARTMENT_CONFIRMED:R02", "UPDATE", "opinion_required_yn", "N", "Y", 1L, "필수의견 적용");
    }

    private BusinessStatusTransitionRow row() {
        return new BusinessStatusTransitionRow(10L, "DRAFT", "FACULTY_ACHIEVEMENT", "SUBMITTED", "DEPARTMENT_CONFIRMED", "R02", "N", "N", "Y", "전이규칙 정비", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
