package kr.ac.knue.commonfoundation.basic46;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationMaterialDeletionService {
    private final EvaluationMaterialDeletionMapper mapper;

    public EvaluationMaterialDeletionService(EvaluationMaterialDeletionMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationMaterialDeletionPreviewResponse preview(EvaluationMaterialDeletionSearchCriteria criteria) {
        EvaluationMaterialDeletionSearchCriteria normalized = criteria == null
                ? new EvaluationMaterialDeletionSearchCriteria(0, 20, null, null, null)
                : criteria;
        List<ValidationError> fields = validatePreview(normalized);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가자료 삭제 미리보기 조건이 올바르지 않습니다.", fields);
        }
        return new EvaluationMaterialDeletionPreviewResponse(
                mapper.listDeletionPreviewTargets(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countDeletionPreviewTargets(normalized),
                mapper.countDeletableTargets(normalized),
                expectedPreviewToken(normalized.evaluationYear(), normalized.areaCode(), normalized.generationBatchId()));
    }

    @Transactional
    public EvaluationMaterialDeletionResult delete(EvaluationMaterialDeletionRequest request, Long requestedBy) {
        List<ValidationError> fields = validateDelete(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가자료 삭제 요청이 올바르지 않습니다.", fields);
        }
        String expectedToken = expectedPreviewToken(request.evaluationYear(), request.areaCode(), request.generationBatchId());
        if (!expectedToken.equals(request.previewToken().trim())) {
            throw new ConflictException("삭제대상 미리보기 후 발급된 previewToken으로만 삭제할 수 있습니다.");
        }

        EvaluationMaterialDeletionRequest normalizedRequest = new EvaluationMaterialDeletionRequest(
                request.evaluationYear().trim(), request.areaCode().trim(), request.generationBatchId().trim(),
                request.deletionReason().trim(), request.previewToken().trim());
        String requestId = "REQ-B46-DEL-" + UUID.randomUUID();
        String batchJobId = "B46-DEL-" + UUID.randomUUID();
        List<EvaluationMaterialDeletionTarget> candidates = mapper.listDeletionCandidates(normalizedRequest);
        int plannedSuccess = 0;
        int plannedExcluded = 0;
        for (EvaluationMaterialDeletionTarget candidate : candidates) {
            if (candidate.canDelete()) {
                plannedSuccess++;
            } else {
                plannedExcluded++;
            }
        }
        if (!candidates.isEmpty() && plannedSuccess == 0 && plannedExcluded > 0) {
            throw new ConflictException("평가확정 자료는 확정취소 후 삭제할 수 있습니다.");
        }
        mapper.insertDeletionBatchJob(batchJobId, normalizedRequest, candidates.size(), plannedSuccess, 0, plannedExcluded, requestedBy, requestId);

        int success = 0;
        int excluded = 0;
        int failure = 0;
        for (EvaluationMaterialDeletionTarget candidate : candidates) {
            String targetRef = "EVALUATION-MATERIAL-" + candidate.evaluationMaterialId();
            if (!candidate.canDelete()) {
                excluded++;
                mapper.insertBatchJobItem(batchJobId, targetRef, "EXCLUDED", null, null,
                        candidate.excludedReason(), requestId, requestedBy);
                continue;
            }
            try {
                int updated = mapper.markEvaluationMaterialDeleted(candidate.evaluationMaterialId(), normalizedRequest.deletionReason(),
                        requestId, requestedBy);
                if (updated == 1) {
                    success++;
                    mapper.insertBatchJobItem(batchJobId, targetRef, "SUCCESS", null, null, null, requestId, requestedBy);
                } else {
                    excluded++;
                    mapper.insertBatchJobItem(batchJobId, targetRef, "EXCLUDED", null, null,
                            "이미 삭제되었거나 평가확정 상태인 평가자료 제외", requestId, requestedBy);
                }
            } catch (RuntimeException exception) {
                failure++;
                mapper.insertBatchJobItem(batchJobId, targetRef, "FAILURE", "B46_DELETION_ERROR",
                        "평가자료 삭제 중 오류가 발생했습니다.", null, requestId, requestedBy);
            }
        }
        mapper.updateBatchJobCounts(batchJobId, success, failure, excluded, requestedBy);
        return new EvaluationMaterialDeletionResult(batchJobId, normalizedRequest.evaluationYear(), normalizedRequest.areaCode(),
                normalizedRequest.generationBatchId(), candidates.size(), success, failure, excluded, requestId);
    }

    private List<ValidationError> validatePreview(EvaluationMaterialDeletionSearchCriteria criteria) {
        List<ValidationError> fields = new ArrayList<>();
        validateRequiredCondition(criteria.evaluationYear(), criteria.areaCode(), criteria.generationBatchId(), fields);
        return fields;
    }

    private List<ValidationError> validateDelete(EvaluationMaterialDeletionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
            fields.add(new ValidationError("areaCode", "평가영역을 입력하세요."));
            fields.add(new ValidationError("generationBatchId", "생성배치ID를 입력하세요."));
            fields.add(new ValidationError("previewToken", "삭제대상 미리보기 토큰을 입력하세요."));
            fields.add(new ValidationError("deletionReason", "삭제사유를 입력하세요."));
            return fields;
        }
        validateRequiredCondition(request.evaluationYear(), request.areaCode(), request.generationBatchId(), fields);
        if (request.previewToken() == null || request.previewToken().isBlank()) {
            fields.add(new ValidationError("previewToken", "삭제대상 미리보기 토큰을 입력하세요."));
        }
        if (request.deletionReason() == null || request.deletionReason().isBlank()) {
            fields.add(new ValidationError("deletionReason", "삭제사유를 입력하세요."));
        }
        return fields;
    }

    private void validateRequiredCondition(String evaluationYear, String areaCode, String generationBatchId, List<ValidationError> fields) {
        if (evaluationYear == null || evaluationYear.isBlank()) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        } else if (!evaluationYear.trim().matches("^[0-9]{4}$")) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 4자리 연도여야 합니다."));
        }
        if (areaCode == null || areaCode.isBlank()) {
            fields.add(new ValidationError("areaCode", "평가영역을 입력하세요."));
        }
        if (generationBatchId == null || generationBatchId.isBlank()) {
            fields.add(new ValidationError("generationBatchId", "생성배치ID를 입력하세요."));
        }
    }

    private String expectedPreviewToken(String evaluationYear, String areaCode, String generationBatchId) {
        return "B46-PREVIEW-" + evaluationYear.trim() + "-" + areaCode.trim() + "-" + generationBatchId.trim();
    }
}
