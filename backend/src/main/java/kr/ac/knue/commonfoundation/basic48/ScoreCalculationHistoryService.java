package kr.ac.knue.commonfoundation.basic48;

import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreCalculationHistoryService {
    private final ScoreCalculationHistoryMapper mapper;

    public ScoreCalculationHistoryService(ScoreCalculationHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public ScoreCalculationHistorySearchResponse list(ScoreCalculationHistorySearchCriteria criteria, Long viewerUserId) {
        ScoreCalculationHistorySearchCriteria normalized = criteria == null
                ? new ScoreCalculationHistorySearchCriteria(0, 20, null, null, null, ScoreCalculationHistoryDataScope.ALL, null, null)
                : criteria;
        validateSearch(normalized);
        ReadonlyGuard before = readonlyGuard();
        List<ScoreCalculationHistoryRow> rows = mapper.listScoreCalculationHistories(normalized);
        long total = mapper.countScoreCalculationHistories(normalized);
        String requestId = requestId("LIST");
        mapper.insertReadAudit("biz_score_calc_hist:list", auditJson("LIST", normalized.normalizedEvaluationYear(),
                normalized.normalizedAreaCode(), total), viewerUserId, requestId);
        assertReadonlyUnchanged(before);
        return new ScoreCalculationHistorySearchResponse(rows, Math.max(normalized.page(), 0), normalized.safeSize(), total);
    }

    @Transactional
    public ScoreCalculationHistoryDetail getDetail(String calcHistId,
                                                   ScoreCalculationHistoryDataScope dataScope,
                                                   String organizationCode,
                                                   Long selfUserId,
                                                   Long viewerUserId) {
        String normalizedCalcHistId = normalize(calcHistId);
        if (normalizedCalcHistId == null) {
            throw new BusinessValidationException("점수 산출 이력 식별자를 입력하세요.",
                    List.of(new ValidationError("calcHistId", "점수 산출 이력 식별자를 입력하세요.")));
        }
        ReadonlyGuard before = readonlyGuard();
        ScoreCalculationHistoryDetail detail = mapper.findScoreCalculationHistoryDetail(normalizedCalcHistId, dataScope,
                normalize(organizationCode), selfUserId);
        if (detail == null) {
            throw new NotFoundException("점수 산출 이력을 찾을 수 없습니다.");
        }
        detail = withCompleteCalculationSteps(detail);
        mapper.insertReadAudit("biz_score_calc_hist:" + detail.calcHistId(), auditJson("DETAIL", detail.evaluationYear(),
                detail.areaCode(), 1), viewerUserId, requestId("DETAIL"));
        assertReadonlyUnchanged(before);
        return detail;
    }

    @Transactional
    public ScoreCalculationHistoryExcelDownload download(ScoreCalculationHistorySearchCriteria criteria, Long viewerUserId) {
        ScoreCalculationHistorySearchCriteria normalized = criteria == null
                ? new ScoreCalculationHistorySearchCriteria(0, 100, null, null, null, ScoreCalculationHistoryDataScope.ALL, null, null)
                : new ScoreCalculationHistorySearchCriteria(criteria.page(), 100, criteria.evaluationYear(), criteria.areaCode(),
                        criteria.targetUserId(), criteria.dataScope(), criteria.organizationCode(), criteria.selfUserId());
        validateSearch(normalized);
        ReadonlyGuard before = readonlyGuard();
        long rowCount = mapper.countScoreCalculationHistories(normalized);
        String requestId = requestId("DOWNLOAD");
        mapper.insertReadAudit("biz_score_calc_hist:download", auditJson("DOWNLOAD", normalized.normalizedEvaluationYear(),
                normalized.normalizedAreaCode(), rowCount), viewerUserId, requestId);
        assertReadonlyUnchanged(before);
        return new ScoreCalculationHistoryExcelDownload("score-calculation-histories-"
                + (normalized.normalizedEvaluationYear() == null ? "all" : normalized.normalizedEvaluationYear()) + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", rowCount,
                "권한과 검색조건이 적용된 점수 산출 이력 조회 결과", requestId);
    }

    public void mutateScoreCalculation(Object ignored) {
        throw new UnsupportedOperationException("점수 산출 이력은 조회 전용이며 점수·기준점수·배분율·계산식 변경 기능을 제공하지 않습니다.");
    }

    private void validateSearch(ScoreCalculationHistorySearchCriteria criteria) {
        if (criteria.normalizedEvaluationYear() != null && !criteria.normalizedEvaluationYear().matches("^[0-9]{4}$")) {
            throw new BusinessValidationException("점수 산출 이력 조회 조건이 올바르지 않습니다.",
                    List.of(new ValidationError("evaluationYear", "평가연도는 4자리 연도여야 합니다.")));
        }
    }

    private ReadonlyGuard readonlyGuard() {
        return new ReadonlyGuard(mapper.countScoreCalculationHistoriesForReadonlyGuard(), mapper.countEvaluationMaterialsForReadonlyGuard());
    }

    private void assertReadonlyUnchanged(ReadonlyGuard before) {
        ReadonlyGuard after = readonlyGuard();
        if (!before.equals(after)) {
            throw new IllegalStateException("점수 산출 이력 조회는 산출근거 또는 평가자료를 변경할 수 없습니다.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ScoreCalculationHistoryDetail withCompleteCalculationSteps(ScoreCalculationHistoryDetail detail) {
        String steps = detail.calculationStepsJson();
        if (steps != null && steps.contains("기준점수") && steps.contains("배분율") && steps.contains("산출점수")) {
            return detail;
        }
        String completeSteps = "["
                + "{\"step\":\"기준점수\",\"value\":\"" + escape(String.valueOf(detail.baseScore())) + "\"},"
                + "{\"step\":\"배분율\",\"value\":\"" + escape(String.valueOf(detail.distributionRate())) + "\"},"
                + "{\"step\":\"산출점수\",\"value\":\"" + escape(String.valueOf(detail.calculatedScore())) + "\"}"
                + "]";
        return new ScoreCalculationHistoryDetail(detail.calcHistId(), detail.targetUserId(), detail.targetUserName(),
                detail.evaluationYear(), detail.areaCode(), detail.areaName(), detail.sourceAchievementId(),
                detail.sourceAchievementTitle(), detail.managementItemCode(), detail.baseScore(), detail.participationType(),
                detail.distributionRate(), detail.capAppliedYn(), detail.formulaVersionId(), detail.generationNo(),
                detail.calculatedScore(), completeSteps, detail.sourceAchievementLink(), detail.requestId(),
                detail.calculatedAt(), detail.readOnlyNotice());
    }

    private String requestId(String action) {
        return "REQ-B48-CALC-" + action + "-" + UUID.randomUUID();
    }

    private String auditJson(String action, String evaluationYear, String areaCode, long rowCount) {
        return "{"
                + "\"screenId\":\"SCR-SCORE-CALC-HISTORY\","
                + "\"action\":\"" + escape(action) + "\","
                + "\"evaluationYear\":\"" + escape(evaluationYear == null ? "ALL" : evaluationYear) + "\","
                + "\"areaCode\":\"" + escape(areaCode == null ? "ALL" : areaCode) + "\","
                + "\"rowCount\":" + rowCount
                + "}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record ReadonlyGuard(long scoreCalculationHistoryCount, long evaluationMaterialCount) {
    }
}
