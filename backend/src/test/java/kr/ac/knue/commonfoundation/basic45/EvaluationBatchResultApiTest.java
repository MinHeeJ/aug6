package kr.ac.knue.commonfoundation.basic45;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EvaluationBatchResultController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationBatchResultApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationBatchResultService service;

    private final CurrentUser admin = new CurrentUser(9L, "admin", "E0009", "관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listReturnsBatchResultCountsWithBatchJobTypeAndTargetConditionFiltersForReq1539Req1540() throws Exception {
        EvaluationBatchResultSearchCriteria criteria = new EvaluationBatchResultSearchCriteria(
                0, 20, "B45-GENERATION-20260903-000001", "GENERATION", "EDUCATION");
        when(service.list(criteria)).thenReturn(new EvaluationBatchResultListResponse(List.of(
                new EvaluationBatchResultRow(
                        "B45-GENERATION-20260903-000001", "GENERATION", "생성", "COMPLETED",
                        "{\"evaluationYear\":\"2026\",\"areaCode\":\"EDUCATION\"}",
                        3, 1, 1, 1, "REQ-B45-SEED-003", "2026-09-03T09:00:00", "2026-09-03T09:01:00")),
                0, 20, 1));

        mockMvc.perform(get("/api/business/evaluation-batch-results")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("batchId", "B45-GENERATION-20260903-000001")
                        .param("jobType", "GENERATION")
                        .param("targetCondition", "EDUCATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].batchId").value("B45-GENERATION-20260903-000001"))
                .andExpect(jsonPath("$.data.results[0].jobType").value("GENERATION"))
                .andExpect(jsonPath("$.data.results[0].totalCount").value(3))
                .andExpect(jsonPath("$.data.results[0].successCount").value(1))
                .andExpect(jsonPath("$.data.results[0].failureCount").value(1))
                .andExpect(jsonPath("$.data.results[0].excludedCount").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listErrorsReturnsFailedTargetIdentityAndErrorDetailForReq1540Req1546() throws Exception {
        when(service.listErrors("B45-GENERATION-20260903-000001", 0, 20)).thenReturn(
                new EvaluationBatchResultErrorListResponse("B45-GENERATION-20260903-000001", List.of(
                        new EvaluationBatchResultErrorRow(
                                "B45-GENERATION-20260903-000001", "ACH-43002", "연구업적 미인증", "SOURCE_STATUS_NOT_CERTIFIED",
                                "인증 상태 원천만 평가자료로 생성할 수 있습니다.", "sourceStatus=제출")),
                        0, 20, 1));

        mockMvc.perform(get("/api/business/evaluation-batch-results/B45-GENERATION-20260903-000001/errors")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value("B45-GENERATION-20260903-000001"))
                .andExpect(jsonPath("$.data.errors[0].targetKey").value("ACH-43002"))
                .andExpect(jsonPath("$.data.errors[0].errorCode").value("SOURCE_STATUS_NOT_CERTIFIED"))
                .andExpect(jsonPath("$.data.errors[0].message").value("인증 상태 원천만 평가자료로 생성할 수 있습니다."));
    }

    @Test
    void readOnlyResultEndpointsRejectUnauthorizedRolesAndDoNotCallServiceForReq1548() throws Exception {
        mockMvc.perform(get("/api/business/evaluation-batch-results")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/business/evaluation-batch-results/B45-GENERATION-20260903-000001/errors")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden());

        verify(service, never()).list(new EvaluationBatchResultSearchCriteria(0, 20, null, null, null));
        verify(service, never()).listErrors("B45-GENERATION-20260903-000001", 0, 20);
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
