package kr.ac.knue.commonfoundation.functionpermissions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import kr.ac.knue.commonfoundation.permissionops.PermissionChangeHistoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FunctionPermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FunctionPermissionManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean FunctionPermissionService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void listFunctionPermissionsReturnsScreenRoleFunctionTypeRowsForReq148AndReq152() throws Exception {
        when(service.listFunctionPermissions(new FunctionPermissionSearchCriteria(0, 10, "SCR-FUNCTION-PERMISSION-MGMT", "R09")))
                .thenReturn(new FunctionPermissionSearchResponse(List.of(row("READ", "ALLOW"), row("UPDATE", "DENY")), 0, 10, 2));

        mockMvc.perform(get("/api/admin/function-permissions")
                        .param("screenId", "SCR-FUNCTION-PERMISSION-MGMT")
                        .param("roleCode", "R09")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.permissions[0].screenId").value("SCR-FUNCTION-PERMISSION-MGMT"))
                .andExpect(jsonPath("$.data.permissions[0].roleCode").value("R09"))
                .andExpect(jsonPath("$.data.permissions[0].functionType").value("READ"))
                .andExpect(jsonPath("$.data.permissions[1].functionType").value("UPDATE"));
    }

    @Test
    void saveFunctionPermissionsPersistsIndependentFunctionTypeAndReturnsUpdatedRowForReq149() throws Exception {
        when(service.saveFunctionPermission(any(FunctionPermissionSaveRequest.class), eq(1L)))
                .thenReturn(row("UPDATE", "DENY"));

        mockMvc.perform(put("/api/admin/function-permissions-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"screenId":"SCR-FUNCTION-PERMISSION-MGMT","roleCode":"R09","functionType":"UPDATE","permissionAllowed":"DENY","changeReason":"수정 기능 차단"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.screenId").value("SCR-FUNCTION-PERMISSION-MGMT"))
                .andExpect(jsonPath("$.data.roleCode").value("R09"))
                .andExpect(jsonPath("$.data.functionType").value("UPDATE"))
                .andExpect(jsonPath("$.data.permissionAllowed").value("DENY"))
                .andExpect(jsonPath("$.data.changeReason").value("수정 기능 차단"));
    }

    @Test
    void saveFunctionPermissionsRequiresFunctionTypeFieldValidationForReq149() throws Exception {
        mockMvc.perform(put("/api/admin/function-permissions-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"screenId":"SCR-FUNCTION-PERMISSION-MGMT","roleCode":"R09","permissionAllowed":"ALLOW","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("functionType"));
    }

    @Test
    void saveFunctionPermissionsRequiresSessionCookieBeforeHistorySideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/function-permissions-save")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"screenId":"SCR-FUNCTION-PERMISSION-MGMT","roleCode":"R09","functionType":"READ","permissionAllowed":"ALLOW","changeReason":"검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveFunctionPermission(any(), any());
    }

    @Test
    void saveFunctionPermissionsRejectsUnknownScreenWithoutPersistenceSideEffectsForReq150() {
        FunctionPermissionMapper mapper = mock(FunctionPermissionMapper.class);
        PermissionChangeHistoryMapper historyMapper = mock(PermissionChangeHistoryMapper.class);
        FunctionPermissionService functionPermissionService = new FunctionPermissionService(mapper, historyMapper);
        when(mapper.existsScreen("SCR-UNKNOWN")).thenReturn(0);
        when(mapper.existsRole("R09")).thenReturn(1);

        assertThatThrownBy(() -> functionPermissionService.saveFunctionPermission(new FunctionPermissionSaveRequest(
                "SCR-UNKNOWN", "R09", "READ", "ALLOW", "검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("기능 권한 저장");
        verify(mapper, never()).upsertFunctionPermission(any(), any(), any(), any(), any(), any());
        verify(historyMapper, never()).insertPermissionChangeHistory(any(), any(), any(), any(), any(), any());
    }

    @Test
    void evaluateFunctionPermissionAllowsAllowedFunctionForReq151() throws Exception {
        when(service.evaluate(new FunctionPermissionEvaluateRequest("SCR-FUNCTION-PERMISSION-MGMT", "R09", "READ", "IN_PROGRESS", null)))
                .thenReturn(new FunctionPermissionEvaluateResponse(true, "SCR-FUNCTION-PERMISSION-MGMT", "R09", "READ", "ALLOW"));

        mockMvc.perform(post("/api/admin/function-permissions/evaluate")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"screenId":"SCR-FUNCTION-PERMISSION-MGMT","roleCode":"R09","functionType":"READ","targetDataStatus":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.functionType").value("READ"));
    }

    @Test
    void evaluateFunctionPermissionBlocksDeniedApiRequestForReq151() throws Exception {
        when(service.evaluate(any(FunctionPermissionEvaluateRequest.class)))
                .thenThrow(new ForbiddenException());

        mockMvc.perform(post("/api/admin/function-permissions/evaluate")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"screenId":"SCR-FUNCTION-PERMISSION-MGMT","roleCode":"R09","functionType":"DELETE","targetDataStatus":"IN_PROGRESS"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void evaluateFunctionPermissionRequiresFunctionTypeValidation() throws Exception {
        mockMvc.perform(post("/api/admin/function-permissions/evaluate")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"screenId":"SCR-FUNCTION-PERMISSION-MGMT","roleCode":"R09"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("functionType"));
    }

    private FunctionPermissionRow row(String functionType, String permissionAllowed) {
        return new FunctionPermissionRow(77L, "SCR-FUNCTION-PERMISSION-MGMT", "기능 권한 관리", "R09", "시스템관리자",
                functionType, permissionAllowed, "수정 기능 차단", LocalDateTime.parse("2026-08-24T09:00:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
