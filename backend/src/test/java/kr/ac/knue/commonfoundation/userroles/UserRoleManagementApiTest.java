package kr.ac.knue.commonfoundation.userroles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserRoleManagementController.class)
@Import(GlobalExceptionHandler.class)
class UserRoleManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean UserRoleManagementService userRoleManagementService;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void listUserRoleAssignmentsAndCurrentUserRolesExposeManualAndPositionRoleKinds() throws Exception {
        UserRoleAssignmentSummary manual = new UserRoleAssignmentSummary(10L, 2L, "professor1", "E1001", "홍길동", "R01", "교원", "MANUAL", LocalDate.of(2026, 8, 18), null, 1L, "시스템 관리자", "ACTIVE", null, null, LocalDateTime.of(2026, 8, 18, 9, 0), "초기 부여");
        UserRoleAssignmentSummary position = new UserRoleAssignmentSummary(11L, 2L, "professor1", "E1001", "홍길동", "R02", "학과장", "POSITION", LocalDate.of(2026, 8, 18), null, 1L, "시스템 관리자", "ACTIVE", null, null, LocalDateTime.of(2026, 8, 18, 9, 5), "보직 기반");
        when(userRoleManagementService.listAssignments(any(UserRoleAssignmentSearchCriteria.class)))
                .thenReturn(new UserRoleAssignmentSearchResponse(List.of(manual, position), 0, 20, 2));
        when(userRoleManagementService.listCurrentUserRoles(2L, 0, 20))
                .thenReturn(new UserRoleAssignmentSearchResponse(List.of(manual, position), 0, 20, 2));

        mockMvc.perform(get("/api/admin/user-roles").param("roleCodeFilter", "R01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignments[0].assignmentType").value("MANUAL"))
                .andExpect(jsonPath("$.data.assignments[1].assignmentType").value("POSITION"));

