package kr.ac.knue.commonfoundation.basic46;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationMaterialGenerationService {
    private final EvaluationMaterialGenerationMapper mapper;

    public EvaluationMaterialGenerationService(EvaluationMaterialGenerationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationMaterialGenerationSearchResponse list(EvaluationMaterialGenerationSearchCriteria criteria) {
        EvaluationMaterialGenerationSearchCriteria normalized = criteria == null
                ? new EvaluationMaterialGenerationSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        return new EvaluationMaterialGenerationSearchResponse(
                mapper.listGenerationTargets(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countGenerationTargets(normalized));
    }

    @Transactional
    public EvaluationMaterialGenerationResult create(EvaluationMaterialGenerationRequest request, Long requestedBy) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가자료 생성 요청이 올바르지 않습니다.", fields);
        }
        String requestId = "REQ-B46-GEN-" + UUID.randomUUID();
        String batchJobId = "B46-GEN-" + UUID.randomUUID();
        List<EvaluationMaterialGenerationTarget> candidates = mapper.listSourceCandidates(request);
        List<BatchItemPlan> itemPlans = new ArrayList<>();
        int plannedSuccess = 0;
        int plannedExcluded = 0;
        for (EvaluationMaterialGenerationTarget candidate : candidates) {
            String targetRef = "SOURCE-ACHIEVEMENT-" + candidate.sourceAchievementId();
            if (!isCertifiedOrAbove(candidate.sourceStatus())) {
                plannedExcluded++;
                itemPlans.add(new BatchItemPlan(targetRef, "EXCLUDED", null, null, "인증 이상 상태가 아닌 원천 실적 제외", null));
                continue;
            }
            if (mapper.existingMaterialCount(candidate.sourceAchievementId(), request.evaluationYear().trim(),
                    request.areaCode().trim(), candidate.targetUserId()) > 0) {
                plannedExcluded++;
                itemPlans.add(new BatchItemPlan(targetRef, "EXCLUDED", null, null, "동일 평가연도·영역·대상자·원천 실적 평가자료 중복 제외", null));
                continue;
            }
            plannedSuccess++;
            itemPlans.add(new BatchItemPlan(targetRef, "SUCCESS", null, null, null, candidate));
        }
        mapper.insertBatchJob(batchJobId, request, candidates.size(), plannedSuccess, 0, plannedExcluded, requestedBy, requestId);
        int success = 0;
        int excluded = 0;
        int failure = 0;
        for (BatchItemPlan plan : itemPlans) {
            if (plan.candidate() != null) {
                try {
                    int inserted = mapper.insertEvaluationMaterial(batchJobId, plan.candidate(), requestId, requestedBy);
                    if (inserted != 1) {
                        plan = new BatchItemPlan(plan.targetRef(), "EXCLUDED", null, null, "중복 평가자료 제외", null);
                    }
                } catch (RuntimeException exception) {
                    plan = new BatchItemPlan(plan.targetRef(), "FAILURE", "B46_GENERATION_ERROR", "평가자료 생성 중 오류가 발생했습니다.", null, null);
                }
            }
            if ("SUCCESS".equals(plan.resultStatus())) {
                success++;
            } else if ("EXCLUDED".equals(plan.resultStatus())) {
                excluded++;
            } else {
                failure++;
            }
            mapper.insertBatchJobItem(batchJobId, plan.targetRef(), plan.resultStatus(), plan.errorCode(),
                    plan.errorMessage(), plan.excludedReason(), requestId, requestedBy);
        }
        mapper.updateBatchJobCounts(batchJobId, success, failure, excluded, requestedBy);
        return new EvaluationMaterialGenerationResult(batchJobId, request.evaluationYear().trim(), request.areaCode().trim(),
                blankToNull(request.organizationCode()), request.targetUserId(), candidates.size(), success, failure, excluded, requestId);
    }

    private List<ValidationError> validate(EvaluationMaterialGenerationRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || request.evaluationYear() == null || request.evaluationYear().isBlank()) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        } else if (!request.evaluationYear().trim().matches("^[0-9]{4}$")) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 4자리 연도여야 합니다."));
        }
        if (request == null || request.areaCode() == null || request.areaCode().isBlank()) {
            fields.add(new ValidationError("areaCode", "평가영역을 입력하세요."));
        }
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            fields.add(new ValidationError("reason", "생성 사유를 입력하세요."));
        }
        return fields;
    }

    private boolean isCertifiedOrAbove(String status) {
        return "CERTIFIED".equals(status) || "EVALUATION_CONFIRMED".equals(status);
    }

    private record BatchItemPlan(
            String targetRef,
            String resultStatus,
            String errorCode,
            String errorMessage,
            String excludedReason,
            EvaluationMaterialGenerationTarget candidate) {
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
