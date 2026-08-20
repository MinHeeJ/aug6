package kr.ac.knue.commonfoundation.operations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
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

@WebMvcTest(CommonOperationsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CommonOperationsApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean CommonOperationsService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void menuExposureCanBeSavedAndReadBack() throws Exception {
        when(service.listMenuExposureSettings()).thenReturn(List.of(new MenuExposureSetting(131L, "메뉴 구조 관리", "Y", LocalDateTime.parse("2026-08-01T09:00:00"), null, "유지", LocalDateTime.parse("2026-08-20T09:00:00"))));
        when(service.saveMenuExposureSettings(any(MenuExposureSaveRequest.class), eq(1L))).thenReturn(List.of(new MenuExposureSetting(131L, "메뉴 구조 관리", "N", LocalDateTime.parse("2026-08-01T09:00:00"), LocalDateTime.parse("2026-08-19T18:00:00"), "운영 중지", LocalDateTime.parse("2026-08-20T09:10:00"))));

        mockMvc.perform(get("/api/admin/menus/exposure").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].menuId").value(131))
                .andExpect(jsonPath("$.data[0].systemUseYn").value("Y"));

        mockMvc.perform(put("/api/admin/menus/exposure-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settings":[{"menuId":131,"systemUseYn":"N","exposureStartAt":"2026-08-01T09:00:00","exposureEndAt":"2026-08-19T18:00:00"}],"changeReason":"운영 중지"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].systemUseYn").value("N"));
    }

    @Test
    void detailCodeUsageCanBeUpdatedWithoutChangingCodeDefinition() throws Exception {
        when(service.listDetailCodeUsageSettings("COMMON_STATUS")).thenReturn(List.of(new DetailCodeUsageSetting("COMMON_STATUS", "ACTIVE", "활성", "Y", LocalDate.parse("2026-01-01"), null, null, LocalDateTime.parse("2026-08-20T09:00:00"))));
        when(service.updateDetailCodeUsageSetting(eq("COMMON_STATUS"), eq("ACTIVE"), any(DetailCodeUsageUpdateRequest.class), eq(1L)))
                .thenReturn(new DetailCodeUsageSetting("COMMON_STATUS", "ACTIVE", "활성", "N", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-08-19"), "종료", LocalDateTime.parse("2026-08-20T09:10:00")));

        mockMvc.perform(get("/api/admin/code-groups/COMMON_STATUS/codes-usage").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].codeValue").value("ACTIVE"));

        mockMvc.perform(put("/api/admin/code-groups/COMMON_STATUS/codes/ACTIVE/usage")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemUseYn":"N","validStartDate":"2026-01-01","validEndDate":"2026-08-19","changeReason":"종료"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codeName").value("활성"))
                .andExpect(jsonPath("$.data.systemUseYn").value("N"));
    }

    @Test
    void getAdminSystemSettingsCommonReturnsPersistedSettingRows() throws Exception {
        when(service.listCommonSettings()).thenReturn(List.of(new CommonSettingRow("PAGE_SIZE_DEFAULT", "50", "ROWS", "시드", LocalDateTime.parse("2026-08-20T09:00:00"))));

        mockMvc.perform(get("/api/admin/system-settings/common").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].settingKey").value("PAGE_SIZE_DEFAULT"))
                .andExpect(jsonPath("$.data[0].settingValue").value("50"));
    }

    @Test
    void getCodeGroupCodeOptionsReturnsPortalActiveOptions() throws Exception {
        when(service.listActiveDetailCodeOptions("COMMON_STATUS")).thenReturn(List.of(new DetailCodeUsageSetting("COMMON_STATUS", "ACTIVE", "활성", "Y", LocalDate.parse("2026-01-01"), null, null, LocalDateTime.parse("2026-08-20T09:00:00"))));

        mockMvc.perform(get("/api/code-groups/COMMON_STATUS/codes/options").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].groupId").value("COMMON_STATUS"))
                .andExpect(jsonPath("$.data[0].codeValue").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].systemUseYn").value("Y"));
    }

    @Test
    void getSystemSettingsDefaultSearchYearReturnsPersistedDefaultYear() throws Exception {
        when(service.getDefaultSearchYear()).thenReturn(new DefaultSearchYearResponse(2026));

        mockMvc.perform(get("/api/system-settings/default-search-year").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.defaultSearchYear").value(2026));
    }

    @Test
    void putCommonValuesPersistsCommonSettingsTableSideEffect() throws Exception {
        when(service.saveCommonSettings(any(CommonSettingsSaveRequest.class), eq(1L))).thenReturn(List.of(new CommonSettingRow("PAGE_SIZE_DEFAULT", "100", "ROWS", "운영값 변경", LocalDateTime.parse("2026-08-20T09:15:00"))));

        mockMvc.perform(put("/api/admin/system-settings/common-values")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settings":[{"settingKey":"PAGE_SIZE_DEFAULT","settingValue":"100","settingUnit":"ROWS"}],"changeReason":"운영값 변경"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].settingKey").value("PAGE_SIZE_DEFAULT"))
                .andExpect(jsonPath("$.data[0].settingValue").value("100"));
        verify(service).saveCommonSettings(any(CommonSettingsSaveRequest.class), eq(1L));
    }

    @Test
    void putCommonValuesRequiresAuthenticatedActorBeforeCommonSettingsSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/system-settings/common-values")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settings":[{"settingKey":"PAGE_SIZE_DEFAULT","settingValue":"100","settingUnit":"ROWS"}],"changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveCommonSettings(any(), any());
    }

    @Test
    void commonSettingsRejectDuplicateOrScopedRowsBeforePersistence() throws Exception {
        mockMvc.perform(put("/api/admin/system-settings/common-values")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settings":[{"settingKey":"PAGE_SIZE_DEFAULT","settingValue":"50"},{"settingKey":"PAGE_SIZE_DEFAULT","settingValue":"100"}],"changeReason":"중복 검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("settingKey"));
        verify(service, never()).saveCommonSettings(any(), any());

        mockMvc.perform(put("/api/admin/system-settings/common-values")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settings":[{"settingKey":"PAGE_SIZE_DEFAULT","settingValue":"50","userId":1}],"changeReason":"scope 금지"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("userId"));
    }

    @Test
    void baseYearSaveAndPreparationHistoryAreExposed() throws Exception {
        when(service.listBaseYearSettings()).thenReturn(List.of(new BaseYearSetting(2026, 2026, 2026, "N", "N", "시드", LocalDateTime.parse("2026-08-20T09:00:00"))));
        when(service.saveBaseYearSettings(any(BaseYearSaveRequest.class), eq(1L))).thenReturn(new BaseYearSetting(2027, 2027, 2027, "Y", "N", "기준연도 변경", LocalDateTime.parse("2026-08-20T09:10:00")));
        when(service.prepareBaseYearStandards(eq(2027), any(StandardPreparationRequest.class), eq(1L))).thenReturn(new StandardPreparationHistory(1L, 2027, "Y", "N", "기준정보 준비", LocalDateTime.parse("2026-08-20T09:11:00"), 1L));

        mockMvc.perform(get("/api/admin/system-settings/base-years").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].defaultSearchYear").value(2026));

        mockMvc.perform(put("/api/admin/system-settings/base-year-current")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseYear":2027,"currentEvaluationYear":2027,"defaultSearchYear":2027,"copyRequestedYn":"Y","initializeRequestedYn":"N","changeReason":"기준연도 변경"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultSearchYear").value(2027));

        mockMvc.perform(post("/api/admin/system-settings/base-years/2027/standards-preparation")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"copyRequestedYn":"Y","initializeRequestedYn":"N","changeReason":"기준정보 준비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseYear").value(2027));
    }

    @Test
    void putBaseYearCurrentPersistsBaseYearSettingsStateTransition() throws Exception {
        when(service.saveBaseYearSettings(any(BaseYearSaveRequest.class), eq(1L))).thenReturn(new BaseYearSetting(2028, 2028, 2028, "N", "Y", "기준연도 저장", LocalDateTime.parse("2026-08-20T09:20:00")));

        mockMvc.perform(put("/api/admin/system-settings/base-year-current")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseYear":2028,"currentEvaluationYear":2028,"defaultSearchYear":2028,"copyRequestedYn":"N","initializeRequestedYn":"Y","changeReason":"기준연도 저장"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseYear").value(2028))
                .andExpect(jsonPath("$.data.defaultSearchYear").value(2028))
                .andExpect(jsonPath("$.data.initializeRequestedYn").value("Y"));
        verify(service).saveBaseYearSettings(any(BaseYearSaveRequest.class), eq(1L));
    }

    @Test
    void putBaseYearCurrentRequiresAuthAndValidationBeforeBaseYearSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/system-settings/base-year-current")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseYear":2028,"currentEvaluationYear":2028,"defaultSearchYear":2028,"copyRequestedYn":"N","initializeRequestedYn":"Y","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(put("/api/admin/system-settings/base-year-current")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(service, never()).saveBaseYearSettings(any(), any());
    }

    @Test
    void postStandardsPreparationPersistsPreparationHistoryStateTransition() throws Exception {
        when(service.prepareBaseYearStandards(eq(2028), any(StandardPreparationRequest.class), eq(1L)))
                .thenReturn(new StandardPreparationHistory(3L, 2028, "Y", "Y", "기준정보 준비", LocalDateTime.parse("2026-08-20T09:21:00"), 1L));

        mockMvc.perform(post("/api/admin/system-settings/base-years/2028/standards-preparation")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"copyRequestedYn":"Y","initializeRequestedYn":"Y","changeReason":"기준정보 준비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preparationId").value(3))
                .andExpect(jsonPath("$.data.baseYear").value(2028))
                .andExpect(jsonPath("$.data.preparedBy").value(1));
        verify(service).prepareBaseYearStandards(eq(2028), any(StandardPreparationRequest.class), eq(1L));
    }

    @Test
    void postStandardsPreparationRequiresAuthBusinessAndValidationBeforeHistorySideEffect() throws Exception {
        mockMvc.perform(post("/api/admin/system-settings/base-years/2028/standards-preparation")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"copyRequestedYn":"Y","initializeRequestedYn":"Y","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(post("/api/admin/system-settings/base-years/2028/standards-preparation")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        when(service.prepareBaseYearStandards(eq(2028), any(StandardPreparationRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("기준정보 준비 업무 규칙 위반", List.of(new ValidationError("baseYear", "준비할 기준연도를 확인할 수 없습니다."))));
        mockMvc.perform(post("/api/admin/system-settings/base-years/2028/standards-preparation")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"copyRequestedYn":"Y","initializeRequestedYn":"Y","changeReason":"업무 규칙 검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("baseYear"));
    }

    @Test
    void putDetailCodeUsageRequiresAuthBusinessAndValidationBeforeDetailCodesSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/code-groups/COMMON_STATUS/codes/ACTIVE/usage")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemUseYn":"N","validStartDate":"2026-01-01","validEndDate":"2026-08-19","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(put("/api/admin/code-groups/COMMON_STATUS/codes/ACTIVE/usage")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        when(service.updateDetailCodeUsageSetting(eq("COMMON_STATUS"), eq("ACTIVE"), any(DetailCodeUsageUpdateRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("상세코드 사용 업무 규칙 위반", List.of(new ValidationError("validEndDate", "시작일보다 빠를 수 없습니다."))));
        mockMvc.perform(put("/api/admin/code-groups/COMMON_STATUS/codes/ACTIVE/usage")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemUseYn":"N","validStartDate":"2026-08-20","validEndDate":"2026-08-19","changeReason":"업무 규칙 검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("validEndDate"));
    }

    @Test
    void putMenuExposureSaveRequiresAuthBusinessAndValidationBeforePersistedStateTransition() throws Exception {
        mockMvc.perform(put("/api/admin/menus/exposure-save")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settings":[{"menuId":131,"systemUseYn":"N","exposureStartAt":"2026-08-01T09:00:00"}],"changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(put("/api/admin/menus/exposure-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        when(service.saveMenuExposureSettings(any(MenuExposureSaveRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("메뉴 노출 업무 규칙 위반", List.of(new ValidationError("menuId", "메뉴를 찾을 수 없습니다."))));
        mockMvc.perform(put("/api/admin/menus/exposure-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settings":[{"menuId":999,"systemUseYn":"N","exposureStartAt":"2026-08-01T09:00:00"}],"changeReason":"업무 규칙 검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("menuId"));
    }

    @Test
    void serviceValidationPreservesRowsOnBusinessErrors() {
        CommonOperationsMapper mapper = org.mockito.Mockito.mock(CommonOperationsMapper.class);
        CommonOperationsService operationsService = new CommonOperationsService(mapper);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> operationsService.saveCommonSettings(new CommonSettingsSaveRequest(List.of(new CommonSettingInput("PAGE_SIZE_DEFAULT", "50", "ROWS", 1L, null)), "scope"), 1L))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).upsertCommonSetting(any(), any(), any(), any(), any());
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
