package kr.ac.knue.commonfoundation.basic48;

import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreAdjustmentHistoryService {
    private final ScoreAdjustmentHistoryMapper mapper;

    public ScoreAdjustmentHistoryService(ScoreAdjustmentHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public ScoreAdjustmentHistorySearchResponse list(ScoreAdjustmentHistorySearchCriteria criteria, Long viewerUserId) {
        ScoreAdjustmentHistorySearchCriteria normalized = criteria == null
                ? new ScoreAdjustmentHistorySearchCriteria(0, 20, null, null, null, null, ScoreAdjustmentHistoryDataScope.ALL, null)
                : criteria;
        validateSearch(normalized);
        ReadonlyGuard before = readonlyGuard();
        List<ScoreAdjustmentHistoryRow> rows = mapper.listScoreAdjustmentHistories(normalized);
        long total = mapper.countScoreAdjustmentHistories(normalized);
        String requestId = requestId("LIST");
        mapper.insertReadAudit("biz_score_adj_hist:list", auditJson("LIST", normalized.normalizedEvaluationYear(),
                normalized.normalizedAreaCode(), normalized.normalizedAdjustmentTarget(), total), viewerUserId, requestId);
        assertReadonlyUnchanged(before);
        return new ScoreAdjustmentHistorySearchResponse(rows, Math.max(normalized.page(), 0), normalized.safeSize(), total);
    }

    @Transactional
    public ScoreAdjustmentHistoryDetail getDetail(String adjustmentHistId,
                                                  ScoreAdjustmentHistoryDataScope dataScope,
                                                  String organizationCode,
                                                  Long viewerUserId) {
        String normalizedAdjustmentHistId = normalize(adjustmentHistId);
        if (normalizedAdjustmentHistId == null) {
            throw new BusinessValidationException("점수 조정 이력 식별자를 입력하세요.",
                    List.of(new ValidationError("adjustmentHistId", "점수 조정 이력 식별자를 입력하세요.")));
        }
        ReadonlyGuard before = readonlyGuard();
        ScoreAdjustmentHistoryDetail detail = mapper.findScoreAdjustmentHistoryDetail(normalizedAdjustmentHistId, dataScope, normalize(organizationCode));
        if (detail == null) {
            throw new NotFoundException("점수 조정 이력을 찾을 수 없습니다.");
        }
        mapper.insertReadAudit("biz_score_adj_hist:" + detail.adjustmentHistId(), auditJson("DETAIL", detail.evaluationYear(),
                detail.areaCode(), detail.adjustmentTarget(), 1), viewerUserId, requestId("DETAIL"));
        assertReadonlyUnchanged(before);
        return detail;
    }

    @Transactional
    public ScoreAdjustmentHistoryExcelDownload download(ScoreAdjustmentHistorySearchCriteria criteria, Long viewerUserId) {
        ScoreAdjustmentHistorySearchCriteria normalized = criteria == null
                ? new ScoreAdjustmentHistorySearchCriteria(0, 100, null, null, null, null, ScoreAdjustmentHistoryDataScope.ALL, null)
                : new ScoreAdjustmentHistorySearchCriteria(criteria.page(), 100, criteria.evaluationYear(), criteria.areaCode(),
                        criteria.targetUserId(), criteria.adjustmentTarget(), criteria.dataScope(), criteria.organizationCode());
        validateSearch(normalized);
        ReadonlyGuard before = readonlyGuard();
        long rowCount = mapper.countScoreAdjustmentHistories(normalized);
        String requestId = requestId("DOWNLOAD");
        mapper.insertReadAudit("biz_score_adj_hist:download", auditJson("DOWNLOAD", normalized.normalizedEvaluationYear(),
                normalized.normalizedAreaCode(), normalized.normalizedAdjustmentTarget(), rowCount), viewerUserId, requestId);
        assertReadonlyUnchanged(before);
        return new ScoreAdjustmentHistoryExcelDownload("score-adjustment-histories-"
                + (normalized.normalizedEvaluationYear() == null ? "all" : normalized.normalizedEvaluationYear()) + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", rowCount,
                "권한과 검색조건이 적용된 점수 조정 이력 조회 결과", requestId);
    }

    public void mutateScoreAdjustment(Object ignored) {
        throw new UnsupportedOperationException("점수 조정 이력은 조회 전용이며 점수나 평가백분율 조정 기능을 제공하지 않습니다.");
    }

    private void validateSearch(ScoreAdjustmentHistorySearchCriteria criteria) {
        if (criteria.normalizedEvaluationYear() != null && !criteria.normalizedEvaluationYear().matches("^[0-9]{4}$")) {
            throw new BusinessValidationException("점수 조정 이력 조회 조건이 올바르지 않습니다.",
                    List.of(new ValidationError("evaluationYear", "평가연도는 4자리 연도여야 합니다.")));
        }
    }

    private ReadonlyGuard readonlyGuard() {
        return new ReadonlyGuard(mapper.countScoreAdjustmentHistoriesForReadonlyGuard(), mapper.countScoreCalculationHistoriesForReadonlyGuard());
    }

    private void assertReadonlyUnchanged(ReadonlyGuard before) {
        ReadonlyGuard after = readonlyGuard();
        if (!before.equals(after)) {
            throw new IllegalStateException("점수 조정 이력 조회는 점수 이력 또는 현재 점수를 변경할 수 없습니다.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String requestId(String action) {
        return "REQ-B48-ADJ-" + action + "-" + UUID.randomUUID();
    }

    private String auditJson(String action, String evaluationYear, String areaCode, String adjustmentTarget, long rowCount) {
        return "{"
                + "\"screenId\":\"SCR-SCORE-ADJUSTMENT-HISTORY\","
                + "\"action\":\"" + escape(action) + "\","
                + "\"evaluationYear\":\"" + escape(evaluationYear == null ? "ALL" : evaluationYear) + "\","
                + "\"areaCode\":\"" + escape(areaCode == null ? "ALL" : areaCode) + "\","
                + "\"adjustmentTarget\":\"" + escape(adjustmentTarget == null ? "ALL" : adjustmentTarget) + "\","
                + "\"rowCount\":" + rowCount
                + "}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record ReadonlyGuard(long scoreAdjustmentHistoryCount, long scoreCalculationHistoryCount) {
    }
}
