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
class ScoreRecalculationHistoryServiceTest {
    @Mock ScoreRecalculationHistoryMapper mapper;
    @InjectMocks ScoreRecalculationHistoryService service;

    @Test
    void listScoreRecalculationHistoriesReadsOnlyAndRecordsAuditForReq1545Req1548() {
        when(mapper.countScoreRecalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countScoreCalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        ScoreRecalculationHistorySearchCriteria criteria = new ScoreRecalculationHistorySearchCriteria(0, 20, "2026", null,
                "2026-09-01", "2026-09-30", ScoreRecalculationHistoryDataScope.ALL, null);
        when(mapper.listScoreRecalculationHistories(criteria)).thenReturn(List.of(row()));
        when(mapper.countScoreRecalculationHistories(criteria)).thenReturn(1L);

        ScoreRecalculationHistorySearchResponse response = service.list(criteria, 1L);

        assertThat(response.results()).extracting(ScoreRecalculationHistoryRow::recalcHistId).containsExactly("B48-RECALC-001");
        assertThat(response.results().get(0).beforeTotalScore()).isEqualByComparingTo("1200.00");
        assertThat(response.results().get(0).afterTotalScore()).isEqualByComparingTo("1236.50");
        verify(mapper).insertReadAudit(eq("biz_recalc_hist:list"), contains("SCR-SCORE-RECALCULATION-HISTORY"), eq(1L), contains("REQ-B48-RECALC-LIST"));
    }

    @Test
    void getScoreRecalculationHistoryDetailExposesCriteriaAndTargetChangesForReq1546Req1547() {
        when(mapper.countScoreRecalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countScoreCalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.findScoreRecalculationHistoryDetail("B48-RECALC-001", ScoreRecalculationHistoryDataScope.ALL, null)).thenReturn(detail());

        ScoreRecalculationHistoryDetail detail = service.getDetail("B48-RECALC-001", ScoreRecalculationHistoryDataScope.ALL, null, 1L);

        assertThat(detail.criteriaDetail()).contains("산식버전");
        assertThat(detail.targetChangeSummaryJson()).contains("targetUserId");
        assertThat(detail.readOnlyNotice()).contains("재계산 실행 CTA");
        verify(mapper).insertReadAudit(eq("biz_recalc_hist:B48-RECALC-001"), contains("DETAIL"), eq(1L), contains("REQ-B48-RECALC-DETAIL"));
    }

    @Test
    void downloadScoreRecalculationHistoriesExcelCountsFilteredRowsAndRecordsAuditForReq1520() {
        when(mapper.countScoreRecalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countScoreCalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        ScoreRecalculationHistorySearchCriteria criteria = new ScoreRecalculationHistorySearchCriteria(0, 20, "2026", null,
                "2026-09-01", "2026-09-30", ScoreRecalculationHistoryDataScope.ALL, null);
        when(mapper.countScoreRecalculationHistories(new ScoreRecalculationHistorySearchCriteria(0, 100, "2026", null,
                "2026-09-01", "2026-09-30", ScoreRecalculationHistoryDataScope.ALL, null))).thenReturn(3L);

        ScoreRecalculationHistoryExcelDownload download = service.download(criteria, 1L);

        assertThat(download.fileName()).isEqualTo("score-recalculation-histories-2026.xlsx");
        assertThat(download.rowCount()).isEqualTo(3L);
        verify(mapper).insertReadAudit(eq("biz_recalc_hist:download"), contains("DOWNLOAD"), eq(1L), contains("REQ-B48-RECALC-DOWNLOAD"));
    }

    @Test
    void invalidExecutionPeriodIsRejectedWithFieldErrorBeforeQueryForReq1545() {
        assertThatThrownBy(() -> service.list(new ScoreRecalculationHistorySearchCriteria(0, 20, "2026", null,
                "2026-09-30", "2026-09-01", ScoreRecalculationHistoryDataScope.ALL, null), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("재계산 이력 조회 조건");
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
