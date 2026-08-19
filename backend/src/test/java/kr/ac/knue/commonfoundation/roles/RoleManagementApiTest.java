package kr.ac.knue.commonfoundation.roles;

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

@WebMvcTest(RoleManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RoleManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean RoleManagementService roleManagementService;

    @Test
    void listRolesReturnsSeedRoleMetadataForR01ToR09() throws Exception {
        when(roleManagementService.listRoles(0, 20, "R09")).thenReturn(List.of(
                new RoleRow("R09", "시스템관리자", "사용자·조직·메뉴·권한·코드 관리", "시스템 관리자", "전체", "Y", "ACTIVE", LocalDateTime.parse("2026-01-02T03:04:05"))));

        mockMvc.perform(get("/api/admin/roles").param("filter", "R09").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].roleCode").value("R09"))
                .andExpect(jsonPath("$.data[0].roleName").value("시스템관리자"))
                .andExpect(jsonPath("$.data[0].purpose").value("사용자·조직·메뉴·권한·코드 관리"));
    }

    @Test
    void updateRolePersistsEditableMetadataAndReturnsUpdatedRole() throws Exception {
        when(roleManagementService.updateRole(eq("R09"), any(RoleUpdateRequest.class), eq(1L)))
                .thenReturn(new RoleRow("R09", "시스템 관리자", "공통기능 관리", "R09 관리자", "전체 데이터", "Y", "ACTIVE", LocalDateTime.parse("2026-03-01T09:00:00")));

        mockMvc.perform(put("/api/admin/roles/R09")
                        .requestAttr("currentUser", new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of()))
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"시스템 관리자\",\"purpose\":\"공통기능 관리\",\"assignmentCriteria\":\"R09 관리자\",\"defaultDataScope\":\"전체 데이터\",\"changeReason\":\"역할 목적 정비\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("R09"))
                .andExpect(jsonPath("$.data.roleName").value("시스템 관리자"))
                .andExpect(jsonPath("$.data.defaultDataScope").value("전체 데이터"));
    }

    @Test
    void updateRoleRequiresAuthenticatedAdminSession() throws Exception {
        mockMvc.perform(put("/api/admin/roles/R09")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"시스템 관리자\",\"purpose\":\"공통기능 관리\",\"assignmentCriteria\":\"R09 관리자\",\"defaultDataScope\":\"전체 데이터\",\"changeReason\":\"역할 목적 정비\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void updateRoleReturnsFieldErrorWhenMandatoryMetadataIsMissing() throws Exception {
        mockMvc.perform(put("/api/admin/roles/R09")
                        .requestAttr("currentUser", new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of()))
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateRoleBusinessRuleReturnsFieldErrorWithoutTableSideEffect() throws Exception {
        when(roleManagementService.updateRole(eq("R10"), any(RoleUpdateRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("역할 업무 규칙 위반",
                        List.of(new ValidationError("roleCode", "R01~R09 범위만 관리할 수 있습니다."))));

        mockMvc.perform(put("/api/admin/roles/R10")
                        .requestAttr("currentUser", new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of()))
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"신규 역할\",\"purpose\":\"범위 검증\",\"assignmentCriteria\":\"관리자\",\"defaultDataScope\":\"전체 데이터\",\"changeReason\":\"검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("roleCode"));
        verify(roleManagementService).updateRole(eq("R10"), any(RoleUpdateRequest.class), eq(1L));
    }

    @Test
    void serviceRejectsRoleCodeMutationAndDoesNotPersistSideEffects() {
        RoleManagementMapper mapper = mock(RoleManagementMapper.class);
        RoleManagementService service = new RoleManagementService(mapper);
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoleName("교원 변경");
        request.setPurpose("본인 업무");
        request.setAssignmentCriteria("재직 교원");
        request.setDefaultDataScope("본인");
        request.setChangeReason("역할명 정비");
        request.captureUnexpectedField("roleCode", "R10");

        assertThatThrownBy(() -> service.updateRole("R01", request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("역할코드");
        verify(mapper, never()).updateRole(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void serviceRejectsNonSeedRoleCodeCreationScopeWithoutChangingRows() {
        RoleManagementMapper mapper = mock(RoleManagementMapper.class);
        RoleManagementService service = new RoleManagementService(mapper);
        when(mapper.findRoleByCode("R10")).thenReturn(null);
        RoleUpdateRequest request = validRequest();

        assertThatThrownBy(() -> service.updateRole("R10", request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("R01~R09");
        verify(mapper, never()).updateRole(any(), any(), any(), any(), any(), any(), any());
    }

    private RoleUpdateRequest validRequest() {
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoleName("시스템 관리자");
        request.setPurpose("공통기능 관리");
        request.setAssignmentCriteria("R09 관리자");
        request.setDefaultDataScope("전체 데이터");
        request.setChangeReason("역할 목적 정비");
        return request;
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
