package kr.ac.knue.commonfoundation.permissionops;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PermissionChangeHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PermissionChangeHistoryManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean PermissionChangeHistoryService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void listPermissionChangeHistoryReturnsBeforeAfterActorReasonAndChangedAtForReq161Req162() throws Exception {
        when(service.listPermissionChangeHistory(new PermissionChangeHistorySearchCriteria(0, 10, "FUNCTION", "SCR-FUNCTION-PERMISSION-MGMT:R09:UPDATE")))
                .thenReturn(new PermissionChangeHistorySearchResponse(List.of(row()), 0, 10, 1));

        mockMvc.perform(get("/api/admin/permission-history")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .param("targetType", "FUNCTION")
                        .param("targetId", "SCR-FUNCTION-PERMISSION-MGMT:R09:UPDATE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.history[0].targetType").value("FUNCTION"))
                .andExpect(jsonPath("$.data.history[0].targetId").value("SCR-FUNCTION-PERMISSION-MGMT:R09:UPDATE"))
                .andExpect(jsonPath("$.data.history[0].beforeValue").value("{\"permissionAllowed\":\"ALLOW\"}"))
                .andExpect(jsonPath("$.data.history[0].afterValue").value("{\"permissionAllowed\":\"DENY\"}"))
                .andExpect(jsonPath("$.data.history[0].changedBy").value(1))
                .andExpect(jsonPath("$.data.history[0].reason").value("수정 기능 차단"))
                .andExpect(jsonPath("$.data.history[0].changedAt").value("2026-08-24T09:00:00"));
    }

    @Test
    void listPermissionChangeHistoryRequiresSessionForReq163() throws Exception {
        mockMvc.perform(get("/api/admin/permission-history")
                        .cookie(adminCookie())
                        .param("targetType", "FUNCTION"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).listPermissionChangeHistory(any());
    }

    @Test
    void listPermissionChangeHistoryRejectsUnknownTargetTypeWithoutQueryForReq163() throws Exception {
        when(service.listPermissionChangeHistory(any())).thenCallRealMethod();

        mockMvc.perform(get("/api/admin/permission-history")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .param("targetType", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("targetType"));
    }

    @Test
    void permissionHistoryEndpointIsReadOnlyForReq164Req171() throws Exception {
        mockMvc.perform(delete("/api/admin/permission-history")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/admin/permission-history")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        verify(service, never()).listPermissionChangeHistory(any());
    }

    private PermissionChangeHistoryRow row() {
        return new PermissionChangeHistoryRow(91L, "FUNCTION", "SCR-FUNCTION-PERMISSION-MGMT:R09:UPDATE",
                "{\"permissionAllowed\":\"ALLOW\"}", "{\"permissionAllowed\":\"DENY\"}", 1L,
                "수정 기능 차단", LocalDateTime.parse("2026-08-24T09:00:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
