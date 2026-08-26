package kr.ac.knue.commonfoundation.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchRetryService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private final BatchRetryMapper mapper;

    public BatchRetryService(BatchRetryMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public BatchRetryTargetSearchResponse listBatchRetryTargets(int page, int size, BatchRetryTargetSearchCriteria criteria) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        BatchRetryTargetSearchCriteria normalized = new BatchRetryTargetSearchCriteria(
                blankToNull(criteria == null ? null : criteria.originalExecutionId()),
                blankToNull(criteria == null ? null : criteria.failedItemKey()));
        return new BatchRetryTargetSearchResponse(
                mapper.listBatchRetryTargets(normalized, safeSize, safePage * safeSize),
                safePage,
                safeSize,
                mapper.countBatchRetryTargets(normalized));
    }

    @Transactional
    public BatchRetryResultRow createBatchRetry(BatchRetryRequest request, Long userId, String requestId) {
        validateRequest(request);
        String originalExecutionId = request.getOriginalExecutionId().trim();
        String failedItemKey = blankToNull(request.getFailedItemKey());
        String retryReason = request.getRetryReason().trim();
        if (mapper.countFailedRetryTarget(originalExecutionId, failedItemKey) == 0) {
            throw new ConflictException("재처리는 실패 대상만 선택할 수 있습니다.");
        }
        String batchId = mapper.findOriginalBatchId(originalExecutionId);
        if (batchId == null) {
            throw new NotFoundException("원실행 배치를 찾을 수 없습니다.");
        }
        String retryExecutionId = "RETRY-" + UUID.randomUUID();
        String effectiveRequestId = blankToNull(requestId) == null ? UUID.randomUUID().toString() : requestId.trim();
        mapper.insertRetryExecution(retryExecutionId, batchId, "RERUN", retryReason, userId, originalExecutionId, effectiveRequestId);
        mapper.insertRetryResult(retryExecutionId, originalExecutionId, failedItemKey, retryReason, effectiveRequestId);
        BatchRetryResultRow result = mapper.findRetryResult(retryExecutionId);
        if (result == null) {
            throw new NotFoundException("재처리 결과를 찾을 수 없습니다.");
        }
        return result;
    }

    private void validateRequest(BatchRetryRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("body", "배치 재처리 요청 본문이 필요합니다."));
            throw new BusinessValidationException("배치 재처리 요청이 올바르지 않습니다.", fields);
        }
        request.getUnexpectedFields().forEach(field -> fields.add(new ValidationError(field, "배치 오류 재처리에서 허용하지 않는 필드입니다.")));
        if (blankToNull(request.getOriginalExecutionId()) == null) {
            fields.add(new ValidationError("originalExecutionId", "원실행ID를 선택하세요."));
        }
        if (blankToNull(request.getRetryReason()) == null) {
            fields.add(new ValidationError("retryReason", "재처리 사유를 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("배치 재처리 요청이 올바르지 않습니다.", fields);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
