package kr.ac.knue.commonfoundation.basic45;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
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
    void previewReturnsGeneratedDeletionTargetsByYearAreaAndGenerationBatchForReq1481() throws Exception {
        when(service.preview(new EvaluationMaterialDeletionSearchCriteria(0, 20, "2026", "EDUCATION", "B45-GENERATION-20260903-000001")))
                .thenReturn(new EvaluationMaterialDeletionPreviewResponse(List.of(
                        new EvaluationMaterialDeletionTarget(45001L, "2026", "EDUCATION", 1L,
                                43001L, "B45-GENERATION-20260903-000001", "인증", "BATCH_GENERATED", "생성 평가자료")), 0, 20, 1, 1));

        mockMvc.perform(get("/api/business/evaluation-material-deletions/preview")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "EDUCATION")
                        .param("generationBatchId", "B45-GENERATION-20260903-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targets[0].evaluationMaterialId").value(45001))
                .andExpect(jsonPath("$.data.targets[0].generationBatchId").value("B45-GENERATION-20260903-000001"))
                .andExpect(jsonPath("$.data.targets[0].materialOrigin").value("BATCH_GENERATED"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void deleteReasonMissingReturnsFieldErrorAndDoesNotCallServiceForReq1499() throws Exception {
        mockMvc.perform(post("/api/business/evaluation-material-deletions")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"areaCode\":\"EDUCATION\",\"generationBatchId\":\"B45-GENERATION-20260903-000001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'deleteReason')]").exists());

        verify(service, never()).delete(any(), any());
    }

    @Test
    void createDeletionLogicallyDeletesOnlyPreviewTargetsAndReturnsCountsForReq1499() throws Exception {
        when(service.delete(any(EvaluationBatchActionRequest.class), any()))
                .thenReturn(new EvaluationMaterialDeletionResult("B45-DELETION-20260903-000001", "REQ-B45-DELETION-TEST", 1, 1, 0));

        mockMvc.perform(post("/api/business/evaluation-material-deletions")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"areaCode\":\"EDUCATION\",\"generationBatchId\":\"B45-GENERATION-20260903-000001\",\"deleteReason\":\"잘못 생성된 평가자료 재생성\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchId").value("B45-DELETION-20260903-000001"))
                .andExpect(jsonPath("$.data.targetCount").value(1))
                .andExpect(jsonPath("$.data.deletedCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B45-DELETION-TEST"));
    }

    @Test
    void deletionRequiresR09AndBlocksUnauthorizedNoChangeForReq1481() throws Exception {
        mockMvc.perform(post("/api/business/evaluation-material-deletions")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"generationBatchId\":\"B45-GENERATION-20260903-000001\",\"deleteReason\":\"권한 없음 검증\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).delete(any(), any());
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
