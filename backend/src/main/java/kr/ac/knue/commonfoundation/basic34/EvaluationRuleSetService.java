package kr.ac.knue.commonfoundation.basic34;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationRuleSetService {
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Set<String> RULE_SET_STATUSES = Set.of("DRAFT", "CONFIRMED", "DISCARDED");
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "evaluation_rule_sets";
    private final EvaluationRuleSetMapper mapper;

    public EvaluationRuleSetService(EvaluationRuleSetMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationRuleSetSearchResponse list(EvaluationRuleSetSearchCriteria criteria) {
        return new EvaluationRuleSetSearchResponse(
                mapper.listEvaluationRuleSets(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countEvaluationRuleSets(criteria));
    }

    @Transactional
    public EvaluationRuleSetRow save(SaveEvaluationRuleSetRequest request, Long userId, String requestId) {
        SaveEvaluationRuleSetRequest normalized = normalizeAndValidate(request);
        String versionStatus = mapper.findRuleVersionStatus(normalized.ruleVersionId());
        if (versionStatus == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(versionStatus)) {
            throw new ConflictException("확정 또는 폐기된 규정버전의 업적평가 기준·점수규칙은 수정할 수 없습니다.");
        }

        EvaluationRuleSetRow before = mapper.findByKey(normalized);
        mapper.upsertEvaluationRuleSet(normalized, userId);
        EvaluationRuleSetRow after = mapper.findByKey(normalized);
        recordChangeHistory(before, after, normalized, userId, requestId);
        return after;
    }

    private SaveEvaluationRuleSetRequest normalizeAndValidate(SaveEvaluationRuleSetRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.ruleVersionId() == null) fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        if (!hasText(request.targetScope())) fields.add(new ValidationError("targetScope", "적용 대상을 입력하세요."));
        if (!hasText(request.ruleSetName())) fields.add(new ValidationError("ruleSetName", "기준·점수규칙명을 입력하세요."));
        if (!hasText(request.ruleSetStatus())) {
            fields.add(new ValidationError("ruleSetStatus", "규칙 상태를 선택하세요."));
        } else if (!RULE_SET_STATUSES.contains(normalized(request.ruleSetStatus()))) {
            fields.add(new ValidationError("ruleSetStatus", "작성중, 확정, 폐기 중 하나를 선택하세요."));
        }
        if (!USE_FLAGS.contains(trim(request.activeYn()))) fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        if (request.effectiveStartDate() == null) fields.add(new ValidationError("effectiveStartDate", "적용시작일을 입력하세요."));
        if (request.effectiveEndDate() == null) fields.add(new ValidationError("effectiveEndDate", "적용종료일을 입력하세요."));
        if (request.effectiveStartDate() != null && request.effectiveEndDate() != null
                && request.effectiveEndDate().isBefore(request.effectiveStartDate())) {
            fields.add(new ValidationError("effectiveEndDate", "적용종료일은 시작일 이후여야 합니다."));
        }
        if (!hasText(request.changeReason())) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("업적평가 기준·점수규칙 저장 요청이 올바르지 않습니다.", fields);
        }
        return new SaveEvaluationRuleSetRequest(
                request.ruleVersionId(),
                request.targetScope().trim(),
                request.ruleSetName().trim(),
                normalized(request.ruleSetStatus()),
                request.activeYn().trim(),
                request.effectiveStartDate(),
                request.effectiveEndDate(),
                request.changeReason().trim());
    }

    private void recordChangeHistory(EvaluationRuleSetRow before, EvaluationRuleSetRow after, SaveEvaluationRuleSetRequest request,
                                     Long userId, String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.ruleVersionId() + ":" + request.targetScope() + ":" + request.ruleSetName()
                + ":" + request.effectiveStartDate() + ":" + request.effectiveEndDate();
        recordIfChanged(before == null ? null : before.ruleSetStatus(), after.ruleSetStatus(), "rule_set_status", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.activeYn(), after.activeYn(), "active_yn", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.changeReason(), after.changeReason(), "change_reason", changeType, targetKey, userId, request.changeReason(), requestId);
    }

    private void recordIfChanged(Object beforeValue, Object afterValue, String fieldName, String changeType,
                                 String targetKey, Long userId, String changeReason, String requestId) {
        String beforeString = beforeValue == null ? null : beforeValue.toString();
        String afterString = afterValue == null ? null : afterValue.toString();
        if (!Objects.equals(beforeString, afterString)) {
            mapper.insertChangeHistory(TARGET_BUSINESS, targetKey, changeType, fieldName, beforeString, afterString,
                    userId, changeReason, requestId);
        }
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String normalized(String value) { return value == null ? null : value.trim().toUpperCase(); }
}
