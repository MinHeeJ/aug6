package kr.ac.knue.commonfoundation.menus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MenuStructureManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LocalizedMenuTreeContractTest {
    @Autowired MockMvc mockMvc;
    @MockBean MenuStructureManagementService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void getLocalizedMenuTreeReturnsEnglishDisplayNameInApiResponseEnvelope() throws Exception {
        when(service.getLocalizedMenuTree("en", admin)).thenReturn(new LocalizedMenuTreeResponse("en", List.of(
                localizedNode(100L, null, "시스템 관리", "System Management", "System Management", List.of(
                        localizedNode(130L, 100L, "메뉴 관리", "Menu Management", "Menu Management", List.of()))))));

        mockMvc.perform(get("/api/admin/menus/localized-tree")
                        .param("lang", "en")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lang").value("en"))
                .andExpect(jsonPath("$.data.rows[0].menuName").value("시스템 관리"))
                .andExpect(jsonPath("$.data.rows[0].menuNameEn").value("System Management"))
                .andExpect(jsonPath("$.data.rows[0].displayName").value("System Management"))
                .andExpect(jsonPath("$.data.rows[0].children[0].displayName").value("Menu Management"));
    }

    @Test
    void getLocalizedMenuTreeFallsBackToKoreanMenuNameWhenEnglishNameIsBlank() throws Exception {
        when(service.getLocalizedMenuTree("en", admin)).thenReturn(new LocalizedMenuTreeResponse("en", List.of(
                localizedNode(100L, null, "시스템 관리", "System Management", "System Management", List.of(
                        localizedNode(130L, 100L, "메뉴 관리", "Menu Management", "Menu Management", List.of(
                                localizedNode(131L, 130L, "메뉴 구조 관리", " ", "메뉴 구조 관리", List.of()))))))));

        mockMvc.perform(get("/api/admin/menus/localized-tree")
                        .param("lang", "en")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].children[0].children[0].menuName").value("메뉴 구조 관리"))
                .andExpect(jsonPath("$.data.rows[0].children[0].children[0].menuNameEn").value(" "))
                .andExpect(jsonPath("$.data.rows[0].children[0].children[0].displayName").value("메뉴 구조 관리"));
    }

    @Test
    void getApiAdminMenusLocalizedTreeAuthRequiredBeforeSideEffect() throws Exception {
        mockMvc.perform(get("/api/admin/menus/localized-tree")
                        .param("lang", "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).getLocalizedMenuTree(any(), any());
    }

    @Test
    void getApiAdminMenusLocalizedTreeBusinessFallbackKeepsMenuOrderAndPermissionFilter() throws Exception {
        when(service.getLocalizedMenuTree("en", admin)).thenReturn(new LocalizedMenuTreeResponse("en", List.of(
                localizedNode(100L, null, "시스템 관리", "System Management", "System Management", List.of(
                        localizedNode(130L, 100L, "메뉴 관리", "Menu Management", "Menu Management", List.of(
                                localizedNode(131L, 130L, "메뉴 구조 관리", " ", "메뉴 구조 관리", List.of()))))))));

        mockMvc.perform(get("/api/admin/menus/localized-tree")
                        .param("lang", "en")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].displayName").value("System Management"))
                .andExpect(jsonPath("$.data.rows[0].children[0].displayName").value("Menu Management"))
                .andExpect(jsonPath("$.data.rows[0].children[0].children[0].displayName").value("메뉴 구조 관리"));
    }

    @Test
    void getApiAdminMenusLocalizedTreeSideEffectFreeValidationRejectsUnsupportedLang() throws Exception {
        when(service.getLocalizedMenuTree("ja", admin)).thenThrow(
                MenuStructureManagementService.unsupportedLanguage("ja"));

        mockMvc.perform(get("/api/admin/menus/localized-tree")
                        .param("lang", "ja")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("lang"));
    }

    @Test
    void serviceBuildsLocalizedTreeWithKoreanEnglishFallbackAndSameOrder() {
        MenuStructureMapper mapper = mock(MenuStructureMapper.class);
        MenuStructureManagementService menuService = new MenuStructureManagementService(mapper);
        when(mapper.findMenusForLocalizedTree()).thenReturn(List.of(
                row(100L, null, "시스템 관리", "System Management", 1),
                row(130L, 100L, "메뉴 관리", "Menu Management", 3),
                row(131L, 130L, "메뉴 구조 관리", " ", 1),
                row(132L, 130L, "메뉴 정보 관리", null, 2)));

        LocalizedMenuTreeResponse response = menuService.getLocalizedMenuTree("en", admin);

        assertThat(response.lang()).isEqualTo("en");
        assertThat(response.rows()).extracting(MenuTreeNode::displayName).containsExactly("System Management");
        MenuTreeNode middle = response.rows().get(0).children().get(0);
        assertThat(middle.displayName()).isEqualTo("Menu Management");
        assertThat(middle.children()).extracting(MenuTreeNode::displayName)
                .containsExactly("메뉴 구조 관리", "메뉴 정보 관리");
        verify(mapper).findMenusForLocalizedTree();
    }

    @Test
    void serviceRejectsInvalidLanguageBeforeQuerySideEffect() {
        MenuStructureMapper mapper = mock(MenuStructureMapper.class);
        MenuStructureManagementService menuService = new MenuStructureManagementService(mapper);

        assertThatThrownBy(() -> menuService.getLocalizedMenuTree("ja", admin))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("지원하지 않는 언어");
        verify(mapper, never()).findMenusForLocalizedTree();
    }

    private MenuTreeNode localizedNode(Long menuId, Long parentMenuId, String menuName, String menuNameEn,
                                       String displayName, List<MenuTreeNode> children) {
        return new MenuTreeNode(menuId, parentMenuId, parentMenuId == null ? "MAIN" : "SCREEN", menuName,
                menuNameEn, displayName, 1, parentMenuId == null ? null : "SCR-MENU-STRUCTURE-MGMT",
                parentMenuId == null ? null : "/admin/menu-structure", "tree", "SYSTEM", "메뉴 구조", "Y", "ACTIVE",
                "시드", LocalDateTime.parse("2026-08-18T09:00:00"), children);
    }

    private MenuTreeRow row(Long menuId, Long parentMenuId, String menuName, String menuNameEn, int displayOrder) {
        return new MenuTreeRow(menuId, parentMenuId, parentMenuId == null ? "MAIN" : "SCREEN", menuName, menuNameEn,
                displayOrder, parentMenuId == null ? null : "SCR-MENU-STRUCTURE-MGMT",
                parentMenuId == null ? null : "/admin/menu-structure", "tree", "SYSTEM", "메뉴 구조", "Y", "ACTIVE",
                "시드", LocalDateTime.parse("2026-08-18T09:00:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
