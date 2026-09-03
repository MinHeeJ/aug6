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
public class EvaluationMaterialDeletionService {
    private final EvaluationMaterialDeletionMapper mapper;
    private final Clock clock;

    @Autowired
    public EvaluationMaterialDeletionService(EvaluationMaterialDeletionMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    EvaluationMaterialDeletionService(EvaluationMaterialDeletionMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EvaluationMaterialDeletionPreviewResponse preview(EvaluationMaterialDeletionSearchCriteria criteria) {
        EvaluationMaterialDeletionSearchCriteria normalized = criteria == null
                ? new EvaluationMaterialDeletionSearchCriteria(0, 20, null, null, null)
                : criteria;
        List<EvaluationMaterialDeletionTarget> targets = mapper.listDeletionTargets(normalized);
        long count = mapper.countDeletionTargets(normalized);
        return new EvaluationMaterialDeletionPreviewResponse(
                targets,
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                count,
                count);
    }

    @Transactional
    public EvaluationMaterialDeletionResult delete(EvaluationBatchActionRequest request, Long requestUserId) {
        List<ValidationError> fields = validate(request, requestUserId);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가자료 삭제 요청이 올바르지 않습니다.", fields);
        }
        EvaluationMaterialDeletionSearchCriteria criteria = new EvaluationMaterialDeletionSearchCriteria(
                0,
                100,
                request.evaluationYear(),
                request.areaCode(),
                request.generationBatchId());
        long targetCountLong = mapper.countDeletionTargets(criteria);
        String batchId = Basic45EvaluationBatchFoundationContract.batchId("DELETION", clock, mapper.nextDeletionSequence());
        String requestId = "REQ-" + batchId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        mapper.insertBatchRequest(batchId, "DELETION", toTargetConditionJson(request), requestUserId, requestId);
        int deletedCount = mapper.logicalDeleteEvaluationMaterials(criteria, request.deleteReason().trim(), requestUserId, requestId);
        int targetCount = Math.toIntExact(targetCountLong);
        int excludedCount = Math.max(0, targetCount - deletedCount);
        mapper.insertBatchResult(batchId, "DELETION", targetCount, deletedCount, 0, excludedCount, requestId);
        return new EvaluationMaterialDeletionResult(batchId, requestId, targetCount, deletedCount, excludedCount);
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
        if (request == null || request.generationBatchId() == null || request.generationBatchId().isBlank()) {
            fields.add(new ValidationError("generationBatchId", "생성배치ID를 입력하세요."));
        }
        if (request == null || request.deleteReason() == null || request.deleteReason().isBlank()) {
            fields.add(new ValidationError("deleteReason", "삭제사유를 입력하세요."));
        }
        return fields;
    }

    private String toTargetConditionJson(EvaluationBatchActionRequest request) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        addJson(joiner, "evaluationYear", request.evaluationYear());
        addJson(joiner, "areaCode", request.areaCode());
        addJson(joiner, "generationBatchId", request.generationBatchId());
        addJson(joiner, "deleteReason", request.deleteReason());
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
