package kr.ac.knue.commonfoundation.fileoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class Phase7SecurityPerformanceOperationsTest {
    @Test
    void listServiceQueriesCompleteUnderThreeSecondBudgetForPhase7Performance() {
        FilePolicyMapper policyMapper = mock(FilePolicyMapper.class);
        when(policyMapper.searchFilePolicies(any())).thenReturn(List.of(
                new FilePolicyRow(1L, "COMMON_ATTACHMENT", "pdf,png,zip", 20, 5, 100, 120, "Y", LocalDateTime.parse("2026-08-24T09:00:00"))));
        when(policyMapper.countFilePolicies(any())).thenReturn(1L);
        FilePolicyManagementService service = new FilePolicyManagementService(policyMapper);

        long started = System.nanoTime();
        FilePolicySearchResponse response = service.listFilePolicies(0, 20, "COMMON");
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    void backupSmokeScriptDocumentsDailyScheduleRetentionAndRestoreRehearsal() throws Exception {
        ClassPathResource script = new ClassPathResource("smoke/postgres-backup-restore-rehearsal.sh");
        String content = script.getContentAsString(StandardCharsets.UTF_8);

        assertThat(content).contains("BACKUP_RETENTION_DAYS");
        assertThat(content).contains("pg_dump");
        assertThat(content).contains("pg_restore");
        assertThat(content).contains("7", "30");
    }
}

@WebMvcTest({FilePolicyManagementController.class, AttachmentMetadataController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Phase7SecureCodingMvcTest {
    @Autowired MockMvc mockMvc;
    @MockBean FilePolicyManagementService filePolicyManagementService;
    @MockBean AttachmentMetadataService attachmentMetadataService;

    @Test
    void filePolicySaveRequiresR09ForPhase7SecureCoding() throws Exception {
        mockMvc.perform(put("/api/admin/file-policies-save")
                        .requestAttr("currentUser", nonAdminUser())
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessType\":\"COMMON_ATTACHMENT\",\"allowedExtensions\":\"pdf\",\"maxFileSizeMb\":20,\"maxFilesPerItem\":5,\"maxTotalSizeMb\":100,\"maxFilenameLength\":120,\"malwareScanEnabled\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void attachmentDownloadAddsNoSniffAndNoStoreHeadersForPhase7SecureCoding() throws Exception {
        when(attachmentMetadataService.downloadAttachment(eq(1001L), eq(adminUser()))).thenReturn(
                new AttachmentDownloadResponse("업적증빙.pdf", "application/pdf", "download".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/admin/attachments/1001/download")
                        .requestAttr("currentUser", adminUser())
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }

    private CurrentUser nonAdminUser() {
        return new CurrentUser(2L, "teacher", "E0002", "일반 사용자", List.of("R01"), List.of());
    }
}
