package kr.ac.knue.commonfoundation.privacy;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PrivacyFieldPolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PrivacyFieldPolicyManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean PrivacyFieldPolicyService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    private final CurrentUser reader = new CurrentUser(2L, "reader", "E0002", "조회 사용자", List.of("R01"), List.of());

    @Test
    void listPrivacyFieldPoliciesDefaultsToTwentyAndReturnsPolicyRowsForReq195() throws Exception {
        when(service.listPrivacyFieldPolicies(new PrivacyFieldPolicySearchCriteria(0, 20, null, null, null)))
                .thenReturn(new PrivacyFieldPolicySearchResponse(List.of(policyRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/privacy/policies")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.policies[0].fieldKey").value("researcher_registration_no"))
                .andExpect(jsonPath("$.data.policies[0].privacyGrade").value("SENSITIVE"))
                .andExpect(jsonPath("$.data.policies[0].encryptionRequiredYn").value("Y"));
    }

    @Test
    void savePrivacyFieldPoliciesPersistsPolicyAndReturnsSavedRowsForReq196ToReq199() throws Exception {
        when(service.savePrivacyFieldPolicies(any(), eq(1L))).thenReturn(List.of(policyRow()));

        mockMvc.perform(put("/api/admin/privacy/policies-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"fieldKey":"researcher_registration_no","privacyGrade":"SENSITIVE","encryptionRequiredYn":"Y","maskingRule":"LAST4","logExclusionYn":"Y","changeReason":"암호화 정책 적용"}]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fieldKey").value("researcher_registration_no"))
                .andExpect(jsonPath("$.data[0].encryptionRequiredYn").value("Y"))
                .andExpect(jsonPath("$.data[0].logExclusionYn").value("Y"));
    }

    @Test
    void savePrivacyFieldPoliciesRejectsMissingFieldKeyWithFieldLevelErrorForReq195() throws Exception {
        when(service.savePrivacyFieldPolicies(any(), eq(1L))).thenThrow(new BusinessValidationException(
                "개인정보 보호정책 저장 요청이 올바르지 않습니다.", List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("fieldKey", "개인정보 필드를 입력하세요."))));

        mockMvc.perform(put("/api/admin/privacy/policies-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"privacyGrade":"SENSITIVE","encryptionRequiredYn":"Y","logExclusionYn":"Y","changeReason":"검증"}]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("fieldKey"));
    }

    @Test
    void savePrivacyFieldPoliciesRejectsActualPersonalValueAndDoesNotPersistForReq200() {
        PrivacyFieldPolicyMapper mapper = mock(PrivacyFieldPolicyMapper.class);
        PrivacyFieldPolicyService privacyFieldPolicyService = new PrivacyFieldPolicyService(mapper);

        assertThatThrownBy(() -> privacyFieldPolicyService.savePrivacyFieldPolicies(List.of(
                new PrivacyFieldPolicySaveRequest("researcher_registration_no", "SENSITIVE", "Y", "LAST4", "Y", "검증", "평문-1234", null)), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("원문값");
        verify(mapper, never()).upsertPrivacyFieldPolicy(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void savePrivacyFieldPoliciesRejectsNonR09SessionBeforePersistence() throws Exception {
        mockMvc.perform(put("/api/admin/privacy/policies-save")
                        .requestAttr("currentUser", reader)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"fieldKey":"researcher_registration_no","privacyGrade":"SENSITIVE","encryptionRequiredYn":"Y","logExclusionYn":"Y","changeReason":"권한 검증"}]
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).savePrivacyFieldPolicies(any(), any());
    }

    @Test
    void policyApiResponseDoesNotExposeActualPersonalValueForReq200() throws Exception {
        when(service.listPrivacyFieldPolicies(new PrivacyFieldPolicySearchCriteria(0, 20, null, null, null)))
                .thenReturn(new PrivacyFieldPolicySearchResponse(List.of(policyRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/privacy/policies")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policies[0].actualValue").doesNotExist())
                .andExpect(jsonPath("$.data.policies[0].originalValue").doesNotExist())
                .andExpect(jsonPath("$.data.policies[0].plainValue").doesNotExist());
    }

    private PrivacyFieldPolicyRow policyRow() {
        return new PrivacyFieldPolicyRow(1L, "researcher_registration_no", "SENSITIVE", "Y", "LAST4", "Y",
                "암호화 정책 적용", LocalDateTime.parse("2026-08-25T09:00:00"), 1L);
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
