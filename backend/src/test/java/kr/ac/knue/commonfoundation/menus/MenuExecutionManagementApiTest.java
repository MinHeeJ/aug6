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

@WebMvcTest(MenuExecutionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MenuExecutionManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean MenuExecutionService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void getMenuExecutionReturnsMenuNameScreenUrlIconBusinessCategoryAndDescription() throws Exception {
        when(service.getMenuExecution(132L)).thenReturn(row("SCR-MENU-INFO-MGMT", "/admin/menu-info", "file-cog"));

        mockMvc.perform(get("/api/admin/menus/132/execution").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.menuId").value(132))
                .andExpect(jsonPath("$.data.menuName").value("메뉴 정보 관리"))
                .andExpect(jsonPath("$.data.screenId").value("SCR-MENU-INFO-MGMT"))
                .andExpect(jsonPath("$.data.url").value("/admin/menu-info"))
                .andExpect(jsonPath("$.data.icon").value("file-cog"))
                .andExpect(jsonPath("$.data.businessCategory").value("SYSTEM"))
                .andExpect(jsonPath("$.data.description").value("메뉴 실행 정보 관리"));
    }

    @Test
    void updateMenuExecutionPersistsExecutionInfoAndReturnsUpdatedRowWithSideEffectMetadata() throws Exception {
        when(service.updateMenuExecution(eq(132L), any(MenuExecutionRequest.class), eq(1L)))
                .thenReturn(row("SCR-MENU-INFO-UPDATED", "/admin/menu-info", "settings"));

        mockMvc.perform(put("/api/admin/menus/132/execution")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"menuName":"메뉴 정보 관리","screenId":"SCR-MENU-INFO-UPDATED","url":"/admin/menu-info","icon":"settings","businessCategory":"SYSTEM","description":"실행 정보 갱신","changeReason":"화면 연결 갱신"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuId").value(132))
                .andExpect(jsonPath("$.data.screenId").value("SCR-MENU-INFO-UPDATED"))
                .andExpect(jsonPath("$.data.url").value("/admin/menu-info"))
                .andExpect(jsonPath("$.data.icon").value("settings"))
                .andExpect(jsonPath("$.data.updatedBy").value(1))
                .andExpect(jsonPath("$.data.changeReason").value("화면 연결 갱신"));
    }

    @Test
    void updateMenuExecutionRequiresFieldLevelValidation() throws Exception {
        mockMvc.perform(put("/api/admin/menus/132/execution")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").exists());
    }

    @Test
    void updateMenuExecutionRequiresAuthBeforeTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/menus/132/execution")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"menuName":"메뉴 정보 관리","screenId":"SCR-MENU-INFO-MGMT","url":"/admin/menu-info","icon":"file-cog","businessCategory":"SYSTEM","description":"설명","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).updateMenuExecution(any(), any(), any());
    }

    @Test
    void updateMenuExecutionRejectsUnknownMenuWithoutPersistenceSideEffects() {
        MenuExecutionMapper mapper = mock(MenuExecutionMapper.class);
        MenuExecutionService menuExecutionService = new MenuExecutionService(mapper);
        when(mapper.existsActiveMenu(9999L)).thenReturn(0);

        assertThatThrownBy(() -> menuExecutionService.updateMenuExecution(9999L,
                new MenuExecutionRequest("없는 메뉴", "SCR-NOT-FOUND", "/admin/missing", "x", "SYSTEM", "설명", "검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("메뉴 실행정보 저장");
        verify(mapper, never()).updateMenuExecutionFields(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).upsertMenuExecutionInfo(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateMenuExecutionSurfacesBusinessRuleFieldError() throws Exception {
        when(service.updateMenuExecution(eq(132L), any(MenuExecutionRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("메뉴 실행정보 저장 요청이 올바르지 않습니다.",
                        List.of(new ValidationError("url", "실행 URL은 / 로 시작해야 합니다."))));

        mockMvc.perform(put("/api/admin/menus/132/execution")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"menuName":"메뉴 정보 관리","screenId":"SCR-MENU-INFO-MGMT","url":"http://localhost:3000/admin/menu-info","icon":"file-cog","businessCategory":"SYSTEM","description":"설명","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("url"));
    }

    @Test
    void forbiddenMenuExecutionRequestReturns403ApiError() throws Exception {
        when(service.getMenuExecution(132L)).thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/admin/menus/132/execution"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private MenuExecutionRow row(String screenId, String url, String icon) {
        return new MenuExecutionRow(132L, 130L, "SCREEN", "메뉴 정보 관리", screenId, url, icon,
                "SYSTEM", "메뉴 실행 정보 관리", "ACTIVE", "화면 연결 갱신", 1L,
                LocalDateTime.parse("2026-08-18T09:00:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
