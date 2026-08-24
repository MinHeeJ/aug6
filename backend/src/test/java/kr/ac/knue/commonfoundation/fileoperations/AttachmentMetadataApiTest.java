package kr.ac.knue.commonfoundation.fileoperations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import kr.ac.knue.commonfoundation.permissions.MenuItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttachmentMetadataController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AttachmentMetadataApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean AttachmentMetadataService service;

    @Test
    void listAttachmentsReturnsOnlyFilesLinkedToRequestedBusinessRecordForT021() throws Exception {
        when(service.listAttachments("FE-2026-0001", 0, 20, adminUser())).thenReturn(
                new AttachmentSearchResponse(List.of(publicAttachment(1001L, "FE-2026-0001", "업적증빙.pdf")), 0, 20, 1));

        mockMvc.perform(get("/api/admin/attachments")
                        .param("businessRecordId", "FE-2026-0001")
                        .param("page", "0")
                        .param("size", "20")
                        .requestAttr("currentUser", adminUser())
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attachments[0].businessRecordId").value("FE-2026-0001"))
                .andExpect(jsonPath("$.data.attachments[0].originalFilename").value("업적증빙.pdf"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        verify(service).listAttachments(eq("FE-2026-0001"), eq(0), eq(20), eq(adminUser()));
    }

    @Test
    void listAttachmentsResponseNeverExposesStoragePathOrStoredFilenameForT023() throws Exception {
        when(service.listAttachments("FE-2026-0001", 0, 20, adminUser())).thenReturn(
                new AttachmentSearchResponse(List.of(publicAttachment(1001L, "FE-2026-0001", "업적증빙.pdf")), 0, 20, 1));

        String responseBody = mockMvc.perform(get("/api/admin/attachments")
                        .param("businessRecordId", "FE-2026-0001")
                        .requestAttr("currentUser", adminUser())
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(responseBody).contains("originalFilename");
        org.assertj.core.api.Assertions.assertThat(responseBody).doesNotContain("storagePath");
        org.assertj.core.api.Assertions.assertThat(responseBody).doesNotContain("storedFilename");
        org.assertj.core.api.Assertions.assertThat(responseBody).doesNotContain("/secure/attachments");
    }

    @Test
    void getAttachmentDownloadReturnsForbiddenForUserWithoutMenuOrAdminRoleForT024() throws Exception {
        when(service.downloadAttachment(1001L, unauthorizedUser())).thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/admin/attachments/1001/download")
                        .requestAttr("currentUser", unauthorizedUser())
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void getAttachmentDownloadRechecksPermissionAndReturnsDownloadResponseForAuthorizedUserForT024AndT025() throws Exception {
        when(service.downloadAttachment(1001L, authorizedUser())).thenReturn(
                new AttachmentDownloadResponse("업적증빙.pdf", "application/pdf", "download-content".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/admin/attachments/1001/download")
                        .requestAttr("currentUser", authorizedUser())
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("filename")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("download-content".getBytes(StandardCharsets.UTF_8)));
        verify(service).downloadAttachment(1001L, authorizedUser());
    }

    private AttachmentFileRow publicAttachment(Long fileId, String recordId, String originalFilename) {
        return new AttachmentFileRow(fileId, "FACULTY_EVALUATION", recordId, "IN_PROGRESS", originalFilename,
                "pdf", 102400L, 2L, LocalDateTime.parse("2026-08-24T09:00:00"), "CLEAN", null);
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }

    private CurrentUser authorizedUser() {
        return new CurrentUser(2L, "teacher", "E0002", "일반 사용자", List.of("R01"),
                List.of(new MenuItem(155L, 150L, "첨부파일 메타정보 조회", "SCR-ATTACHMENT-METADATA", "/admin/attachments", "paperclip", 4, List.of())));
    }

    private CurrentUser unauthorizedUser() {
        return new CurrentUser(3L, "blocked", "E0003", "권한 없음", List.of("R01"), List.of());
    }
}
