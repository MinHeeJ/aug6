package kr.ac.knue.commonfoundation.basic33;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationElementService {
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Pattern EVALUATION_YEAR = Pattern.compile("^[0-9]{4}$");
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "evaluation_elements";
    private final EvaluationElementMapper mapper;

    public EvaluationElementService(EvaluationElementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationElementSearchResponse list(EvaluationElementSearchCriteria criteria) {
        return new EvaluationElementSearchResponse(
                mapper.listEvaluationElements(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countEvaluationElements(criteria));
    }

    @Transactional
    public EvaluationElementRow save(SaveEvaluationElementRequest request, Long adminUserId, String requestId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가요소 저장 요청이 올바르지 않습니다.", fields);
        }
        String status = mapper.findRuleVersionStatus(request.ruleVersionId());
        if (status == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(status)) {
            throw new ConflictException("확정 또는 폐기된 규정버전의 평가요소는 수정할 수 없습니다.");
        }

        SaveEvaluationElementRequest normalizedRequest = new SaveEvaluationElementRequest(
                request.ruleVersionId(),
                normalized(request.areaCode()),
                normalized(request.itemCode()),
                request.evaluationYear().trim(),
                normalized(request.elementCode()),
                request.elementName().trim(),
                request.sortOrder(),
                request.activeYn().trim(),
                request.changeReason().trim());
        Long itemId = mapper.findItemId(
                normalizedRequest.ruleVersionId(),
                normalizedRequest.areaCode(),
                normalizedRequest.itemCode());
        if (itemId == null) {
            throw new NotFoundException("평가항목을 찾을 수 없습니다.");
        }

        EvaluationElementRow before = mapper.findByKey(
                itemId,
                normalizedRequest.evaluationYear(),
                normalizedRequest.elementCode());
        mapper.upsertEvaluationElement(normalizedRequest, itemId, adminUserId);
        EvaluationElementRow after = mapper.findByKey(
                itemId,
                normalizedRequest.evaluationYear(),
                normalizedRequest.elementCode());
        recordChangeHistory(before, after, normalizedRequest, adminUserId, requestId);
        return after;
    }

    private List<ValidationError> validate(SaveEvaluationElementRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.ruleVersionId() == null) {
            fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        }
        if (!hasText(request.areaCode())) {
            fields.add(new ValidationError("areaCode", "평가영역 코드를 입력하세요."));
        }
        if (!hasText(request.itemCode())) {
            fields.add(new ValidationError("itemCode", "평가항목 코드를 입력하세요."));
        }
        if (!hasText(request.evaluationYear())) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        } else if (!EVALUATION_YEAR.matcher(request.evaluationYear().trim()).matches()) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        }
        if (!hasText(request.elementCode())) {
            fields.add(new ValidationError("elementCode", "평가요소 코드를 입력하세요."));
        }
        if (!hasText(request.elementName())) {
            fields.add(new ValidationError("elementName", "평가요소명을 입력하세요."));
        }
        if (request.sortOrder() == null) {
            fields.add(new ValidationError("sortOrder", "정렬순서를 입력하세요."));
        }
        if (!USE_FLAGS.contains(trim(request.activeYn()))) {
            fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        }
        if (!hasText(request.changeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        return fields;
    }

    private void recordChangeHistory(
            EvaluationElementRow before,
            EvaluationElementRow after,
            SaveEvaluationElementRequest request,
            Long adminUserId,
            String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.ruleVersionId() + ":" + request.areaCode() + ":"
                + request.itemCode() + ":" + request.evaluationYear() + ":" + request.elementCode();
        recordIfChanged(before == null ? null : before.elementName(), after.elementName(), "element_name", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : String.valueOf(before.sortOrder()), String.valueOf(after.sortOrder()), "sort_order", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.activeYn(), after.activeYn(), "active_yn", changeType, targetKey, adminUserId, request.changeReason(), requestId);
    }

    private void recordIfChanged(String beforeValue, String afterValue, String fieldName, String changeType,
                                 String targetKey, Long adminUserId, String changeReason, String requestId) {
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory(TARGET_BUSINESS, targetKey, changeType, fieldName, beforeValue, afterValue,
                    adminUserId, changeReason, requestId);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalized(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
