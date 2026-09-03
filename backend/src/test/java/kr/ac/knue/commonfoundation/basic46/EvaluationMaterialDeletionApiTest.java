package kr.ac.knue.commonfoundation.basic46;

import static org.mockito.ArgumentMatchers.any;
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
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EvaluationMaterialDeletionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationMaterialDeletionApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationMaterialDeletionService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void previewEvaluationMaterialDeletionReturnsPreviewTokenTargetsAndCountsForReq1475Req1476() throws Exception {
        when(service.preview(new EvaluationMaterialDeletionSearchCriteria(0, 20, "2026", "RESEARCH_CREATION", "B46-BATCH-GEN-001")))
                .thenReturn(new EvaluationMaterialDeletionPreviewResponse(
                        List.of(row(460001L, "CERTIFIED", true, null)),
                        0,
                        20,
                        1,
                        1,
                        "B46-PREVIEW-2026-RESEARCH_CREATION-B46-BATCH-GEN-001"));

        mockMvc.perform(get("/api/business/evaluation-material-deletions/preview")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "RESEARCH_CREATION")
                        .param("generationBatchId", "B46-BATCH-GEN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.previewToken").value("B46-PREVIEW-2026-RESEARCH_CREATION-B46-BATCH-GEN-001"))
                .andExpect(jsonPath("$.data.deletableCount").value(1))
                .andExpect(jsonPath("$.data.targets[0].evaluationMaterialId").value(460001))
                .andExpect(jsonPath("$.data.targets[0].canDelete").value(true));

        mockMvc.perform(get("/api/business/evaluation-material-deletions/preview")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).preview(new EvaluationMaterialDeletionSearchCriteria(0, 20, null, null, null));
    }

    @Test
    void createEvaluationMaterialDeletionReturnsDeletionBatchIdAndLogicalDeleteCountsForReq1477Req1478() throws Exception {
        when(service.delete(any(EvaluationMaterialDeletionRequest.class), eq(1L)))
                .thenReturn(new EvaluationMaterialDeletionResult("B46-DEL-REQ-001", "2026", "RESEARCH_CREATION",
                        "B46-BATCH-GEN-001", 2, 1, 0, 1, "REQ-B46-DEL-001"));

        mockMvc.perform(post("/api/business/evaluation-material-deletions")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"areaCode\":\"RESEARCH_CREATION\",\"generationBatchId\":\"B46-BATCH-GEN-001\",\"previewToken\":\"B46-PREVIEW-2026-RESEARCH_CREATION-B46-BATCH-GEN-001\",\"deletionReason\":\"잘못 생성된 평가자료 삭제\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletionBatchId").value("B46-DEL-REQ-001"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.excludedCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B46-DEL-001"));
    }

    @Test
    void createEvaluationMaterialDeletionRequiresPreviewTokenAndReasonAndBlocksNonR09ForReq1478Req1479() throws Exception {
        mockMvc.perform(post("/api/business/evaluation-material-deletions")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'evaluationYear')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'areaCode')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'generationBatchId')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'previewToken')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'deletionReason')]").exists());

        mockMvc.perform(post("/api/business/evaluation-material-deletions")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"areaCode\":\"RESEARCH_CREATION\",\"generationBatchId\":\"B46-BATCH-GEN-001\",\"previewToken\":\"B46-PREVIEW-2026-RESEARCH_CREATION-B46-BATCH-GEN-001\",\"deletionReason\":\"삭제\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).delete(any(), eq(2L));
    }

    private EvaluationMaterialDeletionTarget row(Long materialId, String finalStatus, boolean canDelete, String excludedReason) {
        return new EvaluationMaterialDeletionTarget(materialId, "2026", "RESEARCH_CREATION", "KNUE-DEPT-COMP", 2L,
                846001L, "B46-BATCH-GEN-001", finalStatus, canDelete, excludedReason,
                LocalDateTime.parse("2026-09-03T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
