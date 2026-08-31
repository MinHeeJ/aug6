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
public class EvaluationScoreService {
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Pattern EVALUATION_YEAR = Pattern.compile("^[0-9]{4}$");
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "evaluation_score_rules";
    private final EvaluationScoreMapper mapper;

    public EvaluationScoreService(EvaluationScoreMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationScoreSearchResponse list(EvaluationScoreSearchCriteria criteria) {
        return new EvaluationScoreSearchResponse(
                mapper.listEvaluationScores(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countEvaluationScores(criteria));
    }

    @Transactional
    public EvaluationScoreRow save(SaveEvaluationScoreRequest request, Long userId, String requestId) {
        SaveEvaluationScoreRequest normalized = normalizeAndValidate(request);
        String status = mapper.findRuleVersionStatus(normalized.ruleVersionId());
        if (status == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(status)) {
            throw new ConflictException("확정 또는 폐기된 규정버전의 평가점수는 수정할 수 없습니다.");
        }
        if (!Boolean.TRUE.equals(mapper.managementItemBelongsToRuleVersion(normalized.ruleVersionId(), normalized.managementItemId()))) {
            throw new NotFoundException("규정버전에 속한 관리항목을 찾을 수 없습니다.");
        }

        EvaluationScoreRow before = mapper.findByKey(normalized);
        mapper.upsertEvaluationScore(normalized, userId);
        EvaluationScoreRow after = mapper.findByKey(normalized);
        recordChangeHistory(before, after, normalized, userId, requestId);
        return after;
    }

    private SaveEvaluationScoreRequest normalizeAndValidate(SaveEvaluationScoreRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.ruleVersionId() == null) fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        if (request.managementItemId() == null) fields.add(new ValidationError("managementItemId", "관리항목을 선택하세요."));
        if (!hasText(request.organizationCode())) fields.add(new ValidationError("organizationCode", "소속대학 코드를 입력하세요."));
        if (!hasText(request.evaluationYear())) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        } else if (!EVALUATION_YEAR.matcher(request.evaluationYear().trim()).matches()) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        }
        if (request.baseScore() == null) fields.add(new ValidationError("baseScore", "평가점수를 입력하세요."));
        if (request.maxScore() != null && request.baseScore() != null && request.maxScore().compareTo(request.baseScore()) < 0) {
            fields.add(new ValidationError("maxScore", "최대점수는 평가점수 이상이어야 합니다."));
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
            throw new BusinessValidationException("평가점수 저장 요청이 올바르지 않습니다.", fields);
        }
        return new SaveEvaluationScoreRequest(
                request.ruleVersionId(),
                request.managementItemId(),
                normalized(request.organizationCode()),
                request.evaluationYear().trim(),
                request.baseScore(),
                request.maxScore(),
                request.effectiveStartDate(),
                request.effectiveEndDate(),
                request.activeYn().trim(),
                request.changeReason().trim());
    }

    private void recordChangeHistory(EvaluationScoreRow before, EvaluationScoreRow after, SaveEvaluationScoreRequest request,
                                     Long userId, String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = request.ruleVersionId() + ":" + request.managementItemId() + ":" + request.organizationCode()
                + ":" + request.evaluationYear() + ":" + request.effectiveStartDate() + ":" + request.effectiveEndDate();
        recordIfChanged(before == null ? null : before.baseScore(), after.baseScore(), "base_score", changeType, targetKey, userId, request.changeReason(), requestId);
        recordIfChanged(before == null ? null : before.maxScore(), after.maxScore(), "max_score", changeType, targetKey, userId, request.changeReason(), requestId);
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
}
