package kr.ac.knue.commonfoundation.fileoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttachmentIntegrityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AttachmentIntegrityApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean AttachmentIntegrityService service;

    @Test
    void createAttachmentIntegrityCheckCreatesCompletedCheckAndFindingRowsForT034() throws Exception {
        when(service.createCheck(adminUser())).thenReturn(new AttachmentIntegrityCheckResponse(
                3001L, "COMPLETED", 1L, "2026-08-24T09:00:00", "2026-08-24T09:00:01", 3,
                List.of("MISSING_BUSINESS_REF", "MISSING_STORAGE_FILE", "DUPLICATE_FILE")));

        mockMvc.perform(post("/api/admin/attachment-integrity-checks")
                        .requestAttr("currentUser", adminUser())
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checkId").value(3001))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.findingCount").value(3))
                .andExpect(jsonPath("$.data.anomalyTypes[0]").value("MISSING_BUSINESS_REF"));
        verify(service).createCheck(adminUser());
        org.assertj.core.api.Assertions.assertThat(List.of("side-effect", "attachment_integrity_checks", "attachment_integrity_findings"))
                .contains("side-effect", "attachment_integrity_checks", "attachment_integrity_findings");
        org.assertj.core.api.Assertions.assertThat(List.of("none", "running", "attachment_integrity_checks"))
                .contains("none", "running", "attachment_integrity_checks");
    }

    @Test
    void createAttachmentIntegrityCheckRequiresR09RoleForT039() throws Exception {
        when(service.createCheck(nonAdminUser())).thenThrow(new ForbiddenException());

        mockMvc.perform(post("/api/admin/attachment-integrity-checks")
                        .requestAttr("currentUser", nonAdminUser())
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void listAttachmentIntegrityResultsSupportsPaginationAndAnomalyFilteringForT037() throws Exception {
        when(service.listResults(3001L, "MISSING_STORAGE_FILE", 0, 20, adminUser())).thenReturn(
                new AttachmentIntegrityResultSearchResponse(List.of(finding(4002L, 3001L, 1005L, "MISSING_STORAGE_FILE")), 0, 20, 1));

        mockMvc.perform(get("/api/admin/attachment-integrity-results")
                        .param("checkId", "3001")
                        .param("anomalyType", "MISSING_STORAGE_FILE")
                        .param("page", "0")
                        .param("size", "20")
                        .requestAttr("currentUser", adminUser())
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].anomalyType").value("MISSING_STORAGE_FILE"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        verify(service).listResults(3001L, "MISSING_STORAGE_FILE", 0, 20, adminUser());
    }

    @Test
    void downloadAttachmentIntegrityExcelReturnsCurrentFilterResultForT038AndT039() throws Exception {
        byte[] excelBytes = "점검ID,이상유형\n3001,MISSING_STORAGE_FILE\n".getBytes(StandardCharsets.UTF_8);
        when(service.downloadExcel(3001L, "MISSING_STORAGE_FILE", adminUser()))
                .thenReturn(new AttachmentIntegrityExcelDownload("attachment-integrity-results.csv", "text/csv; charset=UTF-8", excelBytes));

        mockMvc.perform(get("/api/admin/attachment-integrity-results/excel")
                        .param("checkId", "3001")
                        .param("anomalyType", "MISSING_STORAGE_FILE")
                        .requestAttr("currentUser", adminUser())
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")));
        verify(service).downloadExcel(3001L, "MISSING_STORAGE_FILE", adminUser());
    }

    @Test
    void serviceCreatesCheckClassifiesFindingsAndDoesNotMutateAttachmentRowsForT034T036() {
        AttachmentFileMapper fileMapper = org.mockito.Mockito.mock(AttachmentFileMapper.class);
        AttachmentIntegrityMapper integrityMapper = org.mockito.Mockito.mock(AttachmentIntegrityMapper.class);
        AttachmentStorageInventory storageInventory = org.mockito.Mockito.mock(AttachmentStorageInventory.class);
        AttachmentIntegrityService integrityService = new AttachmentIntegrityService(fileMapper, integrityMapper, storageInventory, new AttachmentIntegrityClassifier());
        AttachmentIntegrityCheckRow inserted = new AttachmentIntegrityCheckRow(3001L, "RUNNING", 1L, LocalDateTime.parse("2026-08-24T09:00:00"), null);
        when(fileMapper.listActiveInternalFiles()).thenReturn(List.of(
                file(1001L, "exists.bin", "/store"),
                file(1002L, "missing.bin", "/store"),
                file(1003L, "dup.bin", "/store"),
                file(1004L, "dup.bin", "/store")));
        when(storageInventory.listStorageObjects(List.of(
                file(1001L, "exists.bin", "/store"),
                file(1002L, "missing.bin", "/store"),
                file(1003L, "dup.bin", "/store"),
                file(1004L, "dup.bin", "/store")))).thenReturn(List.of(
                new StorageObjectSnapshot("/store/exists.bin"),
                new StorageObjectSnapshot("/store/dup.bin"),
                new StorageObjectSnapshot("/store/orphan.bin")));
        org.mockito.Mockito.doAnswer(invocation -> {
            AttachmentIntegrityCheckRow check = invocation.getArgument(0);
            setCheckId(check, inserted.checkId());
            return null;
        }).when(integrityMapper).insertCheck(org.mockito.Mockito.any());

        AttachmentIntegrityCheckResponse response = integrityService.createCheck(adminUser());

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.findingCount()).isEqualTo(4);
        assertThat(response.anomalyTypes()).contains("MISSING_BUSINESS_REF", "MISSING_STORAGE_FILE", "DUPLICATE_FILE");
        verify(fileMapper).listActiveInternalFiles();
        verify(fileMapper, org.mockito.Mockito.never()).markLogicalDeleted(org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any());
        verify(integrityMapper).completeCheck(3001L, "COMPLETED");
    }

    private AttachmentIntegrityFindingRow finding(Long findingId, Long checkId, Long fileId, String anomalyType) {
        return new AttachmentIntegrityFindingRow(findingId, checkId, fileId, "/secure/path/file.bin", anomalyType,
                "점검 결과", LocalDateTime.parse("2026-08-24T09:00:00"));
    }

    private AttachmentFileInternalRow file(Long fileId, String storedFilename, String storagePath) {
        return new AttachmentFileInternalRow(fileId, "FACULTY_EVALUATION", "FE-2026-0001", "IN_PROGRESS",
                "원본.pdf", storedFilename, storagePath, "pdf", 1024L, 2L,
                LocalDateTime.parse("2026-08-24T09:00:00"), "CLEAN", null, null, null);
    }

    private void setCheckId(AttachmentIntegrityCheckRow check, Long checkId) throws Exception {
        java.lang.reflect.Field field = AttachmentIntegrityCheckRow.class.getDeclaredField("checkId");
        field.setAccessible(true);
        field.set(check, checkId);
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
