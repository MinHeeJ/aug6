package kr.ac.knue.commonfoundation.basic48;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoreCalculationHistoryServiceTest {
    @Mock ScoreCalculationHistoryMapper mapper;
    @InjectMocks ScoreCalculationHistoryService service;

    @Test
    void listScoreCalculationHistoriesReadsOnlyAndRecordsAuditForReq1537Req1540() {
        when(mapper.countScoreCalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countEvaluationMaterialsForReadonlyGuard()).thenReturn(3L, 3L);
        ScoreCalculationHistorySearchCriteria criteria = new ScoreCalculationHistorySearchCriteria(0, 20, "2026", null, null,
                ScoreCalculationHistoryDataScope.ALL, null, null);
        when(mapper.listScoreCalculationHistories(criteria)).thenReturn(List.of(row()));
        when(mapper.countScoreCalculationHistories(criteria)).thenReturn(1L);

        ScoreCalculationHistorySearchResponse response = service.list(criteria, 1L);

        assertThat(response.results()).extracting(ScoreCalculationHistoryRow::calcHistId).containsExactly("B48-CALC-001");
        assertThat(response.totalElements()).isEqualTo(1L);
        verify(mapper).insertReadAudit(eq("biz_score_calc_hist:list"), contains("SCR-SCORE-CALC-HISTORY"), eq(1L), contains("REQ-B48-CALC-LIST"));
    }

    @Test
    void getScoreCalculationHistoryDetailExposesSourceAchievementLinkAndCalculationStepsForReq1538Req1539() {
        when(mapper.countScoreCalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countEvaluationMaterialsForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.findScoreCalculationHistoryDetail("B48-CALC-001", ScoreCalculationHistoryDataScope.ALL, null, null)).thenReturn(detail());

        ScoreCalculationHistoryDetail detail = service.getDetail("B48-CALC-001", ScoreCalculationHistoryDataScope.ALL, null, null, 1L);

        assertThat(detail.calculationStepsJson()).contains("기준점수", "배분율", "산출점수");
        assertThat(detail.sourceAchievementLink()).contains("sourceAchievementId=9001");
        verify(mapper).insertReadAudit(eq("biz_score_calc_hist:B48-CALC-001"), contains("DETAIL"), eq(1L), contains("REQ-B48-CALC-DETAIL"));
    }

    @Test
    void downloadScoreCalculationHistoriesExcelCountsFilteredRowsAndRecordsAuditForReq1520() {
        when(mapper.countScoreCalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countEvaluationMaterialsForReadonlyGuard()).thenReturn(3L, 3L);
        ScoreCalculationHistorySearchCriteria criteria = new ScoreCalculationHistorySearchCriteria(0, 20, "2026", "RESEARCH", null,
                ScoreCalculationHistoryDataScope.ALL, null, null);
        when(mapper.countScoreCalculationHistories(new ScoreCalculationHistorySearchCriteria(0, 100, "2026", "RESEARCH", null,
                ScoreCalculationHistoryDataScope.ALL, null, null))).thenReturn(3L);

        ScoreCalculationHistoryExcelDownload download = service.download(criteria, 1L);

        assertThat(download.fileName()).isEqualTo("score-calculation-histories-2026.xlsx");
        assertThat(download.rowCount()).isEqualTo(3L);
        verify(mapper).insertReadAudit(eq("biz_score_calc_hist:download"), contains("DOWNLOAD"), eq(1L), contains("REQ-B48-CALC-DOWNLOAD"));
    }

    @Test
    void invalidEvaluationYearIsRejectedWithFieldErrorBeforeQueryForReq1537() {
        assertThatThrownBy(() -> service.list(new ScoreCalculationHistorySearchCriteria(0, 20, "26", null, null,
                ScoreCalculationHistoryDataScope.ALL, null, null), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("점수 산출 이력 조회 조건");
    }

    private ScoreCalculationHistoryRow row() {
        return new ScoreCalculationHistoryRow("B48-CALC-001", 2L, "홍길동", "2026", "RESEARCH", 9001L,
                "MI-RESEARCH-PAPER", new BigDecimal("30.00"), "SOLE", new BigDecimal("1.0000"), "N",
                "FORMULA-2026-01", 1, new BigDecimal("30.00"), "REQ-B48-SEED-CALC-001", LocalDateTime.parse("2026-09-03T09:10:00"));
    }

    private ScoreCalculationHistoryDetail detail() {
        return new ScoreCalculationHistoryDetail("B48-CALC-001", 2L, "홍길동", "2026", "RESEARCH", "논문", 9001L,
                "교육학술지 논문", "MI-RESEARCH-PAPER", new BigDecimal("30.00"), "SOLE", new BigDecimal("1.0000"), "N",
                "FORMULA-2026-01", 1, new BigDecimal("30.00"), "[{\"step\":\"기준점수\"}]",
                "/admin/achievement-data-histories?sourceAchievementId=9001", "REQ-B48-SEED-CALC-001",
                LocalDateTime.parse("2026-09-03T09:10:00"), "점수·기준점수·배분율·계산식 수정 없이 조회만 제공하는 산출근거입니다.");
    }
}
