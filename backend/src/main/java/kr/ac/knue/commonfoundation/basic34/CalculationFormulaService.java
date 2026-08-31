package kr.ac.knue.commonfoundation.basic34;

import java.math.BigDecimal;
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
public class CalculationFormulaService {
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Set<String> CALCULATION_TYPES = Set.of("FIXED_SCORE", "DISTRIBUTION_RATE", "CAP", "LADDER");
    private static final Pattern EVALUATION_YEAR = Pattern.compile("^[0-9]{4}$");
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "calculation_formula_versions";
    private final CalculationFormulaMapper mapper;

    public CalculationFormulaService(CalculationFormulaMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CalculationFormulaSearchResponse list(CalculationFormulaSearchCriteria criteria) {
        return new CalculationFormulaSearchResponse(
                mapper.listCalculationFormulas(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countCalculationFormulas(criteria));
    }

    @Transactional
    public CalculationFormulaRow save(SaveCalculationFormulaRequest request, Long userId, String requestId) {
        SaveCalculationFormulaRequest normalized = normalizeAndValidate(request);
        String status = mapper.findRuleVersionStatus(normalized.ruleVersionId());
        if (status == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(status)) {
            throw new ConflictException("확정 또는 폐기된 규정버전의 계산식은 수정할 수 없습니다.");
        }

        CalculationFormulaRow before = mapper.findByKey(normalized);
        mapper.upsertCalculationFormula(normalized, userId);
        CalculationFormulaRow after = mapper.findByKey(normalized);
        recordChangeHistory(before, after, normalized, userId, requestId);
        return after;
    }

    private SaveCalculationFormulaRequest normalizeAndValidate(SaveCalculationFormulaRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.ruleVersionId() == null) fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        if (!hasText(request.formulaCode())) fields.add(new ValidationError("formulaCode", "산식 ID를 입력하세요."));
        if (!hasText(request.calculationType())) {
            fields.add(new ValidationError("calculationType", "계산 유형을 선택하세요."));
        } else if (!CALCULATION_TYPES.contains(normalized(request.calculationType()))) {
            fields.add(new ValidationError("calculationType", "승인된 계산 유형만 선택할 수 있습니다."));
        }
        if (!hasText(request.variableDefinition())) {
            fields.add(new ValidationError("variableDefinition", "변수 정의를 입력하세요."));
        } else if (!isJsonObjectText(request.variableDefinition().trim())) {
            fields.add(new ValidationError("variableDefinition", "변수 정의는 JSON 객체 형식이어야 합니다."));
        }
        if (!hasText(request.roundingRule())) fields.add(new ValidationError("roundingRule", "반올림 기준을 입력하세요."));
        if (request.upperBoundScore() != null && request.lowerBoundScore() != null
                && request.upperBoundScore().compareTo(request.lowerBoundScore()) < 0) {
            fields.add(new ValidationError("upperBoundScore", "상한은 하한 이상이어야 합니다."));
        }
        if (!hasText(request.evaluationYear())) {
            fields.add(new ValidationError("evaluationYear", "적용연도를 입력하세요."));
        } else if (!EVALUATION_YEAR.matcher(request.evaluationYear().trim()).matches()) {
            fields.add(new ValidationError("evaluationYear", "적용연도는 YYYY 형식이어야 합니다."));
        }
        if (request.effectiveStartDate() == null) fields.add(new ValidationError("effectiveStartDate", "적용시작일을 입력하세요."));
        if (request.effectiveEndDate() == null) fields.add(new ValidationError("effectiveEndDate", "적용종료일을 입력하세요."));
        if (request.effectiveStartDate() != null && request.effectiveEndDate() != null
                && request.effectiveEndDate().isBefore(request.effectiveStartDate())) {
            fields.add(new ValidationError("effectiveEndDate", "적용종료일은 시작일 이후여야 합니다."));
        }
        if (!USE_FLAGS.contains(trim(request.activeYn()))) fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        if (!hasText(request.changeReason())) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("계산식 저장 요청이 올바르지 않습니다.", fields);
        }
        return new SaveCalculationFormulaRequest(
                request.ruleVersionId(),
                normalized(request.formulaCode()),
                normalized(request.calculationType()),
                request.variableDefinition().trim(),
                normalized(request.roundingRule()),
                request.lowerBoundScore(),
                request.upperBoundScore(),
                request.evaluationYear().trim(),
                request.effectiveStartDate(),
                request.effectiveEndDate(),
                request.activeYn().trim(),
                request.changeReason().trim());
    }

    private void recordChangeHistory(CalculationFormulaRow before, CalculationFormulaRow after, SaveCalculationFormulaRequest request,
                                     Long userId, String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.ruleVersionId() + ":" + request.formulaCode() + ":" + request.evaluationYear()
                + ":" + request.effectiveStartDate() + ":" + request.effectiveEndDate();
        recordIfChanged(before == null ? null : before.calculationType(), after.calculationType(), "calculation_type", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.variableDefinition(), after.variableDefinition(), "variable_definition", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.roundingRule(), after.roundingRule(), "rounding_rule", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.lowerBoundScore(), after.lowerBoundScore(), "lower_bound_score", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.upperBoundScore(), after.upperBoundScore(), "upper_bound_score", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.activeYn(), after.activeYn(), "active_yn", changeType, targetKey, userId, request.changeReason(), requestId);
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
    private boolean isJsonObjectText(String value) { return value.startsWith("{") && value.endsWith("}"); }
}
