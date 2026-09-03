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
class ScoreAdjustmentHistoryServiceTest {
    @Mock ScoreAdjustmentHistoryMapper mapper;
    @InjectMocks ScoreAdjustmentHistoryService service;

    @Test
    void listScoreAdjustmentHistoriesReadsOnlyAndRecordsAuditForReq1541Req1544() {
        when(mapper.countScoreAdjustmentHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countScoreCalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        ScoreAdjustmentHistorySearchCriteria criteria = new ScoreAdjustmentHistorySearchCriteria(0, 20, "2026", null, null,
                null, ScoreAdjustmentHistoryDataScope.ALL, null);
        when(mapper.listScoreAdjustmentHistories(criteria)).thenReturn(List.of(row()));
        when(mapper.countScoreAdjustmentHistories(criteria)).thenReturn(1L);

        ScoreAdjustmentHistorySearchResponse response = service.list(criteria, 1L);

        assertThat(response.results()).extracting(ScoreAdjustmentHistoryRow::adjustmentHistId).containsExactly("B48-ADJ-001");
        assertThat(response.results().get(0).beforeValue()).isEqualByComparingTo("30.00");
        assertThat(response.results().get(0).afterValue()).isEqualByComparingTo("32.00");
        verify(mapper).insertReadAudit(eq("biz_score_adj_hist:list"), contains("SCR-SCORE-ADJUSTMENT-HISTORY"), eq(1L), contains("REQ-B48-ADJ-LIST"));
    }

    @Test
    void getScoreAdjustmentHistoryDetailExposesRemarkAndApprovalTraceForReq1542Req1543() {
        when(mapper.countScoreAdjustmentHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countScoreCalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.findScoreAdjustmentHistoryDetail("B48-ADJ-001", ScoreAdjustmentHistoryDataScope.ALL, null)).thenReturn(detail());

        ScoreAdjustmentHistoryDetail detail = service.getDetail("B48-ADJ-001", ScoreAdjustmentHistoryDataScope.ALL, null, 1L);

        assertThat(detail.adjustmentRemark()).contains("상향 조정 근거");
        assertThat(detail.approvalTrace()).contains("승인 완료");
        assertThat(detail.readOnlyNotice()).contains("조회 전용");
        verify(mapper).insertReadAudit(eq("biz_score_adj_hist:B48-ADJ-001"), contains("DETAIL"), eq(1L), contains("REQ-B48-ADJ-DETAIL"));
    }

    @Test
    void downloadScoreAdjustmentHistoriesExcelCountsFilteredRowsAndRecordsAuditForReq1520() {
        when(mapper.countScoreAdjustmentHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countScoreCalculationHistoriesForReadonlyGuard()).thenReturn(3L, 3L);
        ScoreAdjustmentHistorySearchCriteria criteria = new ScoreAdjustmentHistorySearchCriteria(0, 20, "2026", "RESEARCH", null,
                "SCORE", ScoreAdjustmentHistoryDataScope.ALL, null);
        when(mapper.countScoreAdjustmentHistories(new ScoreAdjustmentHistorySearchCriteria(0, 100, "2026", "RESEARCH", null,
                "SCORE", ScoreAdjustmentHistoryDataScope.ALL, null))).thenReturn(2L);

        ScoreAdjustmentHistoryExcelDownload download = service.download(criteria, 1L);

        assertThat(download.fileName()).isEqualTo("score-adjustment-histories-2026.xlsx");
        assertThat(download.rowCount()).isEqualTo(2L);
        verify(mapper).insertReadAudit(eq("biz_score_adj_hist:download"), contains("DOWNLOAD"), eq(1L), contains("REQ-B48-ADJ-DOWNLOAD"));
    }

    @Test
    void invalidEvaluationYearIsRejectedWithFieldErrorBeforeQueryForReq1541() {
        assertThatThrownBy(() -> service.list(new ScoreAdjustmentHistorySearchCriteria(0, 20, "26", null, null,
                null, ScoreAdjustmentHistoryDataScope.ALL, null), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("점수 조정 이력 조회 조건");
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
