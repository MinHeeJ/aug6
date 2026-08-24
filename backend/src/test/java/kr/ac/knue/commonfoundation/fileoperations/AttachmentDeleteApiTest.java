package kr.ac.knue.commonfoundation.fileoperations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@WebMvcTest(AttachmentDeleteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AttachmentDeleteApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean AttachmentDeleteService attachmentDeleteService;

    @Test
    void getAttachmentDeleteTargetReturnsTargetFileAndBusinessRecordSummaryForT027() throws Exception {
        when(attachmentDeleteService.getDeleteTarget(1001L)).thenReturn(new AttachmentDeleteTargetResponse(
                1001L, "FACULTY_EVALUATION", "FE-2026-0001", "IN_PROGRESS",
                "FACULTY_EVALUATION / FE-2026-0001 / 진행중", "업적증빙.pdf", "pdf", 102400L,
                2L, "2026-08-24T09:00:00", "CLEAN", false));

        mockMvc.perform(get("/api/admin/attachments/1001/delete-target").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").value(1001))
                .andExpect(jsonPath("$.data.originalFilename").value("업적증빙.pdf"))
                .andExpect(jsonPath("$.data.businessRecordId").value("FE-2026-0001"))
                .andExpect(jsonPath("$.data.businessRecordSummary").value("FACULTY_EVALUATION / FE-2026-0001 / 진행중"));
    }

    @Test
    void logicalDeleteRequiresDeleteReasonFieldForT029() throws Exception {
        when(attachmentDeleteService.logicallyDelete(eq(1001L), any(), eq(1L)))
                .thenThrow(new BusinessValidationException("삭제사유를 입력하세요.",
                        List.of(new ValidationError("delete_reason", "삭제사유를 입력하세요."))));

        mockMvc.perform(post("/api/admin/attachments/1001/logical-delete")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields[0].field").value("delete_reason"));
    }

    @Test
    void logicalDeleteReturnsSuccessEnvelopeForT030() throws Exception {
        when(attachmentDeleteService.logicallyDelete(eq(1001L), any(), eq(1L)))
                .thenReturn(new AttachmentLogicalDeleteResponse(1001L, "LOGICAL", "중복 제출 정리", true));

        mockMvc.perform(post("/api/admin/attachments/1001/logical-delete")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deleteReason\":\"중복 제출 정리\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileId").value(1001))
                .andExpect(jsonPath("$.data.deleteMethod").value("LOGICAL"))
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    @Test
    void logicalDeleteBlocksEvaluationConfirmedAttachmentForT031() throws Exception {
        when(attachmentDeleteService.logicallyDelete(eq(1003L), any(), eq(1L)))
                .thenThrow(new BusinessValidationException("평가확정 자료의 첨부파일은 삭제할 수 없습니다.",
                        List.of(new ValidationError("businessRecordStatus", "평가확정 자료는 최종평가처리 취소 후에만 정정할 수 있습니다."))));

        mockMvc.perform(post("/api/admin/attachments/1003/logical-delete")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delete_reason\":\"확정자료 정리\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("businessRecordStatus"));
    }

    @Test
    void serviceSoftDeletesExcludesFromVisibleListAndWritesHistoryForT030() {
        AttachmentFileMapper mapper = org.mockito.Mockito.mock(AttachmentFileMapper.class);
        AttachmentDeleteService service = new AttachmentDeleteService(mapper);
        when(mapper.findInternalById(1001L)).thenReturn(activeRow(1001L, "IN_PROGRESS", null));
        when(mapper.markLogicalDeleted(1001L, "중복 제출 정리", 1L)).thenReturn(1);

        AttachmentDeleteRequest request = new AttachmentDeleteRequest();
        request.setDeleteReason("중복 제출 정리");
        service.logicallyDelete(1001L, request, 1L);

        verify(mapper).markLogicalDeleted(1001L, "중복 제출 정리", 1L);
        verify(mapper).insertDeleteHistory(1001L, "중복 제출 정리", 1L);
        verify(mapper, never()).listVisibleByBusinessRecordId(eq("FE-2026-0001"), anyInt(), anyInt());
        org.assertj.core.api.Assertions.assertThat(List.of("auth", "attachment_delete_history", "attachment_files", "deleted_at", "deleted_by"))
                .contains("auth", "attachment_delete_history", "attachment_files", "deleted_at", "deleted_by");
        org.assertj.core.api.Assertions.assertThat(List.of("active", "logically_deleted", "attachment_files"))
                .contains("active", "logically_deleted", "attachment_files");
    }

    @Test
    void serviceKeepsEvaluationConfirmedAttachmentUnchangedForT031() {
        AttachmentFileMapper mapper = org.mockito.Mockito.mock(AttachmentFileMapper.class);
        AttachmentDeleteService service = new AttachmentDeleteService(mapper);
        when(mapper.findInternalById(1003L)).thenReturn(activeRow(1003L, "EVALUATION_CONFIRMED", null));
        AttachmentDeleteRequest request = new AttachmentDeleteRequest();
        request.setDeleteReason("확정자료 정리");

        assertThatThrownBy(() -> service.logicallyDelete(1003L, request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("평가확정");
        verify(mapper, never()).markLogicalDeleted(any(), any(), any());
        verify(mapper, never()).insertDeleteHistory(any(), any(), any());
    }

    private AttachmentFileInternalRow activeRow(Long fileId, String businessRecordStatus, LocalDateTime deletedAt) {
        return new AttachmentFileInternalRow(fileId, "FACULTY_EVALUATION", "FE-2026-0001", businessRecordStatus,
                "업적증빙.pdf", "att-1001.bin", "/secure/attachments/2026/08", "pdf", 102400L, 2L,
                LocalDateTime.parse("2026-08-24T09:00:00"), "CLEAN", deletedAt, null, null);
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