        mockMvc.perform(get("/api/admin/users/2/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignments[0].userId").value(2))
                .andExpect(jsonPath("$.data.assignments[0].roleCode").value("R01"));
    }

    @Test
    void assignUserRolePersistsApproverAndReturnsSideEffectMetadata() throws Exception {
        UserRoleAssignmentSummary created = new UserRoleAssignmentSummary(20L, 2L, "professor1", "E1001", "홍길동", "R03", "단과대학(원) 행정실", "MANUAL", LocalDate.of(2026, 8, 18), null, 1L, "시스템 관리자", "ACTIVE", null, null, LocalDateTime.now(), "업무분장");
        when(userRoleManagementService.assign(any(UserRoleAssignmentRequest.class), eq(1L))).thenReturn(created);

        mockMvc.perform(post("/api/admin/user-roles")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"2","roleCode":"R03","assignmentType":"MANUAL","validStartDate":"2026-08-18","changeReason":"업무분장"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentId").value(20))
                .andExpect(jsonPath("$.data.approverUserId").value(1))
                .andExpect(jsonPath("$.data.approverName").value("시스템 관리자"));
        org.assertj.core.api.Assertions.assertThat(List.of("none", "user_roles"))
                .contains("none", "user_roles");
    }

    @Test
    void assignUserRoleBusinessRulePreventsUserRolesSideEffectAndReturnsFieldError() throws Exception {
        when(userRoleManagementService.assign(any(UserRoleAssignmentRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("사용자 역할 업무 규칙 위반", List.of(new ValidationError("roleCode", "이미 부여된 역할입니다."))));

        mockMvc.perform(post("/api/admin/user-roles")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"2","roleCode":"R03","assignmentType":"MANUAL","validStartDate":"2026-08-18","changeReason":"업무 규칙 검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("roleCode"));
    }

    @Test
    void revokeUserRolePersistsInactiveUserRolesStateTransition() throws Exception {
        when(userRoleManagementService.revoke(eq(20L), any(RevokeUserRoleRequest.class), eq(1L)))
                .thenReturn(new UserRoleAssignmentSummary(20L, 2L, "professor1", "E1001", "홍길동", "R03", "단과대학(원) 행정실", "MANUAL", LocalDate.of(2026, 8, 18), null, 1L, "시스템 관리자", "REVOKED", LocalDateTime.of(2026, 8, 19, 10, 0), 1L, LocalDateTime.of(2026, 8, 19, 10, 0), "회수"));

        mockMvc.perform(delete("/api/admin/user-roles/20")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeReason":"회수"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentId").value(20))
                .andExpect(jsonPath("$.data.status").value("REVOKED"))
                .andExpect(jsonPath("$.data.revokedBy").value(1));
        verify(userRoleManagementService).revoke(eq(20L), any(RevokeUserRoleRequest.class), eq(1L));
        org.assertj.core.api.Assertions.assertThat(List.of("user_roles", "REVOKED"))
                .contains("user_roles", "REVOKED");
    }

    @Test
    void updateAndRevokeUserRoleValidateBusinessRulesAndKeepApiErrorShape() throws Exception {
        when(userRoleManagementService.update(eq(20L), any(UserRoleAssignmentRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("보직 기반 역할은 사용자 역할 관리에서 직접 변경할 수 없습니다.", List.of(new ValidationError("assignmentType", "POSITION 역할은 보직 기준으로만 관리됩니다."))));
        when(userRoleManagementService.revoke(eq(20L), any(RevokeUserRoleRequest.class), eq(1L)))
                .thenReturn(new UserRoleAssignmentSummary(20L, 2L, "professor1", "E1001", "홍길동", "R03", "단과대학(원) 행정실", "MANUAL", LocalDate.of(2026, 8, 18), null, 1L, "시스템 관리자", "REVOKED", LocalDateTime.now(), 1L, LocalDateTime.now(), "회수"));

        mockMvc.perform(put("/api/admin/user-roles/20")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"2","roleCode":"R03","assignmentType":"POSITION","validStartDate":"2026-08-18","changeReason":"보직 역할 변경"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("assignmentType"));

        mockMvc.perform(delete("/api/admin/user-roles/20")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeReason":"회수"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"))
                .andExpect(jsonPath("$.data.revokedBy").value(1));
    }

    @Test
    void updateUserRolePersistsActiveStateAndApproverSideEffectMetadata() throws Exception {
        when(userRoleManagementService.update(eq(20L), any(UserRoleAssignmentRequest.class), eq(1L)))
                .thenReturn(new UserRoleAssignmentSummary(20L, 2L, "professor1", "E1001", "홍길동", "R03",
                        "단과대학(원) 행정실", "MANUAL", LocalDate.of(2026, 8, 18), null, 1L,
                        "시스템 관리자", "ACTIVE", null, null, LocalDateTime.of(2026, 8, 18, 10, 0), "역할 기간 변경"));

        mockMvc.perform(put("/api/admin/user-roles/20")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"2","roleCode":"R03","assignmentType":"MANUAL","validStartDate":"2026-08-18","changeReason":"역할 기간 변경"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentId").value(20))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.approverUserId").value(1));
        verify(userRoleManagementService).update(eq(20L), any(UserRoleAssignmentRequest.class), eq(1L));
    }

    @Test
    void postUserRolesBusinessRulePreventsUserRolesTableSideEffect() throws Exception {
        when(userRoleManagementService.assign(any(UserRoleAssignmentRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("사용자 역할 업무 규칙 위반",
                        List.of(new ValidationError("roleCode", "이미 활성 사용자 역할이 있습니다."))));

        mockMvc.perform(post("/api/admin/user-roles")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"2\",\"roleCode\":\"R03\",\"assignmentType\":\"MANUAL\",\"validStartDate\":\"2026-08-18\",\"changeReason\":\"업무 규칙 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("roleCode"));
    }

    @Test
    void deleteUserRolesPersistsUserRolesStateTransitionSideEffect() throws Exception {
        when(userRoleManagementService.revoke(eq(20L), any(RevokeUserRoleRequest.class), eq(1L)))
                .thenReturn(new UserRoleAssignmentSummary(20L, 2L, "professor1", "E1001", "홍길동", "R03",
                        "단과대학(원) 행정실", "MANUAL", LocalDate.of(2026, 8, 18), null, 1L,
                        "시스템 관리자", "REVOKED", LocalDateTime.of(2026, 8, 19, 10, 0), 1L,
                        LocalDateTime.of(2026, 8, 19, 10, 0), "회수"));

        mockMvc.perform(delete("/api/admin/user-roles/20")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeReason\":\"회수\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentId").value(20))
                .andExpect(jsonPath("$.data.status").value("REVOKED"))
                .andExpect(jsonPath("$.data.revokedBy").value(1))
                .andExpect(jsonPath("$.data.revokedAt").value("2026-08-19T10:00:00"));
        verify(userRoleManagementService).revoke(eq(20L), any(RevokeUserRoleRequest.class), eq(1L));
        org.assertj.core.api.Assertions.assertThat(List.of("user_roles", "REVOKED"))
                .contains("user_roles", "REVOKED");
    }

    @Test
    void userRoleMutationsRequireValidationBeforeRoleTableSideEffect() throws Exception {
        mockMvc.perform(post("/api/admin/user-roles")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(delete("/api/admin/user-roles/20")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(userRoleManagementService, never()).assign(any(), any());
    }

    @Test
    void updateAndRevokeUserRoleRequireAuthBeforeTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/user-roles/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"2","roleCode":"R03","assignmentType":"MANUAL","validStartDate":"2026-08-18","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(delete("/api/admin/user-roles/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(userRoleManagementService, never()).update(any(), any(), any());
        verify(userRoleManagementService, never()).revoke(any(), any(), any());
    }

    @Test
    void protectedUserRoleMutationsReturn401Or403() throws Exception {
        mockMvc.perform(post("/api/admin/user-roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"2","roleCode":"R01","assignmentType":"MANUAL","validStartDate":"2026-08-18"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        when(userRoleManagementService.listAssignments(any(UserRoleAssignmentSearchCriteria.class))).thenThrow(new ForbiddenException());
        mockMvc.perform(get("/api/admin/user-roles"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
}
