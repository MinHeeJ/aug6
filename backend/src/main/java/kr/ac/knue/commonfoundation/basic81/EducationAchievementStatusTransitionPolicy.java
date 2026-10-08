package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;

/**
 * Defines the only permitted education-achievement lifecycle transitions and
 * requires rejection context before a status-history record is created.
 * The new header-backed achievement types share the unchanged legacy lifecycle graph.
 */
@Service
public class EducationAchievementStatusTransitionPolicy {
    private static final Set<String> ACHIEVEMENT_TYPES = Set.of(
            "LECTURE_EVALUATION",
            "LECTURE",
            "STUDENT_GUIDANCE",
            "DEGREE_COMPLETION",
            "EMPLOYMENT_RATE_IMPROVEMENT",
            "COURSE_OPERATION",
            "LECTURE_IMPROVEMENT",
            "EMPLOYMENT_RATE_ACHIEVEMENT");
    private static final Map<EducationAchievementStatus, Set<EducationAchievementStatus>> ALLOWED_TRANSITIONS =
            allowedTransitions();

    /**
     * Validates one lifecycle change and returns the immutable history value
     * that the caller must persist with the source row in the same transaction.
     */
    public EducationAchievementStatusHistory transition(
            EducationAchievementStatusTransitionRequest request) {
        validateRequest(request);
        if (!ALLOWED_TRANSITIONS.getOrDefault(request.currentStatus(), Set.of()).contains(request.nextStatus())) {
            throw new ConflictException("허용되지 않은 교육영역 실적 상태 전이입니다.");
        }
        if (requiresRejectionContext(request.nextStatus())
                && isBlank(request.reasonCode())
                && isBlank(request.opinion())) {
            throw new BusinessValidationException(
                    "반려 처리에는 사유 또는 의견을 입력하세요.",
                    List.of(new ValidationError("reasonCode", "학과장미승인 또는 인증반려에는 사유 또는 의견이 필요합니다.")));
        }
        return new EducationAchievementStatusHistory(
                request.achievementType().trim(),
                request.achievementId(),
                request.currentStatus(),
                request.nextStatus(),
                request.actionType().trim(),
                blankToNull(request.reasonCode()),
                blankToNull(request.opinion()),
                request.processedBy(),
                request.processedAt());
    }

    private void validateRequest(EducationAchievementStatusTransitionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            throw new BusinessValidationException(
                    "교육영역 실적 상태 전이 요청이 필요합니다.",
                    List.of(new ValidationError("body", "상태 전이 요청을 입력하세요.")));
        }
        if (request.achievementType() == null
                || !ACHIEVEMENT_TYPES.contains(request.achievementType())) {
            fields.add(new ValidationError("achievementType", "유효한 교육영역 실적 유형을 입력하세요."));
        }
        if (request.achievementId() == null || request.achievementId() <= 0) {
            fields.add(new ValidationError("achievementId", "실적 식별자를 입력하세요."));
        }
        if (request.currentStatus() == null) {
            fields.add(new ValidationError("currentStatus", "현재 상태를 입력하세요."));
        }
        if (request.nextStatus() == null) {
            fields.add(new ValidationError("nextStatus", "다음 상태를 입력하세요."));
        }
        if (isBlank(request.actionType())) {
            fields.add(new ValidationError("actionType", "처리구분을 입력하세요."));
        }
        if (request.processedBy() == null || request.processedBy() <= 0) {
            fields.add(new ValidationError("processedBy", "처리자를 입력하세요."));
        }
        if (request.processedAt() == null) {
            fields.add(new ValidationError("processedAt", "처리일시를 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("교육영역 실적 상태 전이 요청이 올바르지 않습니다.", fields);
        }
    }

    private static Map<EducationAchievementStatus, Set<EducationAchievementStatus>> allowedTransitions() {
        Map<EducationAchievementStatus, Set<EducationAchievementStatus>> transitions =
                new EnumMap<>(EducationAchievementStatus.class);
        transitions.put(EducationAchievementStatus.DRAFT, Set.of(EducationAchievementStatus.SUBMITTED));
        transitions.put(
                EducationAchievementStatus.SUBMITTED,
                Set.of(
                        EducationAchievementStatus.DEPARTMENT_CONFIRMED,
                        EducationAchievementStatus.DEPARTMENT_REJECTED));
        transitions.put(
                EducationAchievementStatus.DEPARTMENT_REJECTED,
                Set.of(EducationAchievementStatus.SUBMITTED));
        transitions.put(
                EducationAchievementStatus.DEPARTMENT_CONFIRMED,
                Set.of(
                        EducationAchievementStatus.CERTIFIED,
                        EducationAchievementStatus.CERTIFICATION_REJECTED));
        transitions.put(
                EducationAchievementStatus.CERTIFICATION_REJECTED,
                Set.of(EducationAchievementStatus.SUBMITTED));
        transitions.put(
                EducationAchievementStatus.CERTIFIED,
                Set.of(EducationAchievementStatus.EVALUATION_CONFIRMED));
        transitions.put(
                EducationAchievementStatus.EVALUATION_CONFIRMED,
                Set.of(EducationAchievementStatus.CERTIFIED));
        return Map.copyOf(transitions);
    }

    private boolean requiresRejectionContext(EducationAchievementStatus nextStatus) {
        return nextStatus == EducationAchievementStatus.DEPARTMENT_REJECTED
                || nextStatus == EducationAchievementStatus.CERTIFICATION_REJECTED;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
