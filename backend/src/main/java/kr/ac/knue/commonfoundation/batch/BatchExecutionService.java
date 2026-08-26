package kr.ac.knue.commonfoundation.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchExecutionService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private final BatchExecutionMapper mapper;
    private final ObjectMapper objectMapper;

    public BatchExecutionService(BatchExecutionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public BatchExecutionSearchResponse listBatchExecutions(int page, int size, BatchExecutionSearchCriteria criteria) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        BatchExecutionSearchCriteria normalized = new BatchExecutionSearchCriteria(
                blankToNull(criteria == null ? null : criteria.batchId()),
                blankToNull(criteria == null ? null : criteria.executionStatus()));
        List<BatchExecutionRow> executions = mapper.listBatchExecutions(normalized, safeSize, safePage * safeSize)
                .stream()
                .map(this::hydrate)
                .toList();
        return new BatchExecutionSearchResponse(executions, safePage, safeSize, mapper.countBatchExecutions(normalized));
    }

    @Transactional
    public BatchExecutionRow createBatchExecution(BatchExecutionRequest request, Long operatorUserId, String requestId) {
        validateExecutionRequest(request, true);
        String batchId = request.getBatchId().trim();
        if (mapper.existsBatchDefinition(batchId) == 0) {
            throw new ConflictException("실행 가능한 배치 정의를 찾을 수 없습니다.");
        }
        BatchExecutionRow row = newExecution(batchId, "MANUAL_RUN", "RUNNING", request.getReason().trim(),
                operatorUserId, null, effectiveRequestId(requestId), parameterJson(request.getParameters()));
        mapper.insertBatchExecution(row);
        return hydrate(mapper.findBatchExecution(row.executionId()));
    }

    @Transactional
    public BatchExecutionRow updateBatchExecutionStatus(String executionId, BatchExecutionStatusRequest request,
            Long operatorUserId, String requestId) {
        validateStatusRequest(executionId, request);
        BatchExecutionRow current = mapper.findBatchExecution(executionId.trim());
        if (current == null) {
            throw new ConflictException("중지할 배치 실행을 찾을 수 없습니다.");
        }
        if (!"RUNNING".equals(current.executionStatus())) {
            throw new ConflictException("RUNNING 상태의 배치만 중지할 수 있습니다.");
        }
        String effectiveRequestId = effectiveRequestId(requestId);
        mapper.stopBatchExecution(executionId.trim(), request.getReason().trim(), operatorUserId, effectiveRequestId);
        return hydrate(mapper.findBatchExecution(executionId.trim()));
    }

    @Transactional
    public BatchExecutionRow createBatchRerun(String originalExecutionId, BatchExecutionRequest request,
            Long operatorUserId, String requestId) {
        validateExecutionId(originalExecutionId);
        validateExecutionRequest(request, false);
        BatchExecutionRow original = mapper.findBatchExecution(originalExecutionId.trim());
        if (original == null) {
            throw new ConflictException("재실행할 원실행을 찾을 수 없습니다.");
        }
        String batchId = blankToNull(request.getBatchId()) == null ? original.batchId() : request.getBatchId().trim();
        if (!original.batchId().equals(batchId)) {
            throw new ConflictException("재실행 batchId는 원실행 batchId와 같아야 합니다.");
        }
        BatchExecutionRow row = newExecution(batchId, "RERUN", "RUNNING", request.getReason().trim(), operatorUserId,
                original.executionId(), effectiveRequestId(requestId), parameterJson(request.getParameters()));
        mapper.insertBatchExecution(row);
        return hydrate(mapper.findBatchExecution(row.executionId()));
    }

    private BatchExecutionRow newExecution(String batchId, String processType, String status, String reason,
            Long operatorUserId, String originalExecutionId, String requestId, String parameterJson) {
        return new BatchExecutionRow("BEX-" + UUID.randomUUID(), batchId, null, status, processType, reason, operatorUserId,
                null, originalExecutionId, requestId, LocalDateTime.now(), LocalDateTime.now(), null, parameterJson);
    }

    private void validateExecutionRequest(BatchExecutionRequest request, boolean requireBatchId) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("body", "배치 실행 요청 본문이 필요합니다."));
            throw new BusinessValidationException("배치 실행 요청이 올바르지 않습니다.", fields);
        }
        request.getUnexpectedFields().forEach(field -> fields.add(new ValidationError(field, "배치 실행 관리에서 허용하지 않는 필드입니다.")));
        if (requireBatchId && blankToNull(request.getBatchId()) == null) {
            fields.add(new ValidationError("batchId", "배치ID를 선택하세요."));
        }
        if (blankToNull(request.getReason()) == null) {
            fields.add(new ValidationError("reason", "처리 사유를 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("배치 실행 요청이 올바르지 않습니다.", fields);
        }
    }

    private void validateStatusRequest(String executionId, BatchExecutionStatusRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (blankToNull(executionId) == null) {
            fields.add(new ValidationError("executionId", "실행ID를 선택하세요."));
        }
        if (request == null) {
            fields.add(new ValidationError("body", "배치 중지 요청 본문이 필요합니다."));
            throw new BusinessValidationException("배치 중지 요청이 올바르지 않습니다.", fields);
        }
        request.getUnexpectedFields().forEach(field -> fields.add(new ValidationError(field, "배치 실행 관리에서 허용하지 않는 필드입니다.")));
        if (blankToNull(request.getReason()) == null) {
            fields.add(new ValidationError("reason", "처리 사유를 입력하세요."));
        }
        String status = blankToNull(request.getTargetStatus());
        if (status != null && !"STOPPED".equals(status)) {
            fields.add(new ValidationError("executionStatus", "중지 요청 상태는 STOPPED만 허용됩니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("배치 중지 요청이 올바르지 않습니다.", fields);
        }
    }

    private void validateExecutionId(String executionId) {
        if (blankToNull(executionId) == null) {
            throw new BusinessValidationException("배치 재실행 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("executionId", "원실행ID를 선택하세요.")));
        }
    }

    private BatchExecutionRow hydrate(BatchExecutionRow row) {
        return row == null ? null : row.withParameters(parse(row.executionParameterJson()));
    }

    private JsonNode parse(String parameterJson) {
        try {
            return objectMapper.readTree(blankToNull(parameterJson) == null ? "{}" : parameterJson);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String parameterJson(JsonNode parameters) {
        return parameters == null || parameters.isNull() ? "{}" : parameters.toString();
    }

    private String effectiveRequestId(String requestId) {
        return blankToNull(requestId) == null ? UUID.randomUUID().toString() : requestId.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
