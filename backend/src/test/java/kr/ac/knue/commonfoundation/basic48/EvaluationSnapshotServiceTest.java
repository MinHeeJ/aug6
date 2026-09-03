package kr.ac.knue.commonfoundation.basic48;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationSnapshotServiceTest {
    @Mock EvaluationSnapshotMapper mapper;
    @InjectMocks EvaluationSnapshotService service;

    @Test
    void listEvaluationSnapshotsReadsOnlySnapshotRowsWithoutChangingCurrentMaterialOrRuleCountsForReq1533Req1536() {
        when(mapper.countEvaluationMaterialsForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countEvaluationRuleSetsForReadonlyGuard()).thenReturn(4L, 4L);
        EvaluationSnapshotSearchCriteria criteria = new EvaluationSnapshotSearchCriteria(0, 20, "2026", null,
                EvaluationSnapshotDataScope.ALL, null);
        when(mapper.listEvaluationSnapshots(criteria)).thenReturn(List.of(row()));
        when(mapper.countEvaluationSnapshots(criteria)).thenReturn(1L);

        EvaluationSnapshotSearchResponse response = service.list(criteria);

        assertThat(response.results()).extracting(EvaluationSnapshotRow::snapshotId).containsExactly("B48-SNAPSHOT-001");
        assertThat(response.totalElements()).isEqualTo(1L);
        verify(mapper).countEvaluationMaterialsForReadonlyGuard();
        verify(mapper).countEvaluationRuleSetsForReadonlyGuard();
    }

    @Test
    void getEvaluationSnapshotDetailRecordsAuditReadHistoryAndPreservesReadonlyGuardForReq1534Req1535Req1536() {
        when(mapper.countEvaluationMaterialsForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countEvaluationRuleSetsForReadonlyGuard()).thenReturn(4L, 4L);
        when(mapper.findEvaluationSnapshotDetail("B48-SNAPSHOT-001", EvaluationSnapshotDataScope.ALL, null)).thenReturn(detail());

        EvaluationSnapshotDetail detail = service.getDetail("B48-SNAPSHOT-001", EvaluationSnapshotDataScope.ALL, null, 1L);

        assertThat(detail.ruleSnapshotJson()).contains("B33-CONFIRMED-2026");
        assertThat(detail.materialSnapshotJson()).contains("materialCount");
        assertThat(detail.preservedResultJson()).contains("finalScore");
        verify(mapper).insertReadAudit(eq("biz_eval_snapshots:B48-SNAPSHOT-001"), contains("SCR-EVAL-SNAPSHOT-HISTORY"), eq(1L), contains("REQ-B48-SNAPSHOT-DETAIL"));
    }

    @Test
    void downloadEvaluationSnapshotsExcelCountsFilteredRowsAndRecordsAuditHistoryForReq1520Req1536() {
        when(mapper.countEvaluationMaterialsForReadonlyGuard()).thenReturn(3L, 3L);
        when(mapper.countEvaluationRuleSetsForReadonlyGuard()).thenReturn(4L, 4L);
        EvaluationSnapshotSearchCriteria criteria = new EvaluationSnapshotSearchCriteria(0, 20, "2026", "2026-FINAL-01",
                EvaluationSnapshotDataScope.ALL, null);
        when(mapper.countEvaluationSnapshots(new EvaluationSnapshotSearchCriteria(0, 100, "2026", "2026-FINAL-01",
                EvaluationSnapshotDataScope.ALL, null))).thenReturn(1L);

        EvaluationSnapshotExcelDownload download = service.download(criteria, 1L);

        assertThat(download.fileName()).isEqualTo("evaluation-snapshots-2026.xlsx");
        assertThat(download.rowCount()).isEqualTo(1L);
        verify(mapper).insertReadAudit(eq("biz_eval_snapshots:download"), contains("DOWNLOAD"), eq(1L), contains("REQ-B48-SNAPSHOT-DOWNLOAD"));
    }

    @Test
    void invalidEvaluationYearIsRejectedWithApiFieldBeforeQueryForReq1533() {
        assertThatThrownBy(() -> service.list(new EvaluationSnapshotSearchCriteria(0, 20, "26", null,
                EvaluationSnapshotDataScope.ALL, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("시점 데이터 조회 조건");
    }

    private EvaluationSnapshotRow row() {
        return new EvaluationSnapshotRow("B48-SNAPSHOT-001", "2026", "2026-FINAL-01", "KNUE-DEPT-COMP", 2L,
                "B48-RULE-SNAPSHOT-001", "B48-MATERIAL-SNAPSHOT-001", "B48-RESULT-SNAPSHOT-001", "PRESERVED",
                LocalDateTime.parse("2026-09-03T09:00:00"), "REQ-B48-SEED-SNAPSHOT-001");
    }

    private EvaluationSnapshotDetail detail() {
        return new EvaluationSnapshotDetail("B48-SNAPSHOT-001", "2026", "2026-FINAL-01", "KNUE-DEPT-COMP", 2L,
                "B48-RULE-SNAPSHOT-001", "B48-MATERIAL-SNAPSHOT-001", "B48-RESULT-SNAPSHOT-001", "PRESERVED",
                "{\"ruleSet\":\"B33-CONFIRMED-2026\"}", "{\"materialCount\":3}", "{\"finalScore\":27.50}",
                LocalDateTime.parse("2026-09-03T09:00:00"), "REQ-B48-SEED-SNAPSHOT-001",
                "현재 기준정보·평가자료 변경 및 신규 확정 실행은 제공하지 않는 조회 전용 snapshot입니다.");
    }
}
