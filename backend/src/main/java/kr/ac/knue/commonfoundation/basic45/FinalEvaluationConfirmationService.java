package kr.ac.knue.commonfoundation.basic45;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalEvaluationConfirmationService {
    private final FinalEvaluationConfirmationMapper mapper;
    private final Clock clock;

    @Autowired
    public FinalEvaluationConfirmationService(FinalEvaluationConfirmationMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    FinalEvaluationConfirmationService(FinalEvaluationConfirmationMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FinalEvaluationConfirmationListResponse list(FinalEvaluationConfirmationSearchCriteria criteria) {
        FinalEvaluationConfirmationSearchCriteria normalized = criteria == null
                ? new FinalEvaluationConfirmationSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        return new FinalEvaluationConfirmationListResponse(
                mapper.listConfirmations(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countConfirmations(normalized));
    }

    @Transactional
    public FinalEvaluationConfirmationResult confirm(Long targetId, String evaluationYear, Long requestUserId) {
        validateTarget(targetId, evaluationYear, requestUserId);
        int totalCount = mapper.countConfirmableMaterials(targetId, evaluationYear.trim());
        if (totalCount <= 0) {
            throw new ConflictException("재계산 완료된 인증 상태 평가자료가 없어 최종평가를 확정할 수 없습니다.");
        }
        String batchId = Basic45EvaluationBatchFoundationContract.batchId("CONFIRMATION", clock, mapper.nextConfirmationSequence());
        String requestId = "REQ-" + batchId;
        mapper.insertBatchRequest(batchId, "CONFIRMATION", toTargetConditionJson(targetId, evaluationYear, null), requestUserId, requestId);
        int changed = mapper.updateMaterialsStatus(targetId, evaluationYear.trim(), "인증", "평가확정", requestId, requestUserId);
        mapper.insertMaterialStatusHistories(targetId, evaluationYear.trim(), "인증", "평가확정", "최종평가 확정", requestId, requestUserId);
        mapper.upsertConfirmation(targetId, evaluationYear.trim(), "평가확정", batchId, requestUserId, requestId);
        mapper.insertBatchResult(batchId, "CONFIRMATION", totalCount, changed, 0, Math.max(0, totalCount - changed), requestId);
        return new FinalEvaluationConfirmationResult(batchId, requestId, targetId, "인증", "평가확정", changed);
    }

    @Transactional
    public FinalEvaluationConfirmationResult cancel(Long targetId, String evaluationYear, String cancelReason, Long requestUserId) {
        validateTarget(targetId, evaluationYear, requestUserId);
        List<ValidationError> fields = new ArrayList<>();
        if (cancelReason == null || cancelReason.isBlank()) {
            fields.add(new ValidationError("cancelReason", "확정취소 사유를 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("최종평가 확정취소 요청이 올바르지 않습니다.", fields);
        }
        int totalCount = mapper.countCancelableMaterials(targetId, evaluationYear.trim());
        if (totalCount <= 0) {
            throw new ConflictException("평가확정 상태 평가자료가 없어 확정을 취소할 수 없습니다.");
        }
        String batchId = Basic45EvaluationBatchFoundationContract.batchId("CONFIRMATION", clock, mapper.nextConfirmationSequence());
        String requestId = "REQ-" + batchId;
        mapper.insertBatchRequest(batchId, "CONFIRMATION", toTargetConditionJson(targetId, evaluationYear, cancelReason), requestUserId, requestId);
        int changed = mapper.updateMaterialsStatus(targetId, evaluationYear.trim(), "평가확정", "인증", requestId, requestUserId);
        mapper.insertMaterialStatusHistories(targetId, evaluationYear.trim(), "평가확정", "인증", cancelReason.trim(), requestId, requestUserId);
        mapper.markConfirmationCanceled(targetId, evaluationYear.trim(), batchId, cancelReason.trim(), requestUserId, requestId);
        mapper.insertBatchResult(batchId, "CONFIRMATION", totalCount, changed, 0, Math.max(0, totalCount - changed), requestId);
        return new FinalEvaluationConfirmationResult(batchId, requestId, targetId, "평가확정", "인증", changed);
    }

    private void validateTarget(Long targetId, String evaluationYear, Long requestUserId) {
        List<ValidationError> fields = new ArrayList<>();
        if (targetId == null || targetId <= 0) {
            fields.add(new ValidationError("targetId", "대상자를 선택하세요."));
        }
        if (requestUserId == null || requestUserId <= 0) {
            fields.add(new ValidationError("requestUserId", "요청자 세션을 확인할 수 없습니다."));
        }
        if (evaluationYear == null || evaluationYear.isBlank()) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        } else if (!evaluationYear.trim().matches("^[0-9]{4}$")) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("최종평가 확정 요청이 올바르지 않습니다.", fields);
        }
    }

    private String toTargetConditionJson(Long targetId, String evaluationYear, String cancelReason) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        addJson(joiner, "evaluationYear", evaluationYear);
        addJson(joiner, "targetUserId", String.valueOf(targetId));
        addJson(joiner, "actionType", cancelReason == null ? "CONFIRM" : "CANCEL");
        addJson(joiner, "cancelReason", cancelReason);
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
