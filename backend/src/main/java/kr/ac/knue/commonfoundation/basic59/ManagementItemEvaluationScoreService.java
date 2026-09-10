package kr.ac.knue.commonfoundation.basic59;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagementItemEvaluationScoreService {
    private static final String DRAFT = "DRAFT";
    private static final String TARGET_BUSINESS = "management_item_score_settings";
    private final ManagementItemEvaluationScoreMapper mapper;

    public ManagementItemEvaluationScoreService(ManagementItemEvaluationScoreMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ManagementItemEvaluationScoreSearchResponse list(ManagementItemEvaluationScoreSearchCriteria criteria) {
        return new ManagementItemEvaluationScoreSearchResponse(
                mapper.listManagementItemEvaluationScores(criteria),
                criteria.safePage(),
                criteria.safeSize(),
                mapper.countManagementItemEvaluationScores(criteria));
    }

    @Transactional
    public ManagementItemEvaluationScoreRow save(SaveManagementItemEvaluationScoreRequest request, CurrentUser user, String requestId) {
        SaveManagementItemEvaluationScoreRequest normalized = normalizeAndValidate(request);
        String status = mapper.findRuleVersionStatus(normalized.ruleVersionId());
        if (status == null) {
            throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        }
        if (!DRAFT.equals(status)) {
            throw new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다.");
        }
        String evaluationYear = mapper.findRuleVersionEvaluationYear(normalized.ruleVersionId());
        if (evaluationYear == null || evaluationYear.isBlank()) {
            throw new NotFoundException("규정버전 적용연도를 찾을 수 없습니다.");
        }
        if (!mapper.managementItemMatchesCategory(normalized.ruleVersionId(), normalized.achievementAreaCode(), normalized.achievementCategoryCode(), normalized.managementItemCode())) {
            throw new BusinessValidationException("관리항목이 선택한 업적영역·업적분류에 속하지 않습니다.",
                    List.of(new ValidationError("managementItemCode", "관리항목과 업적분류를 확인하세요.")));
        }

        ManagementItemEvaluationScoreRow before = mapper.findByBusinessKey(
                normalized.ruleVersionId(), evaluationYear, normalized.achievementAreaCode(),
                normalized.managementItemCode(), normalized.collegeCode());
        ManagementItemEvaluationScoreRow after = mapper.upsertManagementItemEvaluationScore(normalized, user.userId(), requestId);
        recordChangeHistory(before, after, normalized, evaluationYear, user.userId(), requestId);
        return after;
    }

    private SaveManagementItemEvaluationScoreRequest normalizeAndValidate(SaveManagementItemEvaluationScoreRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String achievementAreaCode = normalized(request.achievementAreaCode());
        String achievementCategoryCode = normalized(request.achievementCategoryCode());
        String managementItemCode = normalized(request.managementItemCode());
        String collegeCode = normalized(request.collegeCode());
        String activeYn = normalized(request.activeYn());
        String changeReason = trim(request.changeReason());
        BigDecimal evaluationScore = request.evaluationScore();
        Integer sortOrder = request.sortOrder();

        if (request.ruleVersionId() == null) fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        if (!hasText(achievementAreaCode)) fields.add(new ValidationError("achievementAreaCode", "업적영역 코드를 입력하세요."));
        if (!hasText(achievementCategoryCode)) fields.add(new ValidationError("achievementCategoryCode", "업적분류 코드를 입력하세요."));
        if (!hasText(managementItemCode)) fields.add(new ValidationError("managementItemCode", "관리항목 코드를 입력하세요."));
        if (!hasText(collegeCode)) fields.add(new ValidationError("collegeCode", "소속대학 코드를 입력하세요."));
        if (evaluationScore == null) {
            fields.add(new ValidationError("evaluationScore", "평가점수를 입력하세요."));
        } else if (evaluationScore.compareTo(BigDecimal.ZERO) < 0) {
            fields.add(new ValidationError("evaluationScore", "평가점수는 0 이상이어야 합니다."));
        }
        if (sortOrder == null) {
            fields.add(new ValidationError("sortOrder", "정렬순서를 입력하세요."));
        } else if (sortOrder < 0) {
            fields.add(new ValidationError("sortOrder", "정렬순서는 0 이상이어야 합니다."));
        }
        if (!"Y".equals(activeYn) && !"N".equals(activeYn)) fields.add(new ValidationError("activeYn", "사용여부는 Y 또는 N만 가능합니다."));
        if (!hasText(changeReason)) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("관리항목별 평가점수 저장 요청이 올바르지 않습니다.", fields);

        return new SaveManagementItemEvaluationScoreRequest(request.ruleVersionId(), achievementAreaCode,
                achievementCategoryCode, managementItemCode, collegeCode, evaluationScore, sortOrder, activeYn, changeReason);
    }

    private void recordChangeHistory(ManagementItemEvaluationScoreRow before,
                                     ManagementItemEvaluationScoreRow after,
                                     SaveManagementItemEvaluationScoreRequest request,
                                     String evaluationYear,
                                     Long userId,
                                     String requestId) {
        String beforeValue = before == null ? null : summary(before);
        String afterValue = summary(after);
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory(TARGET_BUSINESS, targetKey(request, evaluationYear), before == null ? "CREATE" : "UPDATE",
                    "evaluationScore", beforeValue, afterValue, userId, request.changeReason(), requestId);
        }
    }

    private String targetKey(SaveManagementItemEvaluationScoreRequest request, String evaluationYear) {
        return request.ruleVersionId() + ":" + evaluationYear + ":" + request.achievementAreaCode() + ":" +
                request.managementItemCode() + ":" + request.collegeCode();
    }

    private String summary(ManagementItemEvaluationScoreRow row) {
        return row.ruleVersionId() + ":" + row.evaluationYear() + ":" + row.achievementAreaCode() + ":" +
                row.managementItemCode() + ":" + row.collegeCode() + ":" + row.evaluationScore() + ":" +
                row.sortOrder() + ":" + row.activeYn();
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String normalized(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed.toUpperCase();
    }
}
