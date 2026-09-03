package kr.ac.knue.commonfoundation.basic48;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreRecalculationHistoryService {
    private final ScoreRecalculationHistoryMapper mapper;

    public ScoreRecalculationHistoryService(ScoreRecalculationHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public ScoreRecalculationHistorySearchResponse list(ScoreRecalculationHistorySearchCriteria criteria, Long viewerUserId) {
        ScoreRecalculationHistorySearchCriteria normalized = criteria == null
                ? new ScoreRecalculationHistorySearchCriteria(0, 20, null, null, null, null,
                        ScoreRecalculationHistoryDataScope.ALL, null)
                : criteria;
        validateSearch(normalized);
        ReadonlyGuard before = readonlyGuard();
        List<ScoreRecalculationHistoryRow> rows = mapper.listScoreRecalculationHistories(normalized);
        long total = mapper.countScoreRecalculationHistories(normalized);
        String requestId = requestId("LIST");
        mapper.insertReadAudit("biz_recalc_hist:list", auditJson("LIST", normalized.normalizedEvaluationYear(),
                normalized.normalizedExecutedFrom(), normalized.normalizedExecutedTo(), total), viewerUserId, requestId);
        assertReadonlyUnchanged(before);
        return new ScoreRecalculationHistorySearchResponse(rows, Math.max(normalized.page(), 0), normalized.safeSize(), total);
    }

    @Transactional
    public ScoreRecalculationHistoryDetail getDetail(String recalcHistId,
                                                     ScoreRecalculationHistoryDataScope dataScope,
                                                     String organizationCode,
                                                     Long viewerUserId) {
        String normalizedRecalcHistId = normalize(recalcHistId);
        if (normalizedRecalcHistId == null) {
            throw new BusinessValidationException("재계산 이력 식별자를 입력하세요.",
                    List.of(new ValidationError("recalcHistId", "재계산 이력 식별자를 입력하세요.")));
        }
        ReadonlyGuard before = readonlyGuard();
        ScoreRecalculationHistoryDetail detail = mapper.findScoreRecalculationHistoryDetail(normalizedRecalcHistId, dataScope, normalize(organizationCode));
        if (detail == null) {
            throw new NotFoundException("재계산 이력을 찾을 수 없습니다.");
        }
        mapper.insertReadAudit("biz_recalc_hist:" + detail.recalcHistId(), auditJson("DETAIL", detail.evaluationYear(),
                null, null, 1), viewerUserId, requestId("DETAIL"));
        assertReadonlyUnchanged(before);
        return detail;
    }

    @Transactional
    public ScoreRecalculationHistoryExcelDownload download(ScoreRecalculationHistorySearchCriteria criteria, Long viewerUserId) {
        ScoreRecalculationHistorySearchCriteria normalized = criteria == null
                ? new ScoreRecalculationHistorySearchCriteria(0, 100, null, null, null, null,
                        ScoreRecalculationHistoryDataScope.ALL, null)
                : new ScoreRecalculationHistorySearchCriteria(criteria.page(), 100, criteria.evaluationYear(), criteria.targetUserId(),
                        criteria.executedFrom(), criteria.executedTo(), criteria.dataScope(), criteria.organizationCode());
        validateSearch(normalized);
        ReadonlyGuard before = readonlyGuard();
        long rowCount = mapper.countScoreRecalculationHistories(normalized);
        String requestId = requestId("DOWNLOAD");
        mapper.insertReadAudit("biz_recalc_hist:download", auditJson("DOWNLOAD", normalized.normalizedEvaluationYear(),
                normalized.normalizedExecutedFrom(), normalized.normalizedExecutedTo(), rowCount), viewerUserId, requestId);
        assertReadonlyUnchanged(before);
        return new ScoreRecalculationHistoryExcelDownload("score-recalculation-histories-"
                + (normalized.normalizedEvaluationYear() == null ? "all" : normalized.normalizedEvaluationYear()) + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", rowCount,
                "권한과 검색조건이 적용된 재계산 이력 조회 결과", requestId);
    }

    public void rejectUnsupportedCommand(Object ignored) {
        throw new UnsupportedOperationException("재계산 이력은 조회 전용이며 재계산 실행 CTA 또는 점수 수정 기능을 제공하지 않습니다.");
    }

    private void validateSearch(ScoreRecalculationHistorySearchCriteria criteria) {
        List<ValidationError> errors = new ArrayList<>();
        if (criteria.normalizedEvaluationYear() != null && !criteria.normalizedEvaluationYear().matches("^[0-9]{4}$")) {
            errors.add(new ValidationError("evaluationYear", "평가연도는 4자리 연도여야 합니다."));
        }
        LocalDate from = parseDate(criteria.normalizedExecutedFrom(), "executedFrom", errors);
        LocalDate to = parseDate(criteria.normalizedExecutedTo(), "executedTo", errors);
        if (from != null && to != null && from.isAfter(to)) {
            errors.add(new ValidationError("executedFrom", "작업 시작일은 작업 종료일보다 늦을 수 없습니다."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("재계산 이력 조회 조건이 올바르지 않습니다.", errors);
        }
    }

    private LocalDate parseDate(String value, String field, List<ValidationError> errors) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            errors.add(new ValidationError(field, "작업기간은 yyyy-MM-dd 형식이어야 합니다."));
            return null;
        }
    }

    private ReadonlyGuard readonlyGuard() {
        return new ReadonlyGuard(mapper.countScoreRecalculationHistoriesForReadonlyGuard(), mapper.countScoreCalculationHistoriesForReadonlyGuard());
    }

    private void assertReadonlyUnchanged(ReadonlyGuard before) {
        ReadonlyGuard after = readonlyGuard();
        if (!before.equals(after)) {
            throw new IllegalStateException("재계산 이력 조회는 재계산 이력 또는 점수 산출 이력을 변경할 수 없습니다.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String requestId(String action) {
        return "REQ-B48-RECALC-" + action + "-" + UUID.randomUUID();
    }

    private String auditJson(String action, String evaluationYear, String executedFrom, String executedTo, long rowCount) {
        return "{"
                + "\"screenId\":\"SCR-SCORE-RECALCULATION-HISTORY\","
                + "\"action\":\"" + escape(action) + "\","
                + "\"evaluationYear\":\"" + escape(evaluationYear == null ? "ALL" : evaluationYear) + "\","
                + "\"executedFrom\":\"" + escape(executedFrom == null ? "ALL" : executedFrom) + "\","
                + "\"executedTo\":\"" + escape(executedTo == null ? "ALL" : executedTo) + "\","
                + "\"rowCount\":" + rowCount
                + "}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record ReadonlyGuard(long scoreRecalculationHistoryCount, long scoreCalculationHistoryCount) {
    }
}
