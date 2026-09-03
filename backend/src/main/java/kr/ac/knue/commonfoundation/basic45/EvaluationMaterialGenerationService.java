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
public class EvaluationMaterialGenerationService {
    private final EvaluationMaterialGenerationMapper mapper;
    private final Clock clock;

    @Autowired
    public EvaluationMaterialGenerationService(EvaluationMaterialGenerationMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    EvaluationMaterialGenerationService(EvaluationMaterialGenerationMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EvaluationMaterialGenerationPreviewResponse preview(EvaluationMaterialGenerationSearchCriteria criteria) {
        EvaluationMaterialGenerationSearchCriteria normalized = criteria == null
                ? new EvaluationMaterialGenerationSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        List<EvaluationMaterialGenerationTarget> targets = mapper.listGenerationTargets(normalized);
        long count = mapper.countGenerationTargets(normalized);
        return new EvaluationMaterialGenerationPreviewResponse(
                targets,
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                count,
                count);
    }

    @Transactional
    public EvaluationMaterialGenerationResult create(EvaluationBatchActionRequest request, Long requestUserId) {
        List<ValidationError> fields = validate(request, requestUserId);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가자료 생성 요청이 올바르지 않습니다.", fields);
        }
        EvaluationMaterialGenerationSearchCriteria criteria = new EvaluationMaterialGenerationSearchCriteria(
                0,
                100,
                request.evaluationYear(),
                request.areaCode(),
                request.organizationCode(),
                request.targetUserId());
        List<EvaluationMaterialGenerationTarget> targets = mapper.listGenerationTargets(criteria);
        String batchId = Basic45EvaluationBatchFoundationContract.batchId("GENERATION", clock, mapper.nextGenerationSequence());
        String requestId = "REQ-" + batchId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        mapper.insertBatchRequest(batchId, "GENERATION", toTargetConditionJson(request), requestUserId, requestId);
        int createdCount = 0;
        for (EvaluationMaterialGenerationTarget target : targets) {
            createdCount += mapper.insertEvaluationMaterial(target, batchId, requestId, requestUserId);
        }
        int targetCount = targets.size();
        int excludedCount = Math.max(0, targetCount - createdCount);
        mapper.insertBatchResult(batchId, "GENERATION", targetCount, createdCount, 0, excludedCount, requestId);
        return new EvaluationMaterialGenerationResult(batchId, requestId, targetCount, createdCount, excludedCount);
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
        return fields;
    }

    private String toTargetConditionJson(EvaluationBatchActionRequest request) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        addJson(joiner, "evaluationYear", request.evaluationYear());
        addJson(joiner, "areaCode", request.areaCode());
        addJson(joiner, "organizationCode", request.organizationCode());
        addJson(joiner, "targetUserId", request.targetUserId());
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
