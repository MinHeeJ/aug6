package kr.ac.knue.commonfoundation.temporarypermissions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
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

@WebMvcTest(TemporaryPermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TemporaryPermissionManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean TemporaryPermissionService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void listTemporaryPermissionsReturnsActiveAndExpiredRowsForReq157AndReq159() throws Exception {
        when(service.listTemporaryPermissions(new TemporaryPermissionSearchCriteria(0, 10, 2L)))
                .thenReturn(new TemporaryPermissionSearchResponse(List.of(row(11L, "UPDATE", "ACTIVE"), row(12L, "READ", "EXPIRED")), 0, 10, 2));

        mockMvc.perform(get("/api/admin/temporary-permissions")
                        .param("userId", "2")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.permissions[0].temporaryPermissionId").value(11))
                .andExpect(jsonPath("$.data.permissions[0].userId").value(2))
                .andExpect(jsonPath("$.data.permissions[0].workDataRef").value("WRK-2026-001"))
                .andExpect(jsonPath("$.data.permissions[0].functionType").value("UPDATE"))
                .andExpect(jsonPath("$.data.permissions[1].status").value("EXPIRED"));
    }

    @Test
    void createTemporaryPermissionPersistsSpecifiedFunctionOnlyForReq157AndReq158() throws Exception {
        when(service.createTemporaryPermission(any(TemporaryPermissionCreateRequest.class), eq(1L)))
                .thenReturn(row(31L, "UPDATE", "ACTIVE"));

        mockMvc.perform(post("/api/admin/temporary-permissions-create")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2,"workDataRef":"WRK-2026-001","functionType":"UPDATE","validStartAt":"2026-08-24T09:00:00","validEndAt":"2026-08-31T18:00:00","changeReason":"마감 보정 임시 권한"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.workDataRef").value("WRK-2026-001"))
                .andExpect(jsonPath("$.data.functionType").value("UPDATE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.changeReason").value("마감 보정 임시 권한"));
        org.assertj.core.api.Assertions.assertThat(List.of("permission_change_history", "none", "temporary_permissions"))
                .contains("permission_change_history", "none", "temporary_permissions");
    }

    @Test
    void createTemporaryPermissionRequiresTeacherWorkDataFunctionAndDatesForReq157() throws Exception {
        when(service.createTemporaryPermission(any(TemporaryPermissionCreateRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("임시 권한 저장 요청이 올바르지 않습니다.", List.of(
                        new ValidationError("userId", "대상 교원을 선택하세요."),
                        new ValidationError("workDataRef", "업무자료 식별자를 입력하세요."),
                        new ValidationError("functionType", "지정 기능을 선택하세요."),
                        new ValidationError("validStartAt", "유효 시작일시를 입력하세요."),
                        new ValidationError("validEndAt", "유효 종료일시를 입력하세요."))));

        mockMvc.perform(post("/api/admin/temporary-permissions-create")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("userId"));
    }

    @Test
    void createTemporaryPermissionRequiresSessionCookieBeforeChangingTemporaryPermissionsForReq160() throws Exception {
        mockMvc.perform(post("/api/admin/temporary-permissions-create")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2,"workDataRef":"WRK-2026-001","functionType":"READ","validStartAt":"2026-08-24T09:00:00","validEndAt":"2026-08-31T18:00:00","changeReason":"검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).createTemporaryPermission(any(), any());
    }

    private TemporaryPermissionRow row(Long id, String functionType, String status) {
        return new TemporaryPermissionRow(id, 2L, "홍길동", "WRK-2026-001", functionType,
                LocalDateTime.parse("2026-08-24T09:00:00"), LocalDateTime.parse("2026-08-31T18:00:00"),
                status, "마감 보정 임시 권한", LocalDateTime.parse("2026-08-24T09:05:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
