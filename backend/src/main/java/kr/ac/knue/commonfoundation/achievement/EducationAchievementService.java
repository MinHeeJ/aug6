package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates protected lecture-evaluation list, save, attachment-reference, and certification transition flows.
 */
@Service
public class EducationAchievementService {
    private static final Set<String> ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04", "R09");
    private final EducationAchievementMapper mapper;
    private final EducationAchievementAccessValidator accessValidator;

    public EducationAchievementService(
            EducationAchievementMapper mapper,
            EducationAchievementAccessValidator accessValidator
    ) {
        this.mapper = mapper;
        this.accessValidator = accessValidator;
    }

    /** Reads a safely filtered page after enforcing the contracted education-achievement role boundary. */
    @Transactional(readOnly = true)
    public LectureEvaluationAchievementSearchResponse list(
            LectureEvaluationAchievementSearchCriteria criteria,
            CurrentUser user
    ) {
        requireRole(user);
        List<LectureEvaluationAchievementRow> rows = mapper.listLectureEvaluationAchievements(criteria)
                .stream()
                .map(this::toRow)
                .toList();
        return new LectureEvaluationAchievementSearchResponse(
                rows,
                Math.max(criteria.page(), 0),
                criteria.size(),
                mapper.countLectureEvaluationAchievements(criteria)
        );
    }

    /**
     * Saves a new or existing lecture-evaluation achievement with the shared period, scope, finalization, and warning guards.
     */
    @Transactional
    public LectureEvaluationAchievementRow save(
            LectureEvaluationAchievementRequest request,
            CurrentUser user,
            String requestId
    ) {
        requireRole(user);
        validateRequest(request);
        Map<String, Object> before = request.getAchievementId() == null
                ? null
                : mapper.findLectureEvaluationAchievement(request.getAchievementId());
        if (request.getAchievementId() != null && before == null) {
            throw new NotFoundException("강의평가 실적을 찾을 수 없습니다.");
        }
        Long targetUserId = before == null ? user.userId() : longValue(before.get("targetUserId"));
        String evaluationYear = before == null ? String.valueOf(java.time.Year.now().getValue())
                : stringValue(before.get("evaluationYear"));
        String organizationCode = before == null ? organizationCodeFor(user) : stringValue(before.get("organizationCode"));
        String status = before == null ? "DRAFTING" : stringValue(before.get("certificationStatus"));
        EducationAchievementAccessDecision decision = accessValidator.validateWrite(
                user,
                new AchievementWriteContext(targetUserId, evaluationYear, organizationCode, request.getOccurredDate(), status)
        );
        if (before == null) {
            mapper.insertLectureEvaluationAchievement(request, evaluationYear, organizationCode, user.userId());
        } else if (mapper.updateLectureEvaluationAchievement(request, user.userId()) == 0) {
            throw new NotFoundException("강의평가 실적을 찾을 수 없습니다.");
        }
        Map<String, Object> saved = mapper.findLectureEvaluationAchievement(request.getAchievementId());
        recordChange(before, saved, user.userId(), requestId);
        LectureEvaluationAchievementRow row = toRow(saved);
        if (decision.hasOccurredDateWarning()) {
            return new LectureEvaluationAchievementRow(
                    row.achievementId(), row.managementNo(), row.teacherName(), row.managementItemCode(), row.occurredDate(),
                    row.certificationStatus(), row.attachmentExists(), row.achievementDetail(), row.attachmentRef(),
                    row.evaluationYear(), row.targetUserId(), row.organizationCode()
            );
        }
        return row;
    }

    /**
     * Applies an allowed certification transition and stores its separate immutable status history in the same transaction.
     */
    @Transactional
    public LectureEvaluationAchievementRow transition(
            Long achievementId,
            LectureEvaluationStatusTransitionRequest request,
            CurrentUser user,
            String requestId
    ) {
        requireRole(user);
        Map<String, Object> before = mapper.findLectureEvaluationAchievement(achievementId);
        if (before == null) {
            throw new NotFoundException("강의평가 실적을 찾을 수 없습니다.");
        }
        EducationAchievementStatusTransition transition = accessValidator.prepareStatusTransition(
                "LECTURE_EVALUATION",
                achievementId,
                stringValue(before.get("certificationStatus")),
                request.nextStatus(),
                request.actionType(),
                request.reasonCode(),
                request.opinion(),
                user.userId(),
                LocalDateTime.now()
        );
        if (mapper.updateLectureEvaluationStatus(
                achievementId,
                transition.previousStatus(),
                transition.nextStatus(),
                user.userId()
        ) == 0) {
            throw new NotFoundException("강의평가 실적 상태가 변경되었습니다. 다시 조회하세요.");
        }
        mapper.insertStatusHistory(transition);
        Map<String, Object> after = mapper.findLectureEvaluationAchievement(achievementId);
        mapper.insertChangeHistory(
                "lecture_evaluation_achievements",
                String.valueOf(achievementId),
                "UPDATE",
                "certification_status",
                transition.previousStatus(),
                transition.nextStatus(),
                user.userId(),
                requestId
        );
        return toRow(after);
    }

    private void validateRequest(LectureEvaluationAchievementRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request.getManagementItemCode() == null || request.getManagementItemCode().isBlank()) {
            fields.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
        }
        if (request.getOccurredDate() == null) {
            fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("강의평가 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private void requireRole(CurrentUser user) {
        if (user == null || user.roles() == null || user.roles().stream().noneMatch(ACHIEVEMENT_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private String organizationCodeFor(CurrentUser user) {
        String organizationCode = mapper.findActiveOrganizationCodeForUser(user.userId());
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new ForbiddenException();
        }
        return organizationCode;
    }

    private void recordChange(Map<String, Object> before, Map<String, Object> after, Long userId, String requestId) {
        mapper.insertChangeHistory(
                "lecture_evaluation_achievements",
                String.valueOf(after.get("achievementId")),
                before == null ? "CREATE" : "UPDATE",
                "achievement",
                before == null ? null : stringValue(before.get("achievementDetail")),
                stringValue(after.get("achievementDetail")),
                userId,
                requestId
        );
    }

    private LectureEvaluationAchievementRow toRow(Map<String, Object> row) {
        return new LectureEvaluationAchievementRow(
                longValue(row.get("achievementId")),
                stringValue(row.get("managementNo")),
                stringValue(row.get("teacherName")),
                stringValue(row.get("managementItemCode")),
                localDateValue(row.get("occurredDate")),
                stringValue(row.get("certificationStatus")),
                Boolean.TRUE.equals(row.get("attachmentExists")),
                stringValue(row.get("achievementDetail")),
                stringValue(row.get("attachmentRef")),
                stringValue(row.get("evaluationYear")),
                longValue(row.get("targetUserId")),
                stringValue(row.get("organizationCode"))
        );
    }

    private java.time.LocalDate localDateValue(Object value) {
        if (value instanceof java.time.LocalDate localDate) return localDate;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        return value == null ? null : java.time.LocalDate.parse(value.toString());
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        return value == null ? null : Long.parseLong(value.toString());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
