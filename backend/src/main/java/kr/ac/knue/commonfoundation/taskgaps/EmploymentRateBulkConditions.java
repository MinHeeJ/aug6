package kr.ac.knue.commonfoundation.taskgaps;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;

/**
 * Validates bulk preview filters without interpreting the unapproved generation/deletion policy.
 * JSON containers and scalars must never be silently coerced into a different database predicate.
 */
public final class EmploymentRateBulkConditions {
    private static final List<String> FILTERS = List.of(
            "managementNo", "managementItemCode", "certificationStatus");

    private EmploymentRateBulkConditions() {
    }

    /**
     * Returns only supplied, non-null string predicates for the existing scoped mapper.
     * An omitted/null condition means no optional filter; it does not approve execution or scope.
     */
    public static Map<String, Object> query(String evaluationYear, JsonNode condition) {
        Map<String, Object> query = new HashMap<>();
        query.put("evaluationYear", evaluationYear);
        if (condition == null || condition.isNull()) {
            return query;
        }
        if (!condition.isObject()) {
            throw invalid("대상 조건은 JSON 객체여야 합니다.");
        }
        condition.fields().forEachRemaining(entry -> {
            String field = entry.getKey();
            JsonNode value = entry.getValue();
            if (!FILTERS.contains(field)) {
                throw invalid("승인된 기존 실적 필터만 사용하세요.");
            }
            if (value.isNull()) {
                return;
            }
            if (!value.isTextual() || value.textValue().isBlank()) {
                throw invalid("대상 조건의 필터 값은 비어 있지 않은 문자열이어야 합니다.");
            }
            query.put(field, value.textValue().trim());
        });
        return query;
    }

    private static BusinessValidationException invalid(String message) {
        return new BusinessValidationException(
                "취업률 실적 입력값을 확인하세요.",
                List.of(new ValidationError("targetCondition", message)));
    }
}
