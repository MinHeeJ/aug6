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

@WebMvcTest(ScoreRecalculationHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ScoreRecalculationHistoryApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ScoreRecalculationHistoryService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
    private final CurrentUser auditor = new CurrentUser(8L, "score-auditor", "E0008", "점수감사자", List.of("R08"), List.of());

    @Test
    void listScoreRecalculationHistoriesReturnsJobFormulaScopeChangedCountAndTotalsForReq1545Req1546() throws Exception {
        ScoreRecalculationHistorySearchCriteria criteria = new ScoreRecalculationHistorySearchCriteria(0, 20, "2026", null,
                "2026-09-01", "2026-09-30", ScoreRecalculationHistoryDataScope.ALL, null);
        when(service.list(criteria, admin.userId())).thenReturn(new ScoreRecalculationHistorySearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/score-recalculation-histories")
                        .requestAttr("currentUser", admin)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("executedFrom", "2026-09-01")
                        .param("executedTo", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].recalcHistId").value("B48-RECALC-001"))
                .andExpect(jsonPath("$.data.results[0].jobId").value("B48-JOB-RECALC-001"))
                .andExpect(jsonPath("$.data.results[0].formulaVersionId").value("FORMULA-2026-V2"))
                .andExpect(jsonPath("$.data.results[0].targetScope").value("FORMULA_VERSION_CHANGE"))
                .andExpect(jsonPath("$.data.results[0].changedCount").value(12))
                .andExpect(jsonPath("$.data.results[0].beforeTotalScore").value(1200.0))
                .andExpect(jsonPath("$.data.results[0].afterTotalScore").value(1236.5));
    }

    @Test
    void getScoreRecalculationHistoryDetailReturnsTargetChangesAndCriteriaWithoutExecuteCtaForReq1547Req1548() throws Exception {
        when(service.getDetail("B48-RECALC-001", ScoreRecalculationHistoryDataScope.ORGANIZATION, "KNUE-DEPT-COMP", auditor.userId()))
                .thenReturn(detail());

        mockMvc.perform(get("/api/business/score-recalculation-histories/B48-RECALC-001")
                        .requestAttr("currentUser", auditor)
                        .cookie(Basic48ApiTestSupport.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recalcHistId").value("B48-RECALC-001"))
                .andExpect(jsonPath("$.data.criteriaDetail").value("산식버전 FORMULA-2026-V1에서 FORMULA-2026-V2로 변경된 관리항목 전체"))
                .andExpect(jsonPath("$.data.targetChangeSummaryJson").value("[{\"targetUserId\":2,\"before\":120.00,\"after\":123.50,\"reason\":\"산식버전 변경\"}]"))
                .andExpect(jsonPath("$.data.readOnlyNotice").value("재계산 이력은 조회 전용이며 재계산 실행 CTA 또는 점수 수정 기능을 제공하지 않습니다."));
    }

    @Test
    void r01CannotReadScoreRecalculationHistoriesForReq1514Req1548() throws Exception {
        mockMvc.perform(get("/api/business/score-recalculation-histories")
                        .requestAttr("currentUser", teacher)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("evaluationYear", "2026"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new ScoreRecalculationHistorySearchCriteria(0, 20, "2026", null,
                null, null, ScoreRecalculationHistoryDataScope.ALL, null), teacher.userId());
    }

    @Test
    void downloadScoreRecalculationHistoriesExcelReturnsFilteredMetadataAndRequestIdForReq1520Req1548() throws Exception {
        ScoreRecalculationHistorySearchCriteria criteria = new ScoreRecalculationHistorySearchCriteria(0, 100, "2026", null,
                "2026-09-01", "2026-09-30", ScoreRecalculationHistoryDataScope.ALL, null);
        when(service.download(criteria, admin.userId())).thenReturn(new ScoreRecalculationHistoryExcelDownload("score-recalculation-histories-2026.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 3,
                "권한과 검색조건이 적용된 재계산 이력 조회 결과", "REQ-B48-RECALC-DOWNLOAD-001"));

        mockMvc.perform(get("/api/business/score-recalculation-histories/download")
                        .requestAttr("currentUser", admin)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("executedFrom", "2026-09-01")
                        .param("executedTo", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("score-recalculation-histories-2026.xlsx"))
                .andExpect(jsonPath("$.data.rowCount").value(3))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B48-RECALC-DOWNLOAD-001"));
    }

    private ScoreRecalculationHistoryRow row() {
        return new ScoreRecalculationHistoryRow("B48-RECALC-001", "B48-JOB-RECALC-001", 2L, "홍길동", "2026",
                "FORMULA-2026-V2", "FORMULA_VERSION_CHANGE", 12, new BigDecimal("1200.00"), new BigDecimal("1236.50"),
                LocalDateTime.parse("2026-09-03T13:10:00"), "REQ-B48-SEED-RECALC-001");
    }

    private ScoreRecalculationHistoryDetail detail() {
        return new ScoreRecalculationHistoryDetail("B48-RECALC-001", "B48-JOB-RECALC-001", 2L, "홍길동", "2026",
                "FORMULA-2026-V2", "FORMULA_VERSION_CHANGE", 12, new BigDecimal("1200.00"), new BigDecimal("1236.50"),
                LocalDateTime.parse("2026-09-03T13:10:00"), "산식버전 FORMULA-2026-V1에서 FORMULA-2026-V2로 변경된 관리항목 전체",
                "[{\"targetUserId\":2,\"before\":120.00,\"after\":123.50,\"reason\":\"산식버전 변경\"}]",
                "REQ-B48-SEED-RECALC-001", "재계산 이력은 조회 전용이며 재계산 실행 CTA 또는 점수 수정 기능을 제공하지 않습니다.");
    }
}
