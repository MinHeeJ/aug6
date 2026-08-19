package kr.ac.knue.commonfoundation.permissions;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MenuPermissionManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MenuPermissionManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean MenuPermissionManagementService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void listMenuPermissionsReturnsRoleOrganizationAndUserMatrixRows() throws Exception {
        when(service.listMenuPermissions(new MenuPermissionSearchCriteria(0, 10, "ROLE", "R09", null)))
                .thenReturn(new MenuPermissionSearchResponse(List.of(row("ROLE", "R09", "ALLOW")), 0, 10, 1));

        mockMvc.perform(get("/api/admin/menu-permissions")
                        .param("targetType", "ROLE")
                        .param("targetId", "R09")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.permissions[0].targetType").value("ROLE"))
                .andExpect(jsonPath("$.data.permissions[0].targetId").value("R09"))
                .andExpect(jsonPath("$.data.permissions[0].topMenuName").value("시스템 관리"))
                .andExpect(jsonPath("$.data.permissions[0].middleMenuName").value("역할·권한 관리"))
                .andExpect(jsonPath("$.data.permissions[0].screenMenuName").value("메뉴 권한 관리"))
                .andExpect(jsonPath("$.data.permissions[0].accessAllowed").value("ALLOW"));
    }

    @Test
    void saveMenuPermissionsPersistsPermissionAndReturnsUpdatedRowWithMetadata() throws Exception {
        when(service.saveMenuPermission(any(MenuPermissionSaveRequest.class), eq(1L)))
                .thenReturn(row("ROLE", "R09", "DENY"));

        mockMvc.perform(put("/api/admin/menu-permissions")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"ROLE","targetId":"R09","menuId":123,"accessAllowed":"DENY","changeReason":"메뉴 접근 제한"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetType").value("ROLE"))
                .andExpect(jsonPath("$.data.targetId").value("R09"))
                .andExpect(jsonPath("$.data.menuId").value(123))
                .andExpect(jsonPath("$.data.accessAllowed").value("DENY"))
                .andExpect(jsonPath("$.data.changeReason").value("메뉴 접근 제한"));
    }

    @Test
    void saveMenuPermissionsRequiresFieldLevelValidation() throws Exception {
        mockMvc.perform(put("/api/admin/menu-permissions")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").exists());
    }

    @Test
    void saveMenuPermissionsRequiresAuthBeforePermissionTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/menu-permissions")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"ROLE","targetId":"R09","menuId":123,"accessAllowed":"ALLOW","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveMenuPermission(any(), any());
    }

    @Test
    void saveMenuPermissionsRejectsUnknownTargetWithoutPersistenceSideEffects() {
        MenuPermissionMapper mapper = mock(MenuPermissionMapper.class);
        MenuPermissionManagementService menuPermissionService = new MenuPermissionManagementService(mapper);
        when(mapper.existsTarget("ROLE", "R77")).thenReturn(0);

        assertThatThrownBy(() -> menuPermissionService.saveMenuPermission(new MenuPermissionSaveRequest("ROLE", "R77", 123L, "ALLOW", "검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("대상");
        verify(mapper, never()).upsertPermission(any(), any(), any(), any(), any(), any());
    }

    @Test
    void effectivePermissionUsesUserOrganizationRolePriorityAndDenyAtSamePriority() {
        EffectivePermissionService permissionService = new EffectivePermissionService(null);

        assertThat(permissionService.resolveAllowed(List.of(
                new PermissionRule("ROLE", "DENY"),
                new PermissionRule("ORGANIZATION", "ALLOW"),
                new PermissionRule("USER", "ALLOW")))).isTrue();
        assertThat(permissionService.resolveAllowed(List.of(
                new PermissionRule("ROLE", "ALLOW"),
                new PermissionRule("ORGANIZATION", "ALLOW"),
                new PermissionRule("USER", "DENY")))).isFalse();
    }

    @Test
    void saveMenuPermissionsSurfacesBusinessRuleFieldError() throws Exception {
        when(service.saveMenuPermission(any(MenuPermissionSaveRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("메뉴 접근권한 저장 요청이 올바르지 않습니다.",
                        List.of(new ValidationError("menuId", "존재하지 않는 메뉴입니다."))));

        mockMvc.perform(put("/api/admin/menu-permissions")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"ROLE","targetId":"R09","menuId":9999,"accessAllowed":"ALLOW","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("menuId"));
    }

    @Test
    void forbiddenMenuPermissionRequestReturns403ApiError() throws Exception {
        when(service.listMenuPermissions(any(MenuPermissionSearchCriteria.class))).thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/admin/menu-permissions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private MenuPermissionRow row(String targetType, String targetId, String accessAllowed) {
        return new MenuPermissionRow(77L, targetType, targetId, targetId, 123L, "시스템 관리", "역할·권한 관리",
                "메뉴 권한 관리", "SCR-MENU-PERMISSION-MGMT", "/admin/menu-permissions", accessAllowed,
                "ACTIVE", "메뉴 접근 제한", LocalDateTime.parse("2026-08-18T09:00:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
