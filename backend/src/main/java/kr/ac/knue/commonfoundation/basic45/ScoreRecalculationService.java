package kr.ac.knue.commonfoundation.basic45;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreRecalculationService {
    private final ScoreRecalculationMapper mapper;
    private final Clock clock;

    @Autowired
    public ScoreRecalculationService(ScoreRecalculationMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    ScoreRecalculationService(ScoreRecalculationMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ScoreRecalculationPreviewResponse preview(ScoreRecalculationSearchCriteria criteria) {
        ScoreRecalculationSearchCriteria normalized = criteria == null
                ? new ScoreRecalculationSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        List<ScoreRecalculationTarget> targets = mapper.listRecalculationTargets(normalized);
        long count = mapper.countRecalculationTargets(normalized);
        return new ScoreRecalculationPreviewResponse(targets, Math.max(normalized.page(), 0), normalized.safeSize(), count, count);
    }

    @Transactional
    public ScoreRecalculationResult recalculate(EvaluationBatchActionRequest request, Long requestUserId) {
        List<ValidationError> fields = validate(request, requestUserId);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("점수 재계산 요청이 올바르지 않습니다.", fields);
        }
        ScoreRecalculationSearchCriteria criteria = new ScoreRecalculationSearchCriteria(
                0, 100, request.evaluationYear(), request.areaCode(), request.targetUserId(), request.formulaVersionId());
        List<ScoreRecalculationTarget> targets = mapper.listRecalculationTargets(criteria);
        String batchId = Basic45EvaluationBatchFoundationContract.batchId("RECALCULATION", clock, mapper.nextRecalculationSequence());
        String requestId = "REQ-" + batchId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        mapper.insertBatchRequest(batchId, "RECALCULATION", toTargetConditionJson(request), requestUserId, requestId);
        int recalculatedCount = 0;
        for (ScoreRecalculationTarget target : targets) {
            mapper.insertScoreCalculationGeneration(target, batchId, requestId, requestUserId);
            recalculatedCount += mapper.updateEvaluationMaterialScore(target, requestId, requestUserId);
        }
        int targetCount = targets.size();
        int excludedCount = Math.max(0, targetCount - recalculatedCount);
        mapper.insertBatchResult(batchId, "RECALCULATION", targetCount, recalculatedCount, 0, excludedCount, requestId);
        return new ScoreRecalculationResult(batchId, requestId, targetCount, recalculatedCount, excludedCount);
    }

    private List<ValidationError> validate(EvaluationBatchActionRequest request, Long requestUserId) {
        List<ValidationError> fields = new ArrayList<>();
        if (requestUserId == null || requestUserId <= 0) {
            fields.add(new ValidationError("requestUserId", "요청자 세션을 확인할 수 없습니다."));
        }
        if (request == null || request.evaluationYear() == null || request.evaluationYear().isBlank()) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        } else if (!request.evaluationYear().trim().matches("^[0-9]{4}$")) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        }
        if (request == null || request.formulaVersionId() == null || request.formulaVersionId().isBlank()) {
            fields.add(new ValidationError("formulaVersionId", "산식버전을 선택하세요."));
        } else if (!request.formulaVersionId().trim().matches("^[0-9]+$")) {
            fields.add(new ValidationError("formulaVersionId", "산식버전은 숫자 ID여야 합니다."));
        }
        return fields;
    }

    private String toTargetConditionJson(EvaluationBatchActionRequest request) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        addJson(joiner, "evaluationYear", request.evaluationYear());
        addJson(joiner, "areaCode", request.areaCode());
        addJson(joiner, "targetUserId", request.targetUserId());
        addJson(joiner, "formulaVersionId", request.formulaVersionId());
        return joiner.toString();
    }

    private void addJson(StringJoiner joiner, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        joiner.add("\"" + key + "\":\"" + escapeJson(value.trim()) + "\"");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
