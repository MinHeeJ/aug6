package kr.ac.knue.commonfoundation.basic43;

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
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AchievementVerificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AchievementVerificationApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean AchievementVerificationService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listAchievementVerificationTargetsSupportsPaginationFiltersAndR09OnlyForReq1341Req1312Req1314() throws Exception {
        when(service.list(new AchievementVerificationSearchCriteria(0, 20, "2026", "EDUCATION", "DEPARTMENT_CONFIRMED")))
                .thenReturn(new AchievementVerificationSearchResponse(List.of(row("CERTIFY", "CERTIFIED", "인증 완료")), 0, 20, 1));

        mockMvc.perform(get("/api/business/achievement-verifications")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "EDUCATION")
                        .param("verificationStatus", "DEPARTMENT_CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targets[0].achievementId").value(9101))
                .andExpect(jsonPath("$.data.targets[0].nextStatus").value("CERTIFIED"))
                .andExpect(jsonPath("$.data.targets[0].evidenceRef").value("FILE-9101"))
                .andExpect(jsonPath("$.data.size").value(20));

        mockMvc.perform(get("/api/business/achievement-verifications")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new AchievementVerificationSearchCriteria(0, 20, null, null, null));
    }

    @Test
    void transitionCertifyPersistsStatusEvidenceProcessorTimestampAndHistoryForReq1342Req1343() throws Exception {
        when(service.transition(eq(9101L), any(BusinessTransitionRequest.class), eq(1L)))
                .thenReturn(row("CERTIFY", "CERTIFIED", "인증 완료"));

        mockMvc.perform(post("/api/business/achievement-verifications/9101/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"CERTIFY\",\"opinion\":\"인증 완료\",\"evidenceRef\":\"FILE-9101\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextStatus").value("CERTIFIED"))
                .andExpect(jsonPath("$.data.evidenceRef").value("FILE-9101"))
                .andExpect(jsonPath("$.data.processedBy").value(1))
                .andExpect(jsonPath("$.data.processedAt").isString());
    }

    @Test
    void transitionReturnRequiresReasonOpinionAndEvidenceForReq1342Req1343Req1314() throws Exception {
        mockMvc.perform(post("/api/business/achievement-verifications/9101/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"RETURN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'reasonCode')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'opinion')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'evidenceRef')]").exists());
    }

    @Test
    void transitionCancelCertificationReturnsToPreviousReviewStatusNotSubmittedForReq1346() throws Exception {
        when(service.transition(eq(9101L), any(BusinessTransitionRequest.class), eq(1L)))
                .thenReturn(new AchievementVerificationRow(501L, 9101L, "2026", 1L, "CANCEL_CERTIFICATION",
                        "CERTIFIED", "DEPARTMENT_CONFIRMED", "인증취소", "FILE-9101", null,
                        1L, LocalDateTime.parse("2026-09-02T10:00:00"), "담당자 인증 처리"));

        mockMvc.perform(post("/api/business/achievement-verifications/9101/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"CANCEL_CERTIFICATION\",\"opinion\":\"재검토\",\"evidenceRef\":\"FILE-9101\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextStatus").value("DEPARTMENT_CONFIRMED"));
    }

    @Test
    void transitionBlocksNonR09AndOutOfScopeBusinessConflictWithoutSideEffectsForReq1344() throws Exception {
        mockMvc.perform(post("/api/business/achievement-verifications/9101/transition")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"CERTIFY\",\"evidenceRef\":\"FILE-9101\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).transition(any(), any(), any());

        when(service.transition(eq(9101L), any(BusinessTransitionRequest.class), eq(1L)))
                .thenThrow(new ConflictException("담당 범위 안의 실적만 처리할 수 있습니다."));
        mockMvc.perform(post("/api/business/achievement-verifications/9101/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"CERTIFY\",\"evidenceRef\":\"FILE-9101\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    private AchievementVerificationRow row(String actionType, String nextStatus, String opinion) {
        return new AchievementVerificationRow(501L, 9101L, "2026", 1L, actionType,
                "DEPARTMENT_CONFIRMED", nextStatus, opinion, "FILE-9101",
                "RETURN".equals(actionType) ? "RR001" : null, 1L,
                LocalDateTime.parse("2026-09-02T09:00:00"), "담당자 인증 처리");
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
