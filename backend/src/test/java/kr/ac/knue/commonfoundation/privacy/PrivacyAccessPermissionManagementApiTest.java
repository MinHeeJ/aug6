package kr.ac.knue.commonfoundation.privacy;

import static org.assertj.core.api.Assertions.assertThat;
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
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PrivacyAccessPermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PrivacyAccessPermissionManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean PrivacyAccessPermissionService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    private final CurrentUser reader = new CurrentUser(2L, "reader", "E0002", "조회 사용자", List.of("R01"), List.of());

    @Test
    void listPrivacyAccessPermissionsFiltersByRoleAndFieldWithTwentyDefaultForReq201() throws Exception {
        when(service.listPrivacyAccessPermissions(new PrivacyAccessPermissionSearchCriteria(0, 20, "R09", "researcher")))
                .thenReturn(new PrivacyAccessPermissionSearchResponse(List.of(permissionRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/privacy/permissions")
                        .param("roleCode", "R09")
                        .param("fieldKey", "researcher")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.permissions[0].roleCode").value("R09"))
                .andExpect(jsonPath("$.data.permissions[0].fieldKey").value("researcher_registration_no"))
                .andExpect(jsonPath("$.data.permissions[0].rawViewAllowedYn").value("Y"));
    }

    @Test
    void savePrivacyAccessPermissionsPersistsIndependentFlagsForReq202ToReq205() throws Exception {
        when(service.savePrivacyAccessPermissions(any(), eq(1L))).thenReturn(List.of(permissionRow()));

        mockMvc.perform(put("/api/admin/privacy/permissions-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"roleCode":"R09","fieldKey":"researcher_registration_no","rawViewAllowedYn":"Y","maskedViewAllowedYn":"N","exportAllowedYn":"Y","accountViewAllowedYn":"N","changeReason":"원문과 출력 권한만 허용"}]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].rawViewAllowedYn").value("Y"))
                .andExpect(jsonPath("$.data[0].maskedViewAllowedYn").value("Y"))
                .andExpect(jsonPath("$.data[0].exportAllowedYn").value("Y"))
                .andExpect(jsonPath("$.data[0].accountViewAllowedYn").value("Y"));
    }

    @Test
    void savePrivacyAccessPermissionsRejectsNonSeedRoleCodeForReq201() throws Exception {
        when(service.savePrivacyAccessPermissions(any(), eq(1L))).thenThrow(new BusinessValidationException(
                "개인정보 조회권한 저장 요청이 올바르지 않습니다.", List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("roleCode", "R01~R09 기존 역할코드만 사용할 수 있습니다."))));

        mockMvc.perform(put("/api/admin/privacy/permissions-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"roleCode":"R10","fieldKey":"researcher_registration_no","rawViewAllowedYn":"Y","maskedViewAllowedYn":"Y","exportAllowedYn":"Y","accountViewAllowedYn":"Y","changeReason":"검증"}]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("roleCode"));
    }

    @Test
    void savePrivacyAccessPermissionsRejectsUserRoleAssignmentPayloadAndDoesNotPersistForReq205() {
        PrivacyAccessPermissionMapper mapper = mock(PrivacyAccessPermissionMapper.class);
        PrivacyAccessPermissionService privacyAccessPermissionService = new PrivacyAccessPermissionService(mapper);

        assertThatThrownBy(() -> privacyAccessPermissionService.savePrivacyAccessPermissions(List.of(
                new PrivacyAccessPermissionSaveRequest("R09", "researcher_registration_no", "Y", "Y", "Y", "Y", "검증", 2L)), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("사용자 역할 부여");
        verify(mapper, never()).upsertPrivacyAccessPermission(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void savePrivacyAccessPermissionsRejectsNonR09SessionBeforePersistence() throws Exception {
        mockMvc.perform(put("/api/admin/privacy/permissions-save")
                        .requestAttr("currentUser", reader)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"roleCode":"R09","fieldKey":"researcher_registration_no","rawViewAllowedYn":"Y","maskedViewAllowedYn":"Y","exportAllowedYn":"Y","accountViewAllowedYn":"Y","changeReason":"권한 검증"}]
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).savePrivacyAccessPermissions(any(), any());
    }

    @Test
    void evaluatePrivacyAccessPermissionAllowsExplicitPermissionForReq206() throws Exception {
        when(service.evaluatePrivacyAccessPermission(new PrivacyAccessEvaluateRequest("R09", "researcher_registration_no", "RAW_VIEW", "업무 확인")))
                .thenReturn(new PrivacyAccessEvaluateResponse("R09", "researcher_registration_no", "RAW_VIEW", true, "권한 설정으로 허용", false));

        mockMvc.perform(post("/api/admin/privacy/permissions/evaluate")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleCode":"R09","fieldKey":"researcher_registration_no","accessType":"RAW_VIEW","processPurpose":"업무 확인"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.rawValue").doesNotExist())
                .andExpect(jsonPath("$.data.plainValue").doesNotExist());
    }

    @Test
    void evaluatePrivacyAccessPermissionDeniesUnsetCombinationForReq206() {
        PrivacyAccessPermissionMapper mapper = mock(PrivacyAccessPermissionMapper.class);
        PrivacyAccessPermissionService privacyAccessPermissionService = new PrivacyAccessPermissionService(mapper);
        when(mapper.findByRoleCodeAndFieldKey("R01", "researcher_registration_no")).thenReturn(null);

        PrivacyAccessEvaluateResponse response = privacyAccessPermissionService.evaluatePrivacyAccessPermission(
                new PrivacyAccessEvaluateRequest("R01", "researcher_registration_no", "RAW_VIEW", "업무 확인"));

        assertThat(response.allowed()).isFalse();
        assertThat(response.reason()).contains("미설정");
        assertThat(response.rawValueExposed()).isFalse();
    }

    @Test
    void evaluatePrivacyAccessPermissionRejectsMissingAccessTypeForReq206() throws Exception {
        when(service.evaluatePrivacyAccessPermission(any())).thenThrow(new BusinessValidationException(
                "개인정보 권한 판정 요청이 올바르지 않습니다.", List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("accessType", "접근 유형을 선택하세요."))));

        mockMvc.perform(post("/api/admin/privacy/permissions/evaluate")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleCode":"R09","fieldKey":"researcher_registration_no","processPurpose":"업무 확인"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("accessType"));
    }

    private PrivacyAccessPermissionRow permissionRow() {
        return new PrivacyAccessPermissionRow(1L, "R09", "시스템관리자", "researcher_registration_no", "Y", "Y", "Y", "Y",
                "관리자 허용", LocalDateTime.parse("2026-08-25T09:00:00"), 1L);
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
