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
public class EvaluationManagementItemService {
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Set<String> DATA_TYPES = Set.of("TEXT", "NUMBER", "DATE", "BOOLEAN", "CODE", "FILE");
    private static final Pattern EVALUATION_YEAR = Pattern.compile("^[0-9]{4}$");
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "evaluation_management_items";
    private final EvaluationManagementItemMapper mapper;

    public EvaluationManagementItemService(EvaluationManagementItemMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationManagementItemSearchResponse list(EvaluationManagementItemSearchCriteria criteria) {
        return new EvaluationManagementItemSearchResponse(
                mapper.listEvaluationManagementItems(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countEvaluationManagementItems(criteria));
    }

    @Transactional
    public EvaluationManagementItemRow save(SaveEvaluationManagementItemRequest request, Long adminUserId, String requestId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("관리항목 저장 요청이 올바르지 않습니다.", fields);
        }
        String status = mapper.findRuleVersionStatus(request.ruleVersionId());
        if (status == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(status)) {
            throw new ConflictException("확정 또는 폐기된 규정버전의 관리항목은 수정할 수 없습니다.");
        }

        SaveEvaluationManagementItemRequest normalizedRequest = new SaveEvaluationManagementItemRequest(
                request.ruleVersionId(),
                normalized(request.areaCode()),
                normalized(request.itemCode()),
                request.evaluationYear().trim(),
                normalized(request.elementCode()),
                normalized(request.managementItemCode()),
                request.managementItemName().trim(),
                request.sortOrder(),
                request.activeYn().trim(),
                request.teacherEditableYn().trim(),
                request.requiredYn().trim(),
                normalized(request.dataType()),
                request.changeReason().trim());
        Long elementId = mapper.findElementId(
                normalizedRequest.ruleVersionId(),
                normalizedRequest.areaCode(),
                normalizedRequest.itemCode(),
                normalizedRequest.evaluationYear(),
                normalizedRequest.elementCode());
        if (elementId == null) {
            throw new NotFoundException("평가요소를 찾을 수 없습니다.");
        }

        EvaluationManagementItemRow before = mapper.findByKey(elementId, normalizedRequest.managementItemCode());
        mapper.upsertEvaluationManagementItem(normalizedRequest, elementId, adminUserId);
        EvaluationManagementItemRow after = mapper.findByKey(elementId, normalizedRequest.managementItemCode());
        recordChangeHistory(before, after, normalizedRequest, adminUserId, requestId);
        return after;
    }

    private List<ValidationError> validate(SaveEvaluationManagementItemRequest request) {
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
        if (!hasText(request.managementItemCode())) {
            fields.add(new ValidationError("managementItemCode", "관리항목 코드를 입력하세요."));
        }
        if (!hasText(request.managementItemName())) {
            fields.add(new ValidationError("managementItemName", "관리항목명을 입력하세요."));
        }
        if (request.sortOrder() == null) {
            fields.add(new ValidationError("sortOrder", "정렬순서를 입력하세요."));
        }
        if (!USE_FLAGS.contains(trim(request.activeYn()))) {
            fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        }
        if (!USE_FLAGS.contains(trim(request.teacherEditableYn()))) {
            fields.add(new ValidationError("teacherEditableYn", "Y 또는 N을 선택하세요."));
        }
        if (!USE_FLAGS.contains(trim(request.requiredYn()))) {
            fields.add(new ValidationError("requiredYn", "Y 또는 N을 선택하세요."));
        }
        if (!DATA_TYPES.contains(normalized(request.dataType()))) {
            fields.add(new ValidationError("dataType", "TEXT, NUMBER, DATE, BOOLEAN, CODE, FILE 중 하나를 선택하세요."));
        }
        if (!hasText(request.changeReason())) {
            fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        }
        return fields;
    }

    private void recordChangeHistory(
            EvaluationManagementItemRow before,
            EvaluationManagementItemRow after,
            SaveEvaluationManagementItemRequest request,
            Long adminUserId,
            String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.ruleVersionId() + ":" + request.areaCode() + ":"
                + request.itemCode() + ":" + request.evaluationYear() + ":"
                + request.elementCode() + ":" + request.managementItemCode();
        recordIfChanged(before == null ? null : before.managementItemName(), after.managementItemName(), "management_item_name", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : String.valueOf(before.sortOrder()), String.valueOf(after.sortOrder()), "sort_order", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.activeYn(), after.activeYn(), "active_yn", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.teacherEditableYn(), after.teacherEditableYn(), "teacher_editable_yn", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.requiredYn(), after.requiredYn(), "required_yn", changeType, targetKey, adminUserId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.dataType(), after.dataType(), "data_type", changeType, targetKey, adminUserId, request.changeReason(), requestId);
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
