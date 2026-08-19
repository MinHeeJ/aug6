package kr.ac.knue.commonfoundation.menus;

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
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import kr.ac.knue.commonfoundation.permissions.MenuItem;
import kr.ac.knue.commonfoundation.permissions.MenuRow;
import kr.ac.knue.commonfoundation.permissions.PermissionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MenuUsageManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MenuUsageManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean MenuUsageManagementService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void listMenuUsageSettingsReturnsMenuUseYnAndExposurePeriodRows() throws Exception {
        when(service.listMenuUsageSettings(new MenuUsageSearchCriteria(0, 10, "메뉴", null)))
                .thenReturn(new MenuUsageSearchResponse(List.of(row("Y")), 0, 10, 1));

        mockMvc.perform(get("/api/admin/menus/usage-settings")
                        .param("filter", "메뉴")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settings[0].menuId").value(133))
                .andExpect(jsonPath("$.data.settings[0].topMenuName").value("시스템 관리"))
                .andExpect(jsonPath("$.data.settings[0].middleMenuName").value("메뉴 관리"))
                .andExpect(jsonPath("$.data.settings[0].menuName").value("메뉴 사용 관리"))
                .andExpect(jsonPath("$.data.settings[0].url").value("/admin/menu-usage"))
                .andExpect(jsonPath("$.data.settings[0].systemUseYn").value("Y"))
                .andExpect(jsonPath("$.data.settings[0].exposureStartAt").value("2026-08-19T00:00:00"))
                .andExpect(jsonPath("$.data.settings[0].exposureEndAt").value("2026-12-31T23:59:59"));
    }

    @Test
    void saveMenuUsageSettingsPersistsUsagePeriodAndReturnsUpdatedRows() throws Exception {
        when(service.saveMenuUsageSettings(any(MenuUsageSettingsRequest.class), eq(1L)))
                .thenReturn(List.of(row("N")));

        mockMvc.perform(put("/api/admin/menus/usage-settings")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"menuId":133,"systemUseYn":"N","exposureStartAt":"2026-08-19T00:00:00","exposureEndAt":"2026-12-31T23:59:59","changeReason":"운영 중지"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].menuId").value(133))
                .andExpect(jsonPath("$.data[0].systemUseYn").value("N"))
                .andExpect(jsonPath("$.data[0].changeReason").value("운영 중지"));
    }

    @Test
    void saveMenuUsageSettingsRequiresFieldLevelValidation() throws Exception {
        mockMvc.perform(put("/api/admin/menus/usage-settings")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").exists());
    }

    @Test
    void saveMenuUsageSettingsRequiresAuthenticatedAdminBeforePersistenceSideEffects() throws Exception {
        mockMvc.perform(put("/api/admin/menus/usage-settings")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"menuId":133,"systemUseYn":"Y","exposureStartAt":"2026-08-19T00:00:00","exposureEndAt":"2026-12-31T23:59:59","changeReason":"권한 검증"}]}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveMenuUsageSettings(any(), any());
    }

    @Test
    void saveMenuUsageSettingsRejectsEndBeforeStartAndDoesNotUpdateRows() {
        MenuUsageMapper mapper = mock(MenuUsageMapper.class);
        MenuUsageManagementService menuUsageService = new MenuUsageManagementService(mapper);
        MenuUsageSettingsRequest request = new MenuUsageSettingsRequest(List.of(new MenuUsageSettingsRequest.Item(
                133L, "Y", LocalDateTime.parse("2026-08-20T00:00:00"), LocalDateTime.parse("2026-08-19T23:59:59"), "기간 검증")));

        assertThatThrownBy(() -> menuUsageService.saveMenuUsageSettings(request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("메뉴 사용 설정 저장 요청");
        verify(mapper, never()).upsertMenuUsageSetting(any(), any(), any(), any(), any(), any());
    }

    @Test
    void saveMenuUsageSettingsDoesNotMutateMenuStructureExecutionOrPermissionTables() {
        MenuUsageMapper mapper = mock(MenuUsageMapper.class);
        MenuUsageManagementService menuUsageService = new MenuUsageManagementService(mapper);
        MenuUsageSettingsRequest request = new MenuUsageSettingsRequest(List.of(new MenuUsageSettingsRequest.Item(
                133L, "N", LocalDateTime.parse("2026-08-19T00:00:00"), LocalDateTime.parse("2026-12-31T23:59:59"), "운영 중지")));
        when(mapper.existsMenu(133L)).thenReturn(1);
        when(mapper.findMenuUsageSetting(133L)).thenReturn(row("N"));

        menuUsageService.saveMenuUsageSettings(request, 1L);

        verify(mapper).upsertMenuUsageSetting(133L, "N", LocalDateTime.parse("2026-08-19T00:00:00"),
                LocalDateTime.parse("2026-12-31T23:59:59"), 1L, "운영 중지");
        verify(mapper, never()).updateMenuStructure(any(), any(), any(), any());
        verify(mapper, never()).updateMenuExecution(any(), any(), any(), any());
        verify(mapper, never()).updateMenuPermission(any(), any(), any(), any());
    }

    @Test
    void inactiveOrOutOfExposureMenuIsHiddenAndBlocksDirectAccess() {
        PermissionMapper mapper = mock(PermissionMapper.class);
        EffectivePermissionService permissionService = new EffectivePermissionService(mapper);
        when(mapper.isMenuRouteExposed("/admin/menu-usage")).thenReturn(0);
        when(mapper.findActiveMenus()).thenReturn(List.of(
                new MenuRow(100L, null, "시스템 관리", null, null, "settings", 1),
                new MenuRow(130L, 100L, "메뉴 관리", null, null, "menu", 3)));

        assertThat(permissionService.canAccess(1L, List.of("R09"), "/admin/menu-usage")).isFalse();
        List<MenuItem> visibleMenus = permissionService.visibleMenus(1L, List.of("R09"));
        assertThat(visibleMenus).noneMatch(item -> item.children().stream()
                .flatMap(child -> child.children().stream())
                .anyMatch(leaf -> "/admin/menu-usage".equals(leaf.url())));
    }

    @Test
    void forbiddenMenuUsageRequestReturns403ApiError() throws Exception {
        when(service.listMenuUsageSettings(any(MenuUsageSearchCriteria.class))).thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/admin/menus/usage-settings"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private MenuUsageRow row(String systemUseYn) {
        return new MenuUsageRow(133L, 130L, "시스템 관리", "메뉴 관리", "메뉴 사용 관리",
                "SCR-MENU-USAGE-MGMT", "/admin/menu-usage", systemUseYn,
                LocalDateTime.parse("2026-08-19T00:00:00"), LocalDateTime.parse("2026-12-31T23:59:59"),
                "ACTIVE", "운영 중지", 1L, LocalDateTime.parse("2026-08-19T09:00:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
