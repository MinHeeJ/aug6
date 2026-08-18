package kr.ac.knue.commonfoundation.menus;

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

@WebMvcTest(MenuStructureManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MenuStructureManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean MenuStructureManagementService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void getMenuTreeReturnsOrderedHierarchy() throws Exception {
        when(service.getMenuTree(null)).thenReturn(List.of(menuNode(100L, null, "시스템 관리", List.of(
                menuNode(130L, 100L, "메뉴 관리", List.of(menuNode(131L, 130L, "메뉴 구조 관리", List.of())))))));

        mockMvc.perform(get("/api/admin/menus/tree").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].menuId").value(100))
                .andExpect(jsonPath("$.data[0].children[0].menuId").value(130))
                .andExpect(jsonPath("$.data[0].children[0].children[0].screenId").value("SCR-MENU-STRUCTURE-MGMT"));
    }

    @Test
    void updateMenuParentPersistsParentAndReturnsChangedNodeWithMetadata() throws Exception {
        when(service.updateMenuParent(eq(131L), any(MenuParentUpdateRequest.class), eq(1L)))
                .thenReturn(new MenuTreeNode(131L, 120L, "SCREEN", "메뉴 구조 관리", 4, "SCR-MENU-STRUCTURE-MGMT",
                        "/admin/menu-structure", "tree", "SYSTEM", "메뉴 부모와 정렬 관리", "Y", "ACTIVE",
                        "메뉴 재배치", LocalDateTime.parse("2026-08-18T09:00:00"), List.of()));

        mockMvc.perform(put("/api/admin/menus/131/parent")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentMenuId":120,"changeReason":"메뉴 재배치"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuId").value(131))
                .andExpect(jsonPath("$.data.parentMenuId").value(120))
                .andExpect(jsonPath("$.data.changeReason").value("메뉴 재배치"));
    }

    @Test
    void reorderMenusPersistsSiblingDisplayOrder() throws Exception {
        when(service.reorderMenus(any(MenuReorderRequest.class), eq(1L))).thenReturn(List.of(
                new MenuTreeNode(131L, 130L, "SCREEN", "메뉴 구조 관리", 1, "SCR-MENU-STRUCTURE-MGMT", "/admin/menu-structure", "tree", "SYSTEM", "메뉴 부모와 정렬 관리", "Y", "ACTIVE", "순서 변경", LocalDateTime.parse("2026-08-18T09:00:00"), List.of()),
                new MenuTreeNode(132L, 130L, "SCREEN", "메뉴 정보 관리", 2, "SCR-MENU-INFO-MGMT", "/admin/menu-info", "file-cog", "SYSTEM", "메뉴 실행 정보 관리", "Y", "ACTIVE", "순서 변경", LocalDateTime.parse("2026-08-18T09:00:00"), List.of())));

        mockMvc.perform(put("/api/admin/menus/reorder")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentMenuId":130,"orderedMenuIds":[131,132],"changeReason":"순서 변경"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].displayOrder").value(1))
                .andExpect(jsonPath("$.data[1].displayOrder").value(2));
    }

    @Test
    void updateMenuParentRequiresFieldLevelValidation() throws Exception {
        mockMvc.perform(put("/api/admin/menus/131/parent")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").exists());
    }

    @Test
    void reorderMenusRequiresFieldValidationBeforeTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/menus/reorder")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").exists());
        verify(service, never()).reorderMenus(any(), any());
    }

    @Test
    void menuStructureMutationsRequireAuthBeforeTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/menus/131/parent")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentMenuId":120,"changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(put("/api/admin/menus/reorder")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentMenuId":130,"orderedMenuIds":[131,132],"changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).updateMenuParent(any(), any(), any());
        verify(service, never()).reorderMenus(any(), any());
    }

    @Test
    void reorderMenusBusinessRulePreventsSiblingOrderTableSideEffect() throws Exception {
        when(service.reorderMenus(any(MenuReorderRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("메뉴 순서 변경 업무 규칙 위반",
                        List.of(new ValidationError("orderedMenuIds", "같은 부모의 메뉴만 정렬할 수 있습니다."))));

        mockMvc.perform(put("/api/admin/menus/reorder")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentMenuId":130,"orderedMenuIds":[131,999],"changeReason":"업무 규칙 검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("orderedMenuIds"));
    }

    @Test
    void updateMenuParentRejectsSelfParentWithoutPersistenceSideEffects() {
        MenuStructureMapper mapper = mock(MenuStructureMapper.class);
        MenuStructureManagementService menuService = new MenuStructureManagementService(mapper);
        when(mapper.existsMenu(131L)).thenReturn(1);
        when(mapper.existsMenu(130L)).thenReturn(1);

        assertThatThrownBy(() -> menuService.updateMenuParent(131L, new MenuParentUpdateRequest(131L, "자기부모 검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("부모");
        verify(mapper, never()).updateParent(any(), any(), any(), any());
    }

    @Test
    void updateMenuParentRejectsDescendantParentWithoutPersistenceSideEffects() {
        MenuStructureMapper mapper = mock(MenuStructureMapper.class);
        MenuStructureManagementService menuService = new MenuStructureManagementService(mapper);
        when(mapper.existsMenu(130L)).thenReturn(1);
        when(mapper.existsMenu(131L)).thenReturn(1);
        when(mapper.isDescendant(130L, 131L)).thenReturn(1);

        assertThatThrownBy(() -> menuService.updateMenuParent(130L, new MenuParentUpdateRequest(131L, "순환 검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("순환");
        verify(mapper, never()).updateParent(any(), any(), any(), any());
    }

    @Test
    void forbiddenMenuStructureRequestReturns403ApiError() throws Exception {
        when(service.getMenuTree(null)).thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/admin/menus/tree"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private MenuTreeNode menuNode(Long menuId, Long parentMenuId, String menuName, List<MenuTreeNode> children) {
        return new MenuTreeNode(menuId, parentMenuId, parentMenuId == null ? "MAIN" : "SCREEN", menuName, 1,
                parentMenuId != null && menuId == 131L ? "SCR-MENU-STRUCTURE-MGMT" : null,
                parentMenuId != null && menuId == 131L ? "/admin/menu-structure" : null,
                "tree", "SYSTEM", "메뉴 구조", "Y", "ACTIVE", "시드", LocalDateTime.parse("2026-08-18T09:00:00"), children);
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
