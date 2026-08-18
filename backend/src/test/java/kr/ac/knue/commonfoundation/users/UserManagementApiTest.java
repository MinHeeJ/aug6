package kr.ac.knue.commonfoundation.users;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

@WebMvcTest(UserManagementController.class)
@Import(GlobalExceptionHandler.class)
class UserManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean UserManagementService userManagementService;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void searchUsersReturnsKorusReadOnlyFieldsLocalUseFlagRolesAndSyncEvidence() throws Exception {
        UserSummary user = new UserSummary(2L, "professor1", "E1001", "홍길동", "KNUE-DEPT-COMP", "컴퓨터교육과",
                "교수", "ACTIVE", "학과장", null, LocalDateTime.of(2026, 8, 18, 9, 0), "Y", "ACTIVE", List.of("R01"));
        when(userManagementService.search(any(UserSearchCriteria.class)))
                .thenReturn(new UserSearchResponse(List.of(user), List.of(new AvailableRole("R01", "교원")), 0, 20, 1));

        mockMvc.perform(get("/api/admin/users")
                        .param("employeeNo", "E1001")
                        .param("name", "홍길동")
                        .param("organizationCodeFilter", "KNUE-DEPT-COMP")
                        .param("rankName", "교수")
                        .param("employmentStatus", "ACTIVE")
                        .param("roleCodeFilter", "R01")
                        .param("systemUseYn", "Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.users[0].positionName").value("학과장"))
                .andExpect(jsonPath("$.data.users[0].lastSyncedAt").exists())
                .andExpect(jsonPath("$.data.users[0].roleCodes[0]").value("R01"));
    }

    @Test
    void updateUserAccountPersistsOnlySystemUseYnAndRejectsKorusSourceMutation() throws Exception {
        UserSummary updated = new UserSummary(2L, "professor1", "E1001", "홍길동", "KNUE-DEPT-COMP", "컴퓨터교육과",
                "교수", "ACTIVE", "학과장", null, LocalDateTime.now(), "N", "INACTIVE", List.of("R01"));
        when(userManagementService.updateAccount(eq(2L), any(UpdateUserAccountRequest.class), eq(1L))).thenReturn(updated);

        mockMvc.perform(patch("/api/admin/users/2/account")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemUseYn":"N","changeReason":"휴직 계정 정리"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemUseYn").value("N"));

        when(userManagementService.updateAccount(eq(2L), any(UpdateUserAccountRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("KORUS 원천 인사정보는 사용자 관리에서 직접 수정할 수 없습니다.",
                        List.of(new ValidationError("name", "KORUS 원천정보는 읽기 전용입니다."))));
        mockMvc.perform(patch("/api/admin/users/2/account")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemUseYn":"Y","changeReason":"검증","name":"변경금지"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("name"));
    }

    @Test
    void updateUserAccountStoresAccountTableSideEffectThroughServiceBoundary() throws Exception {
        UserSummary updated = new UserSummary(2L, "professor1", "E1001", "홍길동", "KNUE-DEPT-COMP", "컴퓨터교육과",
                "교수", "ACTIVE", "학과장", null, LocalDateTime.now(), "N", "INACTIVE", List.of("R01"));
        when(userManagementService.updateAccount(eq(2L), any(UpdateUserAccountRequest.class), eq(1L))).thenReturn(updated);

        mockMvc.perform(patch("/api/admin/users/2/account")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemUseYn":"N","changeReason":"계정 상태 변경"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.systemUseYn").value("N"));
        verify(userManagementService).updateAccount(eq(2L), any(UpdateUserAccountRequest.class), eq(1L));
    }

    @Test
    void updateUserRolesPersistsManualRolesAndReturnsValidationErrors() throws Exception {
        when(userManagementService.updateRoles(eq(2L), any(UpdateUserRolesRequest.class), eq(1L)))
                .thenReturn(List.of(new UserRoleSummary(10L, 2L, "R01", "교원", "MANUAL", LocalDate.now(), null, 1L, "ACTIVE", LocalDateTime.now(), "역할 변경")));

        mockMvc.perform(patch("/api/admin/users/2/roles")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleCodes":["R01"],"validStartDate":"2026-08-18","changeReason":"역할 변경"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roleCode").value("R01"))
                .andExpect(jsonPath("$.data[0].approverUserId").value(1));

        mockMvc.perform(patch("/api/admin/users/2/roles")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleCodes":[],"changeReason":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void protectedUserManagementMutationRequiresAuthenticatedCurrentUserAttribute() throws Exception {
        mockMvc.perform(patch("/api/admin/users/2/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemUseYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void updateUserRolesRequiresAuthBeforeRoleTableSideEffect() throws Exception {
        mockMvc.perform(patch("/api/admin/users/2/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleCodes":["R01"],"validStartDate":"2026-08-18","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(userManagementService, never()).updateRoles(any(), any(), any());
    }

    @Test
    void updateUserRolesBusinessRulePreventsRoleTableSideEffect() throws Exception {
        when(userManagementService.updateRoles(eq(2L), any(UpdateUserRolesRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("사용자 업무 역할 변경 업무 규칙 위반",
                        List.of(new ValidationError("roleCodes", "존재하지 않는 역할입니다."))));

        mockMvc.perform(patch("/api/admin/users/2/roles")
                        .requestAttr("currentUser", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleCodes":["R77"],"validStartDate":"2026-08-18","changeReason":"업무 규칙 검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields[0].field").value("roleCodes"));
        verify(userManagementService).updateRoles(eq(2L), any(UpdateUserRolesRequest.class), eq(1L));
    }

    @Test
    void forbiddenUserManagementRequestReturns403ApiError() throws Exception {
        when(userManagementService.search(any(UserSearchCriteria.class))).thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
}
