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

@WebMvcTest(EvaluationMaterialGenerationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationMaterialGenerationApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationMaterialGenerationService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void previewReturnsOnlyCertifiedSourceCandidatesByYearAreaOrganizationAndTargetForReq1461() throws Exception {
        when(service.preview(new EvaluationMaterialGenerationSearchCriteria(0, 20, "2026", "RESEARCH_CREATION", "COLL-01", "3")))
                .thenReturn(new EvaluationMaterialGenerationPreviewResponse(List.of(
                        new EvaluationMaterialGenerationTarget(9101L, "2026", "RESEARCH_CREATION", "COLL-01", 3L,
                                "인증", "논문", "인증 원천 실적", "미생성")), 0, 20, 1, 1));

        mockMvc.perform(get("/api/business/evaluation-material-generations/preview")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "RESEARCH_CREATION")
                        .param("organizationCode", "COLL-01")
                        .param("targetUserId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targets[0].sourceAchievementId").value(9101))
                .andExpect(jsonPath("$.data.targets[0].sourceStatus").value("인증"))
                .andExpect(jsonPath("$.data.targets[0].areaCode").value("RESEARCH_CREATION"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void createGenerationCreatesMaterialsBatchRequestAndReturnsBatchIdForReq1462Req1463() throws Exception {
        when(service.create(any(EvaluationBatchActionRequest.class), any()))
                .thenReturn(new EvaluationMaterialGenerationResult("B45-GENERATION-20260903-000002", "REQ-B45-000002", 2, 2, 0));

        mockMvc.perform(post("/api/business/evaluation-material-generations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"areaCode\":\"RESEARCH_CREATION\",\"organizationCode\":\"COLL-01\",\"targetUserId\":\"3\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchId").value("B45-GENERATION-20260903-000002"))
                .andExpect(jsonPath("$.data.targetCount").value(2))
                .andExpect(jsonPath("$.data.createdCount").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B45-000002"));
    }

    @Test
    void createGenerationRequiresEvaluationYearAndBlocksNonR09WithoutSideEffectsForReq1462() throws Exception {
        mockMvc.perform(post("/api/business/evaluation-material-generations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"areaCode\":\"RESEARCH_CREATION\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'evaluationYear')]").exists());

        mockMvc.perform(post("/api/business/evaluation-material-generations")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any());
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
