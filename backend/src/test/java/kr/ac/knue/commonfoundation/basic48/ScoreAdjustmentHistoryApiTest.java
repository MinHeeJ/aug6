package kr.ac.knue.commonfoundation.basic48;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ScoreAdjustmentHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ScoreAdjustmentHistoryApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ScoreAdjustmentHistoryService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
    private final CurrentUser auditor = new CurrentUser(8L, "score-auditor", "E0008", "점수감사자", List.of("R08"), List.of());

    @Test
    void listScoreAdjustmentHistoriesReturnsBeforeAfterValuesReasonAdjusterAndApproverForReq1541Req1542() throws Exception {
        ScoreAdjustmentHistorySearchCriteria criteria = new ScoreAdjustmentHistorySearchCriteria(0, 20, "2026", null, null,
                null, ScoreAdjustmentHistoryDataScope.ALL, null);
        when(service.list(criteria, admin.userId())).thenReturn(new ScoreAdjustmentHistorySearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/score-adjustment-histories")
                        .requestAttr("currentUser", admin)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("evaluationYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].adjustmentHistId").value("B48-ADJ-001"))
                .andExpect(jsonPath("$.data.results[0].targetUserId").value(2))
                .andExpect(jsonPath("$.data.results[0].adjustmentTarget").value("SCORE"))
                .andExpect(jsonPath("$.data.results[0].beforeValue").value(30.0))
                .andExpect(jsonPath("$.data.results[0].afterValue").value(32.0))
                .andExpect(jsonPath("$.data.results[0].adjustmentReason").value("우수 학술지 가점 반영"))
                .andExpect(jsonPath("$.data.results[0].adjustedByName").value("시스템관리자"))
                .andExpect(jsonPath("$.data.results[0].approvedByName").value("시스템관리자"));
    }

    @Test
    void getScoreAdjustmentHistoryDetailReturnsRemarkAndApprovalTraceForReq1542Req1543() throws Exception {
        when(service.getDetail("B48-ADJ-001", ScoreAdjustmentHistoryDataScope.ORGANIZATION, "KNUE-DEPT-COMP", auditor.userId()))
                .thenReturn(detail());

        mockMvc.perform(get("/api/business/score-adjustment-histories/B48-ADJ-001")
                        .requestAttr("currentUser", auditor)
                        .cookie(Basic48ApiTestSupport.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adjustmentHistId").value("B48-ADJ-001"))
                .andExpect(jsonPath("$.data.adjustmentRemark").value("상향 조정 근거: 학술지 등급 확인 후 기준 배점 가점 2점을 반영했습니다."))
                .andExpect(jsonPath("$.data.approvalTrace").value("조정 요청 접수 -> 점수산출 감사자 검토 -> R09 승인 완료"))
                .andExpect(jsonPath("$.data.readOnlyNotice").value("점수 조정 이력은 조회 전용이며 점수나 평가백분율 조정 기능을 제공하지 않습니다."));
    }

    @Test
    void r01CannotReadScoreAdjustmentHistoriesForReq1514Req1544() throws Exception {
        mockMvc.perform(get("/api/business/score-adjustment-histories")
                        .requestAttr("currentUser", teacher)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("evaluationYear", "2026"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new ScoreAdjustmentHistorySearchCriteria(0, 20, "2026", null, null,
                null, ScoreAdjustmentHistoryDataScope.ALL, null), teacher.userId());
    }

    @Test
    void downloadScoreAdjustmentHistoriesExcelReturnsFilteredMetadataAndRequestIdForReq1520Req1544() throws Exception {
        ScoreAdjustmentHistorySearchCriteria criteria = new ScoreAdjustmentHistorySearchCriteria(0, 100, "2026", "RESEARCH", null,
                "SCORE", ScoreAdjustmentHistoryDataScope.ALL, null);
        when(service.download(criteria, admin.userId())).thenReturn(new ScoreAdjustmentHistoryExcelDownload("score-adjustment-histories-2026.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 2,
                "권한과 검색조건이 적용된 점수 조정 이력 조회 결과", "REQ-B48-ADJ-DOWNLOAD-001"));

        mockMvc.perform(get("/api/business/score-adjustment-histories/download")
                        .requestAttr("currentUser", admin)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "RESEARCH")
                        .param("adjustmentTarget", "SCORE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("score-adjustment-histories-2026.xlsx"))
                .andExpect(jsonPath("$.data.rowCount").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B48-ADJ-DOWNLOAD-001"));
    }

    private ScoreAdjustmentHistoryRow row() {
        return new ScoreAdjustmentHistoryRow("B48-ADJ-001", 2L, "홍길동", "2026", "RESEARCH",
                "MI-RESEARCH-PAPER", "SCORE", new BigDecimal("30.00"), new BigDecimal("32.00"),
                "우수 학술지 가점 반영", "시스템관리자", "시스템관리자", LocalDateTime.parse("2026-09-03T10:10:00"),
                "REQ-B48-SEED-ADJ-001");
    }

    private ScoreAdjustmentHistoryDetail detail() {
        return new ScoreAdjustmentHistoryDetail("B48-ADJ-001", 2L, "홍길동", "2026", "RESEARCH", "논문",
                "MI-RESEARCH-PAPER", "SCORE", new BigDecimal("30.00"), new BigDecimal("32.00"),
                "우수 학술지 가점 반영", "상향 조정 근거: 학술지 등급 확인 후 기준 배점 가점 2점을 반영했습니다.",
                "시스템관리자", "시스템관리자", LocalDateTime.parse("2026-09-03T10:10:00"),
                LocalDateTime.parse("2026-09-03T10:40:00"), "조정 요청 접수 -> 점수산출 감사자 검토 -> R09 승인 완료",
                "REQ-B48-SEED-ADJ-001", "점수 조정 이력은 조회 전용이며 점수나 평가백분율 조정 기능을 제공하지 않습니다.");
    }
}
