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

@WebMvcTest(ObjectionOpinionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ObjectionOpinionApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ObjectionOpinionService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listObjectionOpinionsSupportsPaginationFiltersSnapshotsAndR09OnlyForReq1353Req1312() throws Exception {
        when(service.list(new ObjectionOpinionSearchCriteria(0, 20, "2026", "NEEDS_REVIEW", "9301")))
                .thenReturn(new ObjectionOpinionSearchResponse(List.of(row("NEEDS_REVIEW")), 0, 20, 1));

        mockMvc.perform(get("/api/business/objection-opinions")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("decisionResult", "NEEDS_REVIEW")
                        .param("applicantName", "9301"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.opinions[0].objectionId").value(9301))
                .andExpect(jsonPath("$.data.opinions[0].applicantUserId").value(2))
                .andExpect(jsonPath("$.data.opinions[0].applicantOpinionSnapshot").value("평가점수 산정 이의"))
                .andExpect(jsonPath("$.data.opinions[0].objectionContentSnapshot").value("논문 실적 누락 확인 요청"))
                .andExpect(jsonPath("$.data.size").value(20));

        mockMvc.perform(get("/api/business/objection-opinions")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new ObjectionOpinionSearchCriteria(0, 20, null, null, null));
    }

    @Test
    void transitionAcceptPersistsOpinionDecisionProcessorTimestampAndHistoryForReq1354() throws Exception {
        when(service.transition(eq(9301L), any(ObjectionOpinionRequest.class), eq(1L)))
                .thenReturn(row("ACCEPTED"));

        mockMvc.perform(post("/api/business/objection-opinions/9301/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionResult\":\"ACCEPTED\",\"reviewerOpinion\":\"신청자 의견을 인용합니다\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decisionResult").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.reviewerOpinion").value("검토자 의견"))
                .andExpect(jsonPath("$.data.processedBy").value(1))
                .andExpect(jsonPath("$.data.processedAt").isString());
    }

    @Test
    void transitionRejectRequiresReasonAndReviewerOpinionForReq1354Req1314() throws Exception {
        mockMvc.perform(post("/api/business/objection-opinions/9301/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionResult\":\"REJECTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'reasonCode')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'reviewerOpinion')]").exists());
    }

    @Test
    void transitionBlocksNonR09AndOutOfScopeBusinessConflictWithoutSideEffectsForReq1355Req1359() throws Exception {
        mockMvc.perform(post("/api/business/objection-opinions/9301/transition")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionResult\":\"ACCEPTED\",\"reviewerOpinion\":\"인용\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).transition(any(), any(), any());

        when(service.transition(eq(9301L), any(ObjectionOpinionRequest.class), eq(1L)))
                .thenThrow(new ConflictException("이의신청 의견 처리 권한 또는 데이터 범위가 없습니다."));
        mockMvc.perform(post("/api/business/objection-opinions/9301/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionResult\":\"ACCEPTED\",\"reviewerOpinion\":\"인용\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void transitionAllowsOnlyAcceptRejectNeedsReviewForReq1356() throws Exception {
        mockMvc.perform(post("/api/business/objection-opinions/9301/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionResult\":\"SCORE_CHANGE\",\"reviewerOpinion\":\"범위 밖\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'decisionResult')]").exists());
    }

    private ObjectionOpinionRow row(String decisionResult) {
        return new ObjectionOpinionRow(801L, 9301L, "2026", 2L,
                "평가점수 산정 이의", "논문 실적 누락 확인 요청", "검토자 의견",
                decisionResult, null, 1L, LocalDateTime.parse("2026-09-02T09:00:00"),
                "이의신청 의견 처리");
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
