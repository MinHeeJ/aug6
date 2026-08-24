package kr.ac.knue.commonfoundation.fileoperations;

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

@WebMvcTest(FilePolicyManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FilePolicyManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean FilePolicyManagementService filePolicyManagementService;

    @Test
    void listFilePoliciesReturnsR09SessionFilteredPageInApiResponse() throws Exception {
        when(filePolicyManagementService.listFilePolicies(0, 20, "FACULTY"))
                .thenReturn(new FilePolicySearchResponse(List.of(policy("FACULTY_EVALUATION", "pdf,docx")), 0, 20, 1));

        mockMvc.perform(get("/api/admin/file-policies")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .param("page", "0")
                        .param("size", "20")
                        .param("filter", "FACULTY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.policies[0].businessType").value("FACULTY_EVALUATION"))
                .andExpect(jsonPath("$.data.policies[0].allowedExtensions").value("pdf,docx"));
    }

    @Test
    void saveFilePolicyPersistsAllowedExtensionsAndCanBeRequeried() throws Exception {
        when(filePolicyManagementService.saveFilePolicy(any(FilePolicySaveRequest.class), eq(1L)))
                .thenReturn(policy("FACULTY_EVALUATION", "pdf,png,zip"));

        mockMvc.perform(put("/api/admin/file-policies-save")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessType\":\"FACULTY_EVALUATION\",\"allowedExtensions\":\"pdf,png,zip\",\"maxFileSizeMb\":20,\"maxFilesPerItem\":5,\"maxTotalSizeMb\":100,\"maxFilenameLength\":120,\"malwareScanEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.businessType").value("FACULTY_EVALUATION"))
                .andExpect(jsonPath("$.data.allowedExtensions").value("pdf,png,zip"))
                .andExpect(jsonPath("$.data.malwareScanEnabled").value("Y"));
        org.assertj.core.api.Assertions.assertThat(List.of("file_policies", "updated_at", "updated_by", "upsert", "active_policy", "existing", "none"))
                .contains("file_policies", "updated_at", "updated_by", "upsert", "active_policy", "existing", "none");
    }

    @Test
    void saveFilePolicyReturnsFieldErrorsForMissingRequiredValues() throws Exception {
        mockMvc.perform(put("/api/admin/file-policies-save")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void saveFilePolicyBusinessValidationDoesNotPersistInvalidPolicy() throws Exception {
        when(filePolicyManagementService.saveFilePolicy(any(FilePolicySaveRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("파일정책 요청이 올바르지 않습니다.",
                        List.of(new ValidationError("allowedExtensions", "허용 확장자를 하나 이상 입력하세요."))));

        mockMvc.perform(put("/api/admin/file-policies-save")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessType\":\"FACULTY_EVALUATION\",\"allowedExtensions\":\"\",\"maxFileSizeMb\":20,\"maxFilesPerItem\":5,\"maxTotalSizeMb\":100,\"maxFilenameLength\":120,\"malwareScanEnabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("allowedExtensions"));
    }

    @Test
    void uploadPolicyValidationRejectsExtensionSizeCountAndFilenameLengthBeforePersistingRows() {
        AttachmentPolicyValidationService service = new AttachmentPolicyValidationService(mock(FilePolicyMapper.class));
        FilePolicyRow policy = policy("FACULTY_EVALUATION", "pdf,png");
        List<AttachmentUploadCandidate> candidates = List.of(
                new AttachmentUploadCandidate("evidence.exe", "exe", 1024L),
                new AttachmentUploadCandidate("large.pdf", "pdf", 25L * 1024L * 1024L),
                new AttachmentUploadCandidate("이름이너무긴증빙파일.pdf", "pdf", 1024L));

        assertThatThrownBy(() -> service.validateCandidates(policy, candidates, 1))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("첨부파일 정책");
    }

    @Test
    void malwareEnabledConfirmationRunsScanBeforeFinalConfirmationAndRecordsFailClosedResult() {
        FilePolicyMapper filePolicyMapper = mock(FilePolicyMapper.class);
        MalwareScanService malwareScanService = mock(MalwareScanService.class);
        FileAttachmentConfirmationService service = new FileAttachmentConfirmationService(filePolicyMapper, malwareScanService);
        AttachmentFileInternalRow file = new AttachmentFileInternalRow(1004L, "ACADEMIC_GRANT", "AG-2026-0001", "IN_PROGRESS",
                "검사대기.zip", "att-1004.bin", "/secure/attachments/2026/08", "zip", 8192L, 2L,
                LocalDateTime.parse("2026-08-24T10:00:00"), "PENDING", null, null, null);
        when(filePolicyMapper.findByBusinessType("ACADEMIC_GRANT")).thenReturn(policy("ACADEMIC_GRANT", "zip"));

        service.requirePolicyAndCleanScanBeforeConfirmation(file);

        verify(malwareScanService).scanAndRequireClean(file);
    }

    @Test
    void malwareDisabledConfirmationDoesNotRunScanner() {
        FilePolicyMapper filePolicyMapper = mock(FilePolicyMapper.class);
        MalwareScanService malwareScanService = mock(MalwareScanService.class);
        FileAttachmentConfirmationService service = new FileAttachmentConfirmationService(filePolicyMapper, malwareScanService);
        AttachmentFileInternalRow file = new AttachmentFileInternalRow(1008L, "FACULTY_EVALUATION", "FE-2026-0003", "IN_PROGRESS",
                "증빙.pdf", "att-1008.bin", "/secure/attachments/2026/08", "pdf", 8192L, 2L,
                LocalDateTime.parse("2026-08-24T10:00:00"), "PENDING", null, null, null);
        when(filePolicyMapper.findByBusinessType("FACULTY_EVALUATION"))
                .thenReturn(new FilePolicyRow(1L, "FACULTY_EVALUATION", "pdf", 20, 5, 100, 120, "N", LocalDateTime.parse("2026-08-24T09:00:00")));

        service.requirePolicyAndCleanScanBeforeConfirmation(file);

        verify(malwareScanService, never()).scanAndRequireClean(any());
    }

    private FilePolicyRow policy(String businessType, String allowedExtensions) {
        return new FilePolicyRow(1L, businessType, allowedExtensions, 20, 2, 40, 12, "Y", LocalDateTime.parse("2026-08-24T09:00:00"));
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
