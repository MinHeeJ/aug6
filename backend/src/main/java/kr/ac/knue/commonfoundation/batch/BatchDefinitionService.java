package kr.ac.knue.commonfoundation.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchDefinitionService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private final BatchDefinitionMapper mapper;
    private final ObjectMapper objectMapper;

    public BatchDefinitionService(BatchDefinitionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public BatchDefinitionSearchResponse listBatchDefinitions(int page, int size, BatchDefinitionSearchCriteria criteria) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        BatchDefinitionSearchCriteria normalized = new BatchDefinitionSearchCriteria(
                blankToNull(criteria == null ? null : criteria.batchId()),
                blankToNull(criteria == null ? null : criteria.batchType()),
                blankToNull(criteria == null ? null : criteria.scheduleCycle()));
        List<BatchDefinitionRow> definitions = mapper.listBatchDefinitions(normalized, safeSize, safePage * safeSize)
                .stream()
                .map(this::hydrate)
                .toList();
        return new BatchDefinitionSearchResponse(definitions, safePage, safeSize, mapper.countBatchDefinitions(normalized));
    }

    @Transactional
    public BatchDefinitionRow saveBatchDefinition(BatchDefinitionRequest request, Long userId, String requestId) {
        validateRequest(request);
        String normalizedBatchId = request.getBatchId().trim();
        request.setBatchId(normalizedBatchId);
        request.setBatchType(blankToNull(request.getBatchType()));
        request.setScheduleCycle(request.getScheduleCycle().trim());
        String effectiveRequestId = blankToNull(requestId) == null ? UUID.randomUUID().toString() : requestId.trim();
        mapper.upsertBatchDefinition(request, userId, effectiveRequestId);
        mapper.upsertBatchParameters(normalizedBatchId, parameterJson(request.getParameters()), userId, effectiveRequestId);
        mapper.deleteDependenciesForBatch(normalizedBatchId);
        for (String predecessor : normalizedUnique(request.getPredecessorBatchIds())) {
            mapper.insertDependency(predecessor, normalizedBatchId, userId, effectiveRequestId);
        }
        for (String successor : normalizedUnique(request.getSuccessorBatchIds())) {
            mapper.insertDependency(normalizedBatchId, successor, userId, effectiveRequestId);
        }
        BatchDefinitionRow saved = mapper.findBatchDefinition(normalizedBatchId);
        if (saved == null) {
            throw new NotFoundException("배치 정의를 찾을 수 없습니다.");
        }
        return hydrate(saved);
    }

    private void validateRequest(BatchDefinitionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("body", "배치 정의 요청 본문이 필요합니다."));
            throw new BusinessValidationException("배치 정의 저장 요청이 올바르지 않습니다.", fields);
        }
        request.getUnexpectedFields().forEach(field -> fields.add(new ValidationError(field, "배치 정의 관리에서 허용하지 않는 필드입니다.")));
        if (blankToNull(request.getBatchId()) == null) {
            fields.add(new ValidationError("batchId", "배치ID를 입력하세요."));
        }
        if (blankToNull(request.getBatchType()) == null) {
            fields.add(new ValidationError("batchType", "업무유형을 입력하세요."));
        }
        if (blankToNull(request.getScheduleCycle()) == null) {
            fields.add(new ValidationError("scheduleCycle", "실행주기를 입력하세요."));
        }
        if (request.getOwnerUserId() == null) {
            fields.add(new ValidationError("ownerUserId", "담당자를 입력하세요."));
        } else if (mapper.existsUser(request.getOwnerUserId()) == 0) {
            fields.add(new ValidationError("ownerUserId", "존재하는 담당자를 선택하세요."));
        }
        if (request.getMaxExecutionSeconds() != null && request.getMaxExecutionSeconds() < 0) {
            fields.add(new ValidationError("maxExecutionSeconds", "최대실행시간은 0 이상이어야 합니다."));
        }
        String batchId = blankToNull(request.getBatchId());
        for (String predecessor : normalizedUnique(request.getPredecessorBatchIds())) {
            validateDependencyBatch(fields, "predecessorBatchIds", batchId, predecessor);
        }
        for (String successor : normalizedUnique(request.getSuccessorBatchIds())) {
            validateDependencyBatch(fields, "successorBatchIds", batchId, successor);
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("배치 정의 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private void validateDependencyBatch(List<ValidationError> fields, String fieldName, String batchId, String relatedBatchId) {
        if (batchId != null && batchId.equals(relatedBatchId)) {
            fields.add(new ValidationError(fieldName, "자기 자신을 선후행 배치로 지정할 수 없습니다."));
            return;
        }
        if (mapper.existsBatchDefinition(relatedBatchId) == 0) {
            fields.add(new ValidationError(fieldName, "존재하는 배치ID만 선후행 관계로 지정하세요."));
        }
    }

    private BatchDefinitionRow hydrate(BatchDefinitionRow row) {
        return row.withChildren(mapper.listPredecessorBatchIds(row.batchId()), mapper.listSuccessorBatchIds(row.batchId()), parse(row.parameterJson()));
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

    private List<String> normalizedUnique(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                unique.add(normalized);
            }
        }
        return List.copyOf(unique);
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
