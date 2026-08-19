package kr.ac.knue.commonfoundation.settings;

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
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommonSystemSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CommonSystemSettingsApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean CommonSystemSettingsService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void getCommonSystemSettingsReturnsFiveGlobalSettings() throws Exception {
        when(service.getCommonSystemSettings()).thenReturn(new CommonSystemSettingsResponse(List.of(
                row("SESSION_IDLE_MINUTES", "30", "minutes"),
                row("PAGE_SIZE", "20", "rows"),
                row("DEFAULT_SEARCH_PERIOD", "30", "days"),
                row("BULK_QUERY_THRESHOLD", "1000", "rows"),
                row("LONG_TASK_NOTICE_THRESHOLD", "60", "seconds")
        )));

        mockMvc.perform(get("/api/admin/system-settings/common").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settings[0].settingKey").value("SESSION_IDLE_MINUTES"))
                .andExpect(jsonPath("$.data.settings[1].settingKey").value("PAGE_SIZE"))
                .andExpect(jsonPath("$.data.settings[4].settingKey").value("LONG_TASK_NOTICE_THRESHOLD"));
    }

    @Test
    void saveCommonSystemSettingsPersistsGlobalValuesAndReturnsReloadedSettings() throws Exception {
        when(service.saveCommonSystemSettings(any(CommonSystemSettingsRequest.class), eq(1L)))
                .thenReturn(new CommonSystemSettingsResponse(List.of(row("PAGE_SIZE", "50", "rows"))));

        mockMvc.perform(put("/api/admin/system-settings/common")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settings":[{"settingKey":"PAGE_SIZE","settingValue":"50","unit":"rows","changeReason":"조회 건수 조정"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settings[0].settingKey").value("PAGE_SIZE"))
                .andExpect(jsonPath("$.data.settings[0].settingValue").value("50"))
                .andExpect(jsonPath("$.data.settings[0].unit").value("rows"));
    }

    @Test
    void saveCommonSystemSettingsRequiresAuthenticatedAdminBeforePersistenceSideEffects() throws Exception {
        mockMvc.perform(put("/api/admin/system-settings/common")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settings":[{"settingKey":"PAGE_SIZE","settingValue":"20","unit":"rows","changeReason":"권한 검증"}]}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveCommonSystemSettings(any(), any());
    }

    @Test
    void saveCommonSystemSettingsRejectsUnknownKeyAndUserSpecificPayload() {
        CommonSystemSettingsMapper mapper = mock(CommonSystemSettingsMapper.class);
        CommonSystemSettingsService settingsService = new CommonSystemSettingsService(mapper);
        CommonSystemSettingsRequest request = new CommonSystemSettingsRequest(List.of(
                new CommonSystemSettingsRequest.Item("PAGE_SIZE", "20", "rows", 99L, "개별 사용자 설정 시도"),
                new CommonSystemSettingsRequest.Item("USER_PAGE_SIZE", "20", "rows", null, "허용되지 않은 키")
        ));

        assertThatThrownBy(() -> settingsService.saveCommonSystemSettings(request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("공통 환경설정 저장 요청");
        verify(mapper, never()).upsertCommonSystemSetting(any(), any(), any(), any(), any());
    }

    @Test
    void saveCommonSystemSettingsAppliesOqNumericGateForKnownNumericSettings() {
        CommonSystemSettingsMapper mapper = mock(CommonSystemSettingsMapper.class);
        CommonSystemSettingsService settingsService = new CommonSystemSettingsService(mapper);
        CommonSystemSettingsRequest request = new CommonSystemSettingsRequest(List.of(
                new CommonSystemSettingsRequest.Item("PAGE_SIZE", "스무건", "rows", null, "숫자 검증")
        ));

        assertThatThrownBy(() -> settingsService.saveCommonSystemSettings(request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .satisfies(error -> assertThat(((BusinessValidationException) error).fields())
                        .anyMatch(field -> field.field().contains("settingValue")));
        verify(mapper, never()).upsertCommonSystemSetting(any(), any(), any(), any(), any());
    }

    private CommonSystemSettingRow row(String key, String value, String unit) {
        return new CommonSystemSettingRow(key, value, unit, "변경 사유", 1L, LocalDateTime.parse("2026-08-19T09:00:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
