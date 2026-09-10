package kr.ac.knue.commonfoundation.basic59;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationElementManagementItemService {
    private static final Pattern EVALUATION_YEAR = Pattern.compile("^[0-9]{4}$");
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "evaluation_element_management_item_settings";
    private final EvaluationElementManagementItemMapper mapper;

    public EvaluationElementManagementItemService(EvaluationElementManagementItemMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationElementManagementItemSearchResponse list(EvaluationElementManagementItemSearchCriteria criteria) {
        return new EvaluationElementManagementItemSearchResponse(
                mapper.listEvaluationElementManagementItems(criteria),
                criteria.safePage(),
                criteria.safeSize(),
                mapper.countEvaluationElementManagementItems(criteria));
    }

    @Transactional
    public EvaluationElementManagementItemRow save(SaveEvaluationElementManagementItemRequest request, CurrentUser user, String requestId) {
        SaveEvaluationElementManagementItemRequest normalized = normalizeAndValidate(request);
        String status = mapper.findRuleVersionStatus(normalized.ruleVersionId());
        if (status == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(status)) {
            throw new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다.");
        }

        EvaluationElementManagementItemRow before = mapper.findByBusinessKey(
                normalized.ruleVersionId(), normalized.evaluationYear(), normalized.areaCode(), normalized.elementCode(), normalized.managementItemCode());
        EvaluationElementManagementItemRow after = mapper.upsertEvaluationElementManagementItem(normalized, user.userId(), requestId);
        recordChangeHistory(before, after, normalized, user.userId(), requestId);
        return after;
    }

    private SaveEvaluationElementManagementItemRequest normalizeAndValidate(SaveEvaluationElementManagementItemRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String evaluationYear = trim(request.evaluationYear());
        String areaCode = normalized(request.areaCode());
        String elementCode = normalized(request.elementCode());
        String managementItemCode = normalized(request.managementItemCode());
        String managementItemName = trim(request.managementItemName());
        String teacherEditablePart = trim(request.teacherEditablePart());
        String activeYn = normalized(request.activeYn());
        String changeReason = trim(request.changeReason());

        if (request.ruleVersionId() == null) fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        if (!hasText(evaluationYear)) fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        else if (!EVALUATION_YEAR.matcher(evaluationYear).matches()) fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        if (!hasText(areaCode)) fields.add(new ValidationError("areaCode", "평가영역 코드를 입력하세요."));
        if (!hasText(elementCode)) fields.add(new ValidationError("elementCode", "평가요소 코드를 입력하세요."));
        if (!hasText(managementItemCode)) fields.add(new ValidationError("managementItemCode", "관리항목 코드를 입력하세요."));
        if (!hasText(managementItemName)) fields.add(new ValidationError("managementItemName", "관리항목명을 입력하세요."));
        if (!hasText(teacherEditablePart)) fields.add(new ValidationError("teacherEditablePart", "교수입력 가능부분을 입력하세요."));
        if (request.sortOrder() == null) fields.add(new ValidationError("sortOrder", "정렬순서를 입력하세요."));
        if (!USE_FLAGS.contains(activeYn)) fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        if (!hasText(changeReason)) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("평가요소별 관리항목 저장 요청이 올바르지 않습니다.", fields);

        return new SaveEvaluationElementManagementItemRequest(request.ruleVersionId(), evaluationYear, areaCode,
                elementCode, managementItemCode, managementItemName, teacherEditablePart, request.sortOrder(), activeYn, changeReason);
    }

    private void recordChangeHistory(EvaluationElementManagementItemRow before,
                                     EvaluationElementManagementItemRow after,
                                     SaveEvaluationElementManagementItemRequest request,
                                     Long userId,
                                     String requestId) {
        String beforeValue = before == null ? null : summary(before);
        String afterValue = summary(after);
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory(TARGET_BUSINESS, targetKey(request), before == null ? "CREATE" : "UPDATE",
                    "setting", beforeValue, afterValue, userId, request.changeReason(), requestId);
        }
    }

    private String targetKey(SaveEvaluationElementManagementItemRequest request) {
        return request.ruleVersionId() + ":" + request.evaluationYear() + ":" + request.areaCode() + ":" + request.elementCode() + ":" + request.managementItemCode();
    }

    private String summary(EvaluationElementManagementItemRow row) {
        return row.ruleVersionId() + ":" + row.managementItemName() + ":" + row.teacherEditablePart() + ":" + row.sortOrder() + ":" + row.activeYn();
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String normalized(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed.toUpperCase();
    }
}
