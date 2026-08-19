package kr.ac.knue.commonfoundation.codes;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.time.LocalDate;
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

@WebMvcTest(DetailCodeUsageManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DetailCodeUsageManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean DetailCodeUsageManagementService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void listDetailCodeUsageSettingsReturnsUseYnPeriodRowsAndNewInputOptions() throws Exception {
        when(service.listDetailCodeUsageSettings("PROC_STATUS", 0, 10))
                .thenReturn(new DetailCodeUsageSearchResponse(List.of(activeRow(), endedRow()), List.of(activeRow()), 0, 10, 2));

        mockMvc.perform(get("/api/admin/code-groups/PROC_STATUS/codes/usage-settings")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settings[0].groupId").value("PROC_STATUS"))
                .andExpect(jsonPath("$.data.settings[0].codeValue").value("OPEN"))
                .andExpect(jsonPath("$.data.settings[0].codeName").value("진행"))
                .andExpect(jsonPath("$.data.settings[0].systemUseYn").value("Y"))
                .andExpect(jsonPath("$.data.settings[0].validStartDate").value("2026-01-01"))
                .andExpect(jsonPath("$.data.settings[0].validEndDate").value("2099-12-31"))
                .andExpect(jsonPath("$.data.selectableOptions.length()").value(1))
                .andExpect(jsonPath("$.data.selectableOptions[0].codeValue").value("OPEN"));
    }

    @Test
    void saveDetailCodeUsageSettingsPersistsOnlyUsagePeriodAndReturnsUpdatedRows() throws Exception {
        when(service.saveDetailCodeUsageSettings(eq("PROC_STATUS"), any(DetailCodeUsageSettingsRequest.class), eq(1L)))
                .thenReturn(List.of(endedRow()));

        mockMvc.perform(put("/api/admin/code-groups/PROC_STATUS/codes/usage-settings")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"codeValue":"ENDED","systemUseYn":"N","validStartDate":"2026-01-01","validEndDate":"2026-08-19","changeReason":"신규 입력 종료"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].codeValue").value("ENDED"))
                .andExpect(jsonPath("$.data[0].codeName").value("종료됨"))
                .andExpect(jsonPath("$.data[0].systemUseYn").value("N"));
    }

    @Test
    void saveDetailCodeUsageSettingsRequiresAuthenticatedAdminBeforePersistenceSideEffects() throws Exception {
        mockMvc.perform(put("/api/admin/code-groups/PROC_STATUS/codes/usage-settings")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"codeValue":"OPEN","systemUseYn":"Y","validStartDate":"2026-01-01","validEndDate":"2099-12-31","changeReason":"권한 검증"}]}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveDetailCodeUsageSettings(any(), any(), any());
    }

    @Test
    void saveDetailCodeUsageSettingsRejectsEndBeforeStartAndDoesNotUpdateRows() {
        DetailCodeUsageManagementMapper mapper = mock(DetailCodeUsageManagementMapper.class);
        DetailCodeUsageManagementService usageService = new DetailCodeUsageManagementService(mapper);
        DetailCodeUsageSettingsRequest request = new DetailCodeUsageSettingsRequest(List.of(new DetailCodeUsageSettingsRequest.Item(
                "OPEN", null, "Y", LocalDate.parse("2026-08-20"), LocalDate.parse("2026-08-19"), "기간 검증")));

        assertThatThrownBy(() -> usageService.saveDetailCodeUsageSettings("PROC_STATUS", request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("코드 사용 설정 저장 요청");
        verify(mapper, never()).updateDetailCodeUsageSetting(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void saveDetailCodeUsageSettingsRejectsCodeNameMutationAndKeepsDefinitionColumns() {
        DetailCodeUsageManagementMapper mapper = mock(DetailCodeUsageManagementMapper.class);
        DetailCodeUsageManagementService usageService = new DetailCodeUsageManagementService(mapper);
        when(mapper.findCodeGroupById("PROC_STATUS")).thenReturn(group());
        when(mapper.findDetailCodeUsageSetting("PROC_STATUS", "OPEN")).thenReturn(activeRow());
        DetailCodeUsageSettingsRequest request = new DetailCodeUsageSettingsRequest(List.of(new DetailCodeUsageSettingsRequest.Item(
                "OPEN", "변경된 코드명", "N", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-08-19"), "정의 변경 시도")));

        assertThatThrownBy(() -> usageService.saveDetailCodeUsageSettings("PROC_STATUS", request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("코드 사용 설정 저장 요청");
        verify(mapper, never()).updateDetailCodeUsageSetting(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void endedOrInactiveCodesAreExcludedFromNewInputOptionsButCodeNamesRemainInUsageRows() {
        DetailCodeUsageManagementMapper mapper = mock(DetailCodeUsageManagementMapper.class);
        DetailCodeUsageManagementService usageService = new DetailCodeUsageManagementService(mapper);
        when(mapper.findCodeGroupById("PROC_STATUS")).thenReturn(group());
        when(mapper.listDetailCodeUsageSettings("PROC_STATUS", 10, 0)).thenReturn(List.of(activeRow(), endedRow()));
        when(mapper.countDetailCodeUsageSettings("PROC_STATUS")).thenReturn(2L);
        when(mapper.listSelectableDetailCodesForNewInput("PROC_STATUS")).thenReturn(List.of(activeRow()));

        DetailCodeUsageSearchResponse response = usageService.listDetailCodeUsageSettings("PROC_STATUS", 0, 10);

        assertThat(response.settings()).extracting(DetailCodeUsageRow::codeName).contains("종료됨");
        assertThat(response.selectableOptions()).extracting(DetailCodeUsageRow::codeValue).containsExactly("OPEN");
    }

    @Test
    void forbiddenDetailCodeUsageRequestReturns403ApiError() throws Exception {
        when(service.listDetailCodeUsageSettings("PROC_STATUS", 0, 10)).thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/admin/code-groups/PROC_STATUS/codes/usage-settings"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private DetailCodeUsageRow activeRow() {
        return new DetailCodeUsageRow("PROC_STATUS", "OPEN", "진행", "Y",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2099-12-31"), "ACTIVE", "초기", 1L,
                LocalDate.parse("2026-08-19").atStartOfDay(), true);
    }

    private DetailCodeUsageRow endedRow() {
        return new DetailCodeUsageRow("PROC_STATUS", "ENDED", "종료됨", "N",
                LocalDate.parse("2020-01-01"), LocalDate.parse("2026-08-19"), "INACTIVE", "신규 입력 종료", 1L,
                LocalDate.parse("2026-08-19").atStartOfDay(), false);
    }

    private CodeGroupRow group() {
        return new CodeGroupRow("PROC_STATUS", "처리상태", "처리상태 코드", "시스템관리", "Y", "ACTIVE", 2,
                LocalDate.parse("2026-01-01").atStartOfDay(), LocalDate.parse("2026-08-19").atStartOfDay());
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
