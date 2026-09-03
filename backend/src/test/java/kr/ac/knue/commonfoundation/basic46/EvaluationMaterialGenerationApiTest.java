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

@WebMvcTest(EvaluationMaterialGenerationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationMaterialGenerationApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationMaterialGenerationService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationMaterialGenerationsReturnsConditionsBatchIdCountsAndOnlyR09ForReq1471Req1473() throws Exception {
        when(service.list(new EvaluationMaterialGenerationSearchCriteria(0, 20, "2026", "RESEARCH_CREATION", "KNUE-DEPT-COMP", 2L)))
                .thenReturn(new EvaluationMaterialGenerationSearchResponse(List.of(target("GENERATED", "B46-BATCH-GEN-001", null)), 0, 20, 1));

        mockMvc.perform(get("/api/business/evaluation-material-generations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "RESEARCH_CREATION")
                        .param("organizationCode", "KNUE-DEPT-COMP")
                        .param("targetUserId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targets[0].sourceAchievementId").value(846001))
                .andExpect(jsonPath("$.data.targets[0].generationStatus").value("GENERATED"))
                .andExpect(jsonPath("$.data.targets[0].generationBatchId").value("B46-BATCH-GEN-001"))
                .andExpect(jsonPath("$.data.size").value(20));

        mockMvc.perform(get("/api/business/evaluation-material-generations")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new EvaluationMaterialGenerationSearchCriteria(0, 20, null, null, null, null));
    }

    @Test
    void createEvaluationMaterialGenerationReturnsGenerationBatchIdAndCountsForReq1472Req1473() throws Exception {
        when(service.create(any(EvaluationMaterialGenerationRequest.class), eq(1L)))
                .thenReturn(new EvaluationMaterialGenerationResult("B46-GEN-REQ-001", "2026", "RESEARCH_CREATION",
                        "KNUE-DEPT-COMP", 2L, 3, 2, 0, 1, "REQ-B46-GEN-001"));

        mockMvc.perform(post("/api/business/evaluation-material-generations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"areaCode\":\"RESEARCH_CREATION\",\"organizationCode\":\"KNUE-DEPT-COMP\",\"targetUserId\":2,\"reason\":\"인증 실적 평가자료 생성\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationBatchId").value("B46-GEN-REQ-001"))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.excludedCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B46-GEN-001"));
    }

    @Test
    void createEvaluationMaterialGenerationRequiresYearAreaReasonAndBlocksNonR09ForReq1474() throws Exception {
        mockMvc.perform(post("/api/business/evaluation-material-generations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'evaluationYear')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'areaCode')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'reason')]").exists());

        mockMvc.perform(post("/api/business/evaluation-material-generations")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"areaCode\":\"RESEARCH_CREATION\",\"reason\":\"생성\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any(), eq(2L));
    }

    private EvaluationMaterialGenerationTarget target(String generationStatus, String batchId, String excludedReason) {
        return new EvaluationMaterialGenerationTarget(846001L, "2026", "RESEARCH_CREATION", "KNUE-DEPT-COMP", 2L,
                "CERTIFIED", generationStatus, batchId, LocalDateTime.parse("2026-09-03T09:00:00"), excludedReason);
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
