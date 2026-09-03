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
public class FinalEvaluationConfirmationService {
    private final FinalEvaluationConfirmationMapper mapper;

    public FinalEvaluationConfirmationService(FinalEvaluationConfirmationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public FinalEvaluationConfirmationSearchResponse list(FinalEvaluationConfirmationSearchCriteria criteria) {
        FinalEvaluationConfirmationSearchCriteria normalized = criteria == null
                ? new FinalEvaluationConfirmationSearchCriteria(0, 20, null, null, null)
                : criteria;
        return new FinalEvaluationConfirmationSearchResponse(
                mapper.listFinalEvaluationConfirmations(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countFinalEvaluationConfirmations(normalized));
    }

    @Transactional
    public FinalEvaluationTransitionResult transition(Long targetUserId, FinalEvaluationTransitionRequest request, Long requestedBy) {
        List<ValidationError> fields = validate(targetUserId, request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("최종평가 전이 요청이 올바르지 않습니다.", fields);
        }

        String actionType = request.actionType().trim().toUpperCase();
        String evaluationYear = normalizeEvaluationYear(targetUserId, request.evaluationYear());
        FinalEvaluationConfirmationRow candidate = mapper.findConfirmationCandidate(targetUserId, evaluationYear);
        if (candidate == null || candidate.materialCount() == null || candidate.materialCount() == 0) {
            throw new ConflictException("최종평가 처리 대상 평가자료를 찾을 수 없습니다.");
        }

        if ("CONFIRM".equals(actionType)) {
            return confirm(candidate, requestedBy, normalizedReason(request.reason()));
        }
        return cancel(candidate, requestedBy, request.cancelReason().trim());
    }

    private FinalEvaluationTransitionResult confirm(FinalEvaluationConfirmationRow candidate, Long requestedBy, String reason) {
        if (!"CERTIFIED".equals(candidate.finalStatus())) {
            throw new ConflictException("인증 상태인 대상자만 최종평가 확정할 수 있습니다.");
        }
        if (!"SUCCESS".equals(candidate.latestRecalculationStatus())) {
            throw new ConflictException("최신 점수 재계산이 성공한 대상자만 최종평가 확정할 수 있습니다.");
        }
        String requestId = "REQ-B46-FINAL-" + UUID.randomUUID();
        String batchJobId = "B46-FINAL-" + UUID.randomUUID();
        String snapshotRef = "B46-SNAPSHOT-" + UUID.randomUUID();
        int totalCount = candidate.materialCount();
        int successCount = mapper.updateMaterialsStatus(candidate.targetUserId(), candidate.evaluationYear(),
                "CERTIFIED", "EVALUATION_CONFIRMED", requestId, requestedBy);
        if (successCount == 0) {
            throw new ConflictException("확정 가능한 인증 상태 평가자료가 없습니다.");
        }
        int excludedCount = Math.max(totalCount - successCount, 0);
        mapper.insertFinalizationBatchJob(batchJobId, "FINALIZATION", candidate.targetUserId(), candidate.evaluationYear(),
                totalCount, successCount, 0, excludedCount, requestedBy, requestId, reason);
        mapper.insertBatchJobItem(batchJobId, "TARGET-USER-" + candidate.targetUserId(), successCount > 0 ? "SUCCESS" : "EXCLUDED",
                null, null, successCount > 0 ? null : "확정 가능한 인증 상태 평가자료가 없습니다.", requestId, requestedBy);
        mapper.insertFinalization(candidate.targetUserId(), candidate.evaluationYear(), "EVALUATION_CONFIRMED", requestedBy,
                null, null, snapshotRef, snapshotJson(candidate, "EVALUATION_CONFIRMED", reason), batchJobId, requestId, requestedBy);
        return new FinalEvaluationTransitionResult(batchJobId, candidate.targetUserId(), candidate.evaluationYear(), "CONFIRM",
                "EVALUATION_CONFIRMED", totalCount, successCount, 0, excludedCount, snapshotRef, requestId);
    }

    private FinalEvaluationTransitionResult cancel(FinalEvaluationConfirmationRow candidate, Long requestedBy, String cancelReason) {
        if (!"EVALUATION_CONFIRMED".equals(candidate.finalStatus())) {
            throw new ConflictException("평가확정 상태인 대상자만 확정취소할 수 있습니다.");
        }
        String requestId = "REQ-B46-FINAL-CANCEL-" + UUID.randomUUID();
        String batchJobId = "B46-FINAL-CANCEL-" + UUID.randomUUID();
        String snapshotRef = candidate.snapshotRef() == null || candidate.snapshotRef().isBlank()
                ? "B46-SNAPSHOT-CANCEL-" + UUID.randomUUID()
                : candidate.snapshotRef();
        int totalCount = candidate.materialCount();
        int successCount = mapper.updateMaterialsStatus(candidate.targetUserId(), candidate.evaluationYear(),
                "EVALUATION_CONFIRMED", "CERTIFIED", requestId, requestedBy);
        if (successCount == 0) {
            throw new ConflictException("확정취소 가능한 평가확정 자료가 없습니다.");
        }
        int excludedCount = Math.max(totalCount - successCount, 0);
        mapper.insertFinalizationBatchJob(batchJobId, "FINALIZATION_CANCEL", candidate.targetUserId(), candidate.evaluationYear(),
                totalCount, successCount, 0, excludedCount, requestedBy, requestId, cancelReason);
        mapper.insertBatchJobItem(batchJobId, "TARGET-USER-" + candidate.targetUserId(), successCount > 0 ? "SUCCESS" : "EXCLUDED",
                null, null, successCount > 0 ? null : "확정취소 가능한 평가확정 자료가 없습니다.", requestId, requestedBy);
        mapper.insertFinalization(candidate.targetUserId(), candidate.evaluationYear(), "CANCELLED", null, requestedBy,
                cancelReason, snapshotRef, snapshotJson(candidate, "CERTIFIED", cancelReason), batchJobId, requestId, requestedBy);
        return new FinalEvaluationTransitionResult(batchJobId, candidate.targetUserId(), candidate.evaluationYear(), "CANCEL",
                "CERTIFIED", totalCount, successCount, 0, excludedCount, snapshotRef, requestId);
    }

    private List<ValidationError> validate(Long targetUserId, FinalEvaluationTransitionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (targetUserId == null || targetUserId <= 0) {
            fields.add(new ValidationError("targetId", "대상자 식별자가 올바르지 않습니다."));
        }
        if (request == null || request.actionType() == null || request.actionType().isBlank()) {
            fields.add(new ValidationError("actionType", "처리구분을 입력하세요."));
            return fields;
        }
        String actionType = request.actionType().trim().toUpperCase();
        if (!"CONFIRM".equals(actionType) && !"CANCEL".equals(actionType)) {
            fields.add(new ValidationError("actionType", "처리구분은 CONFIRM 또는 CANCEL이어야 합니다."));
        }
        if (request.evaluationYear() != null && !request.evaluationYear().isBlank()
                && !request.evaluationYear().trim().matches("^[0-9]{4}$")) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 4자리 연도여야 합니다."));
        }
        if ("CANCEL".equals(actionType) && (request.cancelReason() == null || request.cancelReason().isBlank())) {
            fields.add(new ValidationError("cancelReason", "확정취소 사유를 입력하세요."));
        }
        return fields;
    }

    private String normalizeEvaluationYear(Long targetUserId, String requestedYear) {
        if (requestedYear != null && !requestedYear.trim().isBlank()) {
            return requestedYear.trim();
        }
        String latest = mapper.findLatestEvaluationYearForTarget(targetUserId);
        if (latest == null || latest.isBlank()) {
            throw new ConflictException("대상자의 평가연도를 찾을 수 없습니다.");
        }
        return latest;
    }

    private String normalizedReason(String reason) {
        return reason == null || reason.trim().isBlank() ? "최종평가 확정" : reason.trim();
    }

    private String snapshotJson(FinalEvaluationConfirmationRow candidate, String statusAfterTransition, String reason) {
        String safeReason = escapeJson(reason == null ? "" : reason);
        return "{"
                + "\"targetUserId\":" + candidate.targetUserId() + ","
                + "\"evaluationYear\":\"" + escapeJson(candidate.evaluationYear()) + "\","
                + "\"finalScore\":" + candidate.finalScore() + ","
                + "\"latestRecalculationBatchId\":\"" + escapeJson(nullToBlank(candidate.latestRecalculationBatchId())) + "\","
                + "\"statusAfterTransition\":\"" + escapeJson(statusAfterTransition) + "\","
                + "\"reason\":\"" + safeReason + "\""
                + "}";
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
