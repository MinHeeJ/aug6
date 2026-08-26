package kr.ac.knue.commonfoundation.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExcelOperationsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic26CrossCuttingVerificationTest {
    @Autowired MockMvc mockMvc;
    @MockBean ExcelOperationsService service;

    @Test
    void downloadAndFileProducingOperationsRevalidateSessionAndRoleBeforeReturningFileBodies() throws Exception {
        when(service.downloadUploadTemplate("TPL-2026", 1L))
                .thenReturn(new ExcelDownloadFile("양식.csv", "text/csv", "교번\n".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/admin/excel-upload-templates/TPL-2026/file").cookie(adminCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mockMvc.perform(get("/api/admin/excel-upload-templates/TPL-2026/file")
                        .requestAttr("currentUser", nonAdminUser()).cookie(adminCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).downloadUploadTemplate(eq("TPL-2026"), eq(2L));

        mockMvc.perform(get("/api/admin/excel-upload-templates/TPL-2026/file")
                        .requestAttr("currentUser", adminUser()).cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Stored-File-Name"))
                .andExpect(header().doesNotExist("X-Storage-Path"));
    }

    @Test
    void validationFailuresUseApiErrorFieldsWithoutLeakingSensitiveSystemDetails() throws Exception {
        when(service.createExcelDownload(any(ExcelDownloadRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("엑셀 다운로드 요청이 올바르지 않습니다.",
                        List.of(new ValidationError("outputType", "출력유형을 선택하세요."))));

        mockMvc.perform(post("/api/admin/excel-downloads")
                        .requestAttr("currentUser", adminUser()).cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("outputType"))
                .andExpect(jsonPath("$.error.message").value("엑셀 다운로드 요청이 올바르지 않습니다."))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception"))))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))));
    }

    @Test
    void openApiUncoveredRequirementLedgerKeepsCrossCuttingExcelSecurityAndCleanupAssertions() throws Exception {
        String openApi = new ClassPathResource("contracts/openapi.yaml").getContentAsString(StandardCharsets.UTF_8);
        String ledger = openApi.substring(openApi.indexOf("x-uncovered-requirements:"));

        assertThat(ledger).contains(
                "canonical_id: REQ-565", "status: covered", "contracts/openapi.yaml x-uncovered-requirements",
                "canonical_id: REQ-586", "status: covered", "downloadUploadTemplate",
                "canonical_id: REQ-588", "status: data-constraint", "data-model.md 공통 제약");
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }

    private CurrentUser nonAdminUser() {
        return new CurrentUser(2L, "operator", "E0002", "업무 담당자", List.of("R05"), List.of());
    }
}
