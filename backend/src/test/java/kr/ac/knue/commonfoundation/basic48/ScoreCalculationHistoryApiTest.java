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

@WebMvcTest(ScoreCalculationHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ScoreCalculationHistoryApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ScoreCalculationHistoryService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
    private final CurrentUser auditor = new CurrentUser(8L, "score-auditor", "E0008", "점수산출감사자", List.of("R08"), List.of());

    @Test
    void listScoreCalculationHistoriesReturnsSourceBasisAndGenerationRowsForReq1537Req1538() throws Exception {
        ScoreCalculationHistorySearchCriteria criteria = new ScoreCalculationHistorySearchCriteria(0, 20, "2026", null, null,
                ScoreCalculationHistoryDataScope.ALL, null, null);
        when(service.list(criteria, admin.userId())).thenReturn(new ScoreCalculationHistorySearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/score-calculation-histories")
                        .requestAttr("currentUser", admin)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("evaluationYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].calcHistId").value("B48-CALC-001"))
                .andExpect(jsonPath("$.data.results[0].targetUserId").value(2))
                .andExpect(jsonPath("$.data.results[0].managementItemCode").value("MI-RESEARCH-PAPER"))
                .andExpect(jsonPath("$.data.results[0].baseScore").value(30.0))
                .andExpect(jsonPath("$.data.results[0].distributionRate").value(1.0))
                .andExpect(jsonPath("$.data.results[0].formulaVersionId").value("FORMULA-2026-01"))
                .andExpect(jsonPath("$.data.results[0].generationNo").value(1));
    }

    @Test
    void getScoreCalculationHistoryDetailReturnsCalculationStepsAndSourceAchievementLinkForReq1538Req1539() throws Exception {
        when(service.getDetail("B48-CALC-001", ScoreCalculationHistoryDataScope.ORGANIZATION, "KNUE-DEPT-COMP", null, auditor.userId()))
                .thenReturn(detail());

        mockMvc.perform(get("/api/business/score-calculation-histories/B48-CALC-001")
                        .requestAttr("currentUser", auditor)
                        .cookie(Basic48ApiTestSupport.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.calcHistId").value("B48-CALC-001"))
                .andExpect(jsonPath("$.data.sourceAchievementLink").value("/admin/achievement-data-histories?sourceAchievementId=9001"))
                .andExpect(jsonPath("$.data.calculationStepsJson").value("[{\"step\":\"기준점수\",\"value\":30.00},{\"step\":\"배분율\",\"value\":1.0000},{\"step\":\"산출점수\",\"value\":30.00}]"))
                .andExpect(jsonPath("$.data.readOnlyNotice").value("점수·기준점수·배분율·계산식 수정 없이 조회만 제공하는 산출근거입니다."));
    }

    @Test
    void r01CanReadOnlyOwnScoreCalculationHistoriesAndCannotRequestOtherTargetForReq1512() throws Exception {
        mockMvc.perform(get("/api/business/score-calculation-histories")
                        .requestAttr("currentUser", teacher)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("targetUserId", "1")
                        .param("evaluationYear", "2026"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new ScoreCalculationHistorySearchCriteria(0, 20, "2026", null, 1L,
                ScoreCalculationHistoryDataScope.SELF, null, teacher.userId()), teacher.userId());
    }

    @Test
    void downloadScoreCalculationHistoriesExcelReturnsFilteredMetadataAndRequestIdForReq1520Req1540() throws Exception {
        ScoreCalculationHistorySearchCriteria criteria = new ScoreCalculationHistorySearchCriteria(0, 100, "2026", "RESEARCH", null,
                ScoreCalculationHistoryDataScope.ALL, null, null);
        when(service.download(criteria, admin.userId())).thenReturn(new ScoreCalculationHistoryExcelDownload("score-calculation-histories-2026.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 3,
                "권한과 검색조건이 적용된 점수 산출 이력 조회 결과", "REQ-B48-CALC-DOWNLOAD-001"));

        mockMvc.perform(get("/api/business/score-calculation-histories/download")
                        .requestAttr("currentUser", admin)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "RESEARCH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("score-calculation-histories-2026.xlsx"))
                .andExpect(jsonPath("$.data.rowCount").value(3))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B48-CALC-DOWNLOAD-001"));
    }

    private ScoreCalculationHistoryRow row() {
        return new ScoreCalculationHistoryRow("B48-CALC-001", 2L, "홍길동", "2026", "RESEARCH", 9001L,
                "MI-RESEARCH-PAPER", new BigDecimal("30.00"), "SOLE", new BigDecimal("1.0000"), "N",
                "FORMULA-2026-01", 1, new BigDecimal("30.00"), "REQ-B48-SEED-CALC-001", LocalDateTime.parse("2026-09-03T09:10:00"));
    }

    private ScoreCalculationHistoryDetail detail() {
        return new ScoreCalculationHistoryDetail("B48-CALC-001", 2L, "홍길동", "2026", "RESEARCH", "논문", 9001L,
                "교육학술지 논문", "MI-RESEARCH-PAPER", new BigDecimal("30.00"), "SOLE", new BigDecimal("1.0000"), "N",
                "FORMULA-2026-01", 1, new BigDecimal("30.00"),
                "[{\"step\":\"기준점수\",\"value\":30.00},{\"step\":\"배분율\",\"value\":1.0000},{\"step\":\"산출점수\",\"value\":30.00}]",
                "/admin/achievement-data-histories?sourceAchievementId=9001", "REQ-B48-SEED-CALC-001",
                LocalDateTime.parse("2026-09-03T09:10:00"), "점수·기준점수·배분율·계산식 수정 없이 조회만 제공하는 산출근거입니다.");
    }
}
