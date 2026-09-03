package kr.ac.knue.commonfoundation.basic48;

import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationSnapshotService {
    private final EvaluationSnapshotMapper mapper;

    public EvaluationSnapshotService(EvaluationSnapshotMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationSnapshotSearchResponse list(EvaluationSnapshotSearchCriteria criteria) {
        EvaluationSnapshotSearchCriteria normalized = criteria == null
                ? new EvaluationSnapshotSearchCriteria(0, 20, null, null, EvaluationSnapshotDataScope.ALL, null)
                : criteria;
        validateSearch(normalized);
        readonlyGuard();
        List<EvaluationSnapshotRow> rows = mapper.listEvaluationSnapshots(normalized);
        long total = mapper.countEvaluationSnapshots(normalized);
        return new EvaluationSnapshotSearchResponse(rows, Math.max(normalized.page(), 0), normalized.safeSize(), total);
    }

    @Transactional
    public EvaluationSnapshotDetail getDetail(String snapshotId,
                                              EvaluationSnapshotDataScope dataScope,
                                              String organizationCode,
                                              Long viewerUserId) {
        String normalizedSnapshotId = normalize(snapshotId);
        if (normalizedSnapshotId == null) {
            throw new BusinessValidationException("시점 데이터 식별자를 입력하세요.",
                    List.of(new ValidationError("snapshotId", "시점 데이터 식별자를 입력하세요.")));
        }
        ReadonlyGuard before = readonlyGuard();
        EvaluationSnapshotDetail detail = mapper.findEvaluationSnapshotDetail(normalizedSnapshotId, dataScope, normalize(organizationCode));
        if (detail == null) {
            throw new NotFoundException("시점 데이터 snapshot을 찾을 수 없습니다.");
        }
        mapper.insertReadAudit("biz_eval_snapshots:" + detail.snapshotId(),
                auditJson("DETAIL", detail.evaluationYear(), detail.finalizationPoint(), 1), viewerUserId, requestId("DETAIL"));
        assertReadonlyUnchanged(before);
        return detail;
    }

    @Transactional
    public EvaluationSnapshotExcelDownload download(EvaluationSnapshotSearchCriteria criteria, Long viewerUserId) {
        EvaluationSnapshotSearchCriteria normalized = criteria == null
                ? new EvaluationSnapshotSearchCriteria(0, 100, null, null, EvaluationSnapshotDataScope.ALL, null)
                : new EvaluationSnapshotSearchCriteria(criteria.page(), 100, criteria.evaluationYear(), criteria.finalizationPoint(),
                        criteria.dataScope(), criteria.organizationCode());
        validateSearch(normalized);
        ReadonlyGuard before = readonlyGuard();
        long rowCount = mapper.countEvaluationSnapshots(normalized);
        String requestId = requestId("DOWNLOAD");
        mapper.insertReadAudit("biz_eval_snapshots:download", auditJson("DOWNLOAD", normalized.normalizedEvaluationYear(),
                normalized.normalizedFinalizationPoint(), rowCount), viewerUserId, requestId);
        assertReadonlyUnchanged(before);
        return new EvaluationSnapshotExcelDownload("evaluation-snapshots-" + (normalized.normalizedEvaluationYear() == null ? "all" : normalized.normalizedEvaluationYear()) + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", rowCount,
                "권한과 검색조건이 적용된 시점 데이터 조회 결과", requestId);
    }

    public void mutateSnapshot(Object ignored) {
        throw new UnsupportedOperationException("시점 데이터 관리는 조회 전용이며 변경 기능을 제공하지 않습니다.");
    }

    private void validateSearch(EvaluationSnapshotSearchCriteria criteria) {
        if (criteria.normalizedEvaluationYear() != null && !criteria.normalizedEvaluationYear().matches("^[0-9]{4}$")) {
            throw new BusinessValidationException("시점 데이터 조회 조건이 올바르지 않습니다.",
                    List.of(new ValidationError("evaluationYear", "평가연도는 4자리 연도여야 합니다.")));
        }
    }

    private ReadonlyGuard readonlyGuard() {
        return new ReadonlyGuard(mapper.countEvaluationMaterialsForReadonlyGuard(), mapper.countEvaluationRuleSetsForReadonlyGuard());
    }

    private void assertReadonlyUnchanged(ReadonlyGuard before) {
        ReadonlyGuard after = readonlyGuard();
        if (!before.equals(after)) {
            throw new IllegalStateException("시점 데이터 조회는 현재 기준정보와 평가자료를 변경할 수 없습니다.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String requestId(String action) {
        return "REQ-B48-SNAPSHOT-" + action + "-" + UUID.randomUUID();
    }

    private String auditJson(String action, String evaluationYear, String finalizationPoint, long rowCount) {
        return "{"
                + "\"screenId\":\"SCR-EVAL-SNAPSHOT-HISTORY\","
                + "\"action\":\"" + escape(action) + "\","
                + "\"evaluationYear\":\"" + escape(evaluationYear == null ? "ALL" : evaluationYear) + "\","
                + "\"finalizationPoint\":\"" + escape(finalizationPoint == null ? "ALL" : finalizationPoint) + "\","
                + "\"rowCount\":" + rowCount
                + "}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record ReadonlyGuard(long evaluationMaterialCount, long evaluationRuleSetCount) {
    }
}
