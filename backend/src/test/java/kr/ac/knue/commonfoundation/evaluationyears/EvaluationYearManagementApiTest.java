package kr.ac.knue.commonfoundation.evaluationyears;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EvaluationYearManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationYearManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationYearManagementService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void getEvaluationYearSettingsReturnsCurrentDefaultAndPreparationRows() throws Exception {
        when(service.getEvaluationYearSettings()).thenReturn(response());

        mockMvc.perform(get("/api/admin/system-settings/evaluation-years").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentEvaluationYear").value(2026))
                .andExpect(jsonPath("$.data.defaultSearchYear").value(2025))
                .andExpect(jsonPath("$.data.preparations[0].targetYear").value(2027))
                .andExpect(jsonPath("$.data.preparations[0].copyRequestedYn").value("Y"))
                .andExpect(jsonPath("$.data.preparations[0].resetRequestedYn").value("N"));
    }

    @Test
    void saveEvaluationYearSettingsPersistsSettingsAndTargetYearPreparationStatus() throws Exception {
        when(service.saveEvaluationYearSettings(any(EvaluationYearSettingsRequest.class), eq(1L))).thenReturn(response());

        mockMvc.perform(put("/api/admin/system-settings/evaluation-years")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentEvaluationYear":2026,"defaultSearchYear":2025,"changeReason":"기준연도 변경","preparations":[{"targetYear":2027,"copyRequestedYn":"Y","resetRequestedYn":"N","changeReason":"차년도 준비"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentEvaluationYear").value(2026))
                .andExpect(jsonPath("$.data.preparations[0].targetYear").value(2027));
    }

    @Test
    void saveEvaluationYearSettingsRequiresFieldLevelValidation() throws Exception {
        mockMvc.perform(put("/api/admin/system-settings/evaluation-years")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentEvaluationYear\":2026,\"defaultSearchYear\":2025,\"preparations\":[{\"copyRequestedYn\":\"Y\",\"resetRequestedYn\":\"N\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").exists());
    }

    @Test
    void saveEvaluationYearSettingsRequiresAuthenticatedAdminBeforePersistenceSideEffects() throws Exception {
        mockMvc.perform(put("/api/admin/system-settings/evaluation-years")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentEvaluationYear\":2026,\"defaultSearchYear\":2025,\"changeReason\":\"권한 검증\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveEvaluationYearSettings(any(), any());
    }

    @Test
    void saveEvaluationYearSettingsRejectsCopyAndResetAtSameTimeAndKeepsExistingState() {
        EvaluationYearMapper mapper = mock(EvaluationYearMapper.class);
        EvaluationYearManagementService evaluationYearService = new EvaluationYearManagementService(mapper);
        EvaluationYearSettingsRequest request = new EvaluationYearSettingsRequest(2026, 2025, "동시 요청", List.of(
                new EvaluationYearSettingsRequest.Preparation(2027, "Y", "Y", "충돌")));

        assertThatThrownBy(() -> evaluationYearService.saveEvaluationYearSettings(request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("기준연도 설정 저장 요청");
        verify(mapper, never()).upsertEvaluationYearSettings(any(), any(), any(), any());
        verify(mapper, never()).upsertEvaluationYearPreparation(any(), any(), any(), any(), any());
    }

    @Test
    void saveEvaluationYearSettingsDoesNotMutateExistingEvaluationResultsOrEditReferenceValues() {
        EvaluationYearMapper mapper = mock(EvaluationYearMapper.class);
        EvaluationYearManagementService evaluationYearService = new EvaluationYearManagementService(mapper);
        EvaluationYearSettingsRequest request = new EvaluationYearSettingsRequest(2026, 2025, "기준연도 변경", List.of(
                new EvaluationYearSettingsRequest.Preparation(2027, "Y", "N", "차년도 준비")));
        when(mapper.getEvaluationYearSettings()).thenReturn(new EvaluationYearSettingsRow(2026, 2025, 1L, LocalDateTime.parse("2026-08-19T09:00:00"), "기준연도 변경"));
        when(mapper.listEvaluationYearPreparations()).thenReturn(List.of(row()));

        evaluationYearService.saveEvaluationYearSettings(request, 1L);

        verify(mapper).upsertEvaluationYearSettings(2026, 2025, 1L, "기준연도 변경");
        verify(mapper).upsertEvaluationYearPreparation(2027, "Y", "N", 1L, "차년도 준비");
        verify(mapper, never()).mutateExistingEvaluationResults(any(), any(), any());
        verify(mapper, never()).editReferenceInformationValues(any(), any(), any());
    }

    @Test
    void forbiddenEvaluationYearRequestReturns403ApiError() throws Exception {
        when(service.getEvaluationYearSettings()).thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/admin/system-settings/evaluation-years"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private EvaluationYearSettingsResponse response() {
        return new EvaluationYearSettingsResponse(2026, 2025, List.of(row()), 1L,
                LocalDateTime.parse("2026-08-19T09:00:00"), "기준연도 변경");
    }

    private EvaluationYearPreparationRow row() {
        return new EvaluationYearPreparationRow(2027, "Y", "N", 1L,
                LocalDateTime.parse("2026-08-19T09:00:00"), "차년도 준비");
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
