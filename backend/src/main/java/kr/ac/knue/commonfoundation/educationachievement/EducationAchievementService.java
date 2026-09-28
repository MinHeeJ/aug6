package kr.ac.knue.commonfoundation.educationachievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.CodedConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements persistence and workflow rules for the approved education-achievement input screens. */
@Service
public class EducationAchievementService {
    private static final Set<String> SUPPORTED_TYPES = Set.of("LECTURE_EVALUATION", "LECTURE_ACHIEVEMENT", "STUDENT_GUIDANCE", "DEGREE_COMPLETION");
    private static final Set<String> ACTIONS = Set.of("SUBMIT", "CONFIRM", "REJECT", "CERTIFY", "RETURN");
    private final EducationAchievementMapper mapper;

    public EducationAchievementService(EducationAchievementMapper mapper) {
        this.mapper = mapper;
    }

    /** Returns a caller-scoped, paged list after validating the requested achievement type and page size. */
    @Transactional(readOnly = true)
    public EducationAchievementSearchResponse list(EducationAchievementSearchCriteria criteria, CurrentUser user) {
        if (criteria == null || !criteria.hasSupportedAchievementType()) throw validation("achievementType", "지원하지 않는 실적 유형입니다.");
        requireFunctionPermission(criteria.achievementType(), user, "READ");
        Long ownerUserId = user.roles().contains("R01") ? user.userId() : null;
        return new EducationAchievementSearchResponse(mapper.list(criteria, ownerUserId), criteria.safePage(), criteria.safeSize(), mapper.count(criteria, ownerUserId));
    }

    /** Creates or updates a supported achievement, replacing degree-completion children in the same transaction. */
    @Transactional
    public EducationAchievementRow save(SaveEducationAchievementRequest request, CurrentUser user, String requestId) {
        List<ValidationError> errors = validateSave(request);
        if (!errors.isEmpty()) throw new BusinessValidationException("교육영역 실적 저장 요청이 올바르지 않습니다.", errors);
        String achievementType = request.achievementType().trim().toUpperCase();
        if (!SUPPORTED_TYPES.contains(achievementType)) throw validation("achievementType", "지원하지 않는 실적 유형입니다.");
        requireFunctionPermission(achievementType, user, "CREATE");
        LocalDate occurrenceDate = request.occurrenceDate();
        if (mapper.activeInputPeriodExists(String.valueOf(occurrenceDate.getYear())) == 0) throw new CodedConflictException("PERIOD_NOT_ACTIVE", "현재 평가연도 입력기간이 아닙니다.");

        Long achievementId = request.achievementId();
        boolean creating = achievementId == null;
        if (creating) {
            achievementId = mapper.insertAchievement(UUID.randomUUID().toString(), databaseAchievementType(achievementType), String.valueOf(occurrenceDate.getYear()), user.userId(), request.managementItemCode().trim().toUpperCase(), occurrenceDate);
            mapper.insertManagementValue(achievementId, request.managementItemCode().trim().toUpperCase(), request.managementItemCode().trim(), user.userId());
        } else {
            EducationAchievementRow existing = mapper.findByIdForUpdate(achievementId);
            if (existing == null || !achievementType.equals(existing.achievementType())) throw new NotFoundException("교육영역 실적을 찾을 수 없습니다.");
            if ("Y".equals(existing.evaluationConfirmedYn()) || "EVALUATION_CONFIRMED".equals(existing.achievementStatus())) throw new CodedConflictException("CONFIRMED_DATA_LOCKED", "평가확정된 실적은 수정할 수 없습니다.");
            mapper.updateAchievement(achievementId, request.managementItemCode().trim().toUpperCase(), occurrenceDate, user.userId());
            mapper.replaceManagementValue(achievementId, request.managementItemCode().trim().toUpperCase(), request.managementItemCode().trim(), user.userId());
        }
        if ("STUDENT_GUIDANCE".equals(achievementType)) {
            mapper.deleteStudentGuidanceDetails(achievementId);
            for (StudentGuidanceDetailRequest detail : request.studentGuidanceDetails()) {
                mapper.insertStudentGuidanceDetail(achievementId, detail.studentName().trim(), detail.guidanceStartDate(),
                        detail.guidanceEndDate(), detail.studentCount(), user.userId());
            }
        }
        if ("DEGREE_COMPLETION".equals(achievementType)) {
            mapper.deleteDegreeCompletionStudentDetails(achievementId);
            for (DegreeCompletionStudentDetailRequest detail : request.degreeCompletionStudentDetails()) {
                mapper.insertDegreeCompletionStudentDetail(achievementId, databaseDegreeType(detail.degreeType()), detail.studentName().trim(), detail.thesisTitle().trim(), detail.degreeAwardedOn(), user.userId());
            }
        }
        EducationAchievementRow saved = mapper.findById(achievementId);
        mapper.insertChangeHistory(String.valueOf(achievementId), creating ? "CREATE" : "UPDATE", "achievement_status", null, saved.achievementStatus(), user.userId(), "교육영역 실적 저장", requestId);
        return saved;
    }

    /** Applies a configured status transition and appends an immutable workflow-history record. */
    @Transactional
    public EducationAchievementTransitionResponse transition(Long achievementId, BusinessTransitionRequest request, CurrentUser user, String requestId) {
        List<ValidationError> errors = validateTransition(achievementId, request);
        if (!errors.isEmpty()) throw new BusinessValidationException("교육영역 상태 전이 요청이 올바르지 않습니다.", errors);
        EducationAchievementRow current = mapper.findByIdForUpdate(achievementId);
        if (current == null || !SUPPORTED_TYPES.contains(current.achievementType())) throw new NotFoundException("교육영역 실적을 찾을 수 없습니다.");
        requireTransitionPermission(current.achievementType(), user);
        if ("Y".equals(current.evaluationConfirmedYn()) || "EVALUATION_CONFIRMED".equals(current.achievementStatus())) throw new ConflictException("평가확정된 실적은 상태를 변경할 수 없습니다.");
        String nextStatus = nextStatus(request.actionType());
        if (mapper.transitionExists(current.achievementStatus(), nextStatus, user.roles()) == 0) throw new ConflictException("현재 상태에서는 요청한 상태 전이를 수행할 수 없습니다.");
        String reason = trimToNull(request.opinion());
        mapper.updateStatus(achievementId, nextStatus, user.userId(), reason);
        mapper.insertStatusHistory(achievementId, current.achievementStatus(), nextStatus, user.userId(), reason, requestId);
        mapper.insertChangeHistory(String.valueOf(achievementId), "UPDATE", "achievement_status", current.achievementStatus(), nextStatus, user.userId(), reason == null ? "교육영역 상태 전이" : reason, requestId);
        return new EducationAchievementTransitionResponse(nextStatus, mapper.findLatestStatusHistory(achievementId));
    }

    /**
     * Marks an achievement deleted and records the pre-delete state in the common audit history.
     * Physical removal is deliberately avoided so operational history and recovery investigations remain possible.
     */
    @Transactional
    public EducationAchievementDeletionResponse delete(Long achievementId, CurrentUser user, String requestId) {
        if (achievementId == null || achievementId <= 0) throw validation("achievementId", "실적을 선택하세요.");
        if (!user.roles().contains("R01")) throw new ForbiddenException();

        EducationAchievementRow existing = mapper.findByIdForUpdate(achievementId);
        if (existing != null) {
            requireFunctionPermission(existing.achievementType(), user, "DELETE");
            if (!user.userId().equals(existing.ownerUserId())) throw new ForbiddenException();
            if ("Y".equals(existing.evaluationConfirmedYn()) || "EVALUATION_CONFIRMED".equals(existing.achievementStatus())) {
                throw new CodedConflictException("CONFIRMED_DATA_LOCKED", "평가확정된 실적은 삭제할 수 없습니다.");
            }
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        String beforeValue = snapshotBeforeDeletion(achievementId, existing);
        mapper.logicallyDeleteAchievement(achievementId, user.userId(), "교육영역 실적 삭제");
        mapper.insertChangeHistory(String.valueOf(achievementId), "DELETE", "deleted_yn", beforeValue, "Y",
                user.userId(), "교육영역 실적 삭제", requestId);
        return new EducationAchievementDeletionResponse(achievementId, "Y", user.userId(), deletedAt,
                new EducationAchievementChangeHistory("DELETE", beforeValue, user.userId(), requestId));
    }

    private List<ValidationError> validateSave(SaveEducationAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null || blank(request.achievementType())) errors.add(new ValidationError("achievementType", "실적 유형을 선택하세요."));
        if (request == null || blank(request.managementItemCode())) errors.add(new ValidationError("managementItemCode", "관리항목 코드를 입력하세요."));
        if (request == null || request.occurrenceDate() == null) errors.add(new ValidationError("occurrenceDate", "발생일을 입력하세요."));
        if (request != null && "STUDENT_GUIDANCE".equalsIgnoreCase(request.achievementType())) {
            List<StudentGuidanceDetailRequest> details = request.studentGuidanceDetails();
            if (details == null || details.isEmpty()) errors.add(new ValidationError("studentGuidanceDetails", "학생지도 정보를 1건 이상 입력하세요."));
            else for (StudentGuidanceDetailRequest detail : details) {
                if (detail == null || blank(detail.studentName())) errors.add(new ValidationError("studentGuidanceDetails.studentName", "지도학생명을 입력하세요."));
                if (detail == null || detail.guidanceStartDate() == null) errors.add(new ValidationError("studentGuidanceDetails.guidanceStartDate", "지도 시작일을 입력하세요."));
                if (detail == null || detail.guidanceEndDate() == null) errors.add(new ValidationError("studentGuidanceDetails.guidanceEndDate", "지도 종료일을 입력하세요."));
                if (detail != null && detail.guidanceStartDate() != null && detail.guidanceEndDate() != null && detail.guidanceEndDate().isBefore(detail.guidanceStartDate())) errors.add(new ValidationError("studentGuidanceDetails.guidanceEndDate", "지도 종료일은 시작일보다 빠를 수 없습니다."));
                if (detail == null || detail.studentCount() == null || detail.studentCount() <= 0) errors.add(new ValidationError("studentGuidanceDetails.studentCount", "학생수는 1명 이상이어야 합니다."));
            }
        }
        if (request != null && "DEGREE_COMPLETION".equalsIgnoreCase(request.achievementType())) {
            List<DegreeCompletionStudentDetailRequest> details = request.degreeCompletionStudentDetails();
            if (details == null || details.isEmpty()) errors.add(new ValidationError("degreeCompletionStudentDetails", "석사 또는 박사 배출 학생 정보를 입력하세요."));
            else for (DegreeCompletionStudentDetailRequest detail : details) {
                if (detail == null || blank(detail.degreeType()) || !Set.of("MASTER", "DOCTOR", "DOCTORATE").contains(detail.degreeType().trim().toUpperCase())) errors.add(new ValidationError("degreeCompletionStudentDetails.degreeType", "학위구분은 MASTER 또는 DOCTOR여야 합니다."));
                if (detail == null || blank(detail.studentName())) errors.add(new ValidationError("degreeCompletionStudentDetails.studentName", "학생명을 입력하세요."));
                if (detail == null || blank(detail.thesisTitle())) errors.add(new ValidationError("degreeCompletionStudentDetails.thesisTitle", "논문 제목을 입력하세요."));
                if (detail == null || detail.degreeAwardedOn() == null) errors.add(new ValidationError("degreeCompletionStudentDetails.degreeAwardedOn", "학위수여일을 입력하세요."));
            }
        }
        return errors;
    }

    private List<ValidationError> validateTransition(Long achievementId, BusinessTransitionRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (achievementId == null || achievementId <= 0) errors.add(new ValidationError("achievementId", "실적을 선택하세요."));
        String action = request == null ? null : request.actionType();
        if (blank(action) || !ACTIONS.contains(action.trim().toUpperCase())) errors.add(new ValidationError("actionType", "허용되지 않은 처리구분입니다."));
        if ("REJECT".equalsIgnoreCase(action) || "RETURN".equalsIgnoreCase(action)) if (request == null || (blank(request.reasonCode()) && blank(request.opinion()))) errors.add(new ValidationError("opinion", "반려 또는 미승인 사유·의견을 입력하세요."));
        return errors;
    }

    private void requireFunctionPermission(String achievementType, CurrentUser user, String functionType) {
        if (mapper.functionPermissionAllowed(screenId(achievementType), user.roles(), functionType) == 0) throw new ForbiddenException();
    }

    private void requireTransitionPermission(String achievementType, CurrentUser user) {
        if (mapper.functionPermissionAllowed(screenId(achievementType), user.roles(), "EXECUTE") == 0 && mapper.functionPermissionAllowed(screenId(achievementType), user.roles(), "CREATE") == 0) throw new ForbiddenException();
    }

    private String nextStatus(String action) {
        return switch (action.trim().toUpperCase()) {
            case "SUBMIT" -> "SUBMITTED";
            case "CONFIRM" -> "DEPARTMENT_CONFIRMED";
            case "REJECT" -> "DEPARTMENT_REJECTED";
            case "CERTIFY" -> "CERTIFIED";
            case "RETURN" -> "CERTIFICATION_RETURNED";
            default -> throw new IllegalArgumentException("허용되지 않은 처리구분입니다.");
        };
    }

    private String screenId(String achievementType) {
        return switch (achievementType.trim().toUpperCase()) {
            case "LECTURE_ACHIEVEMENT" -> "SCR-LECTURE-ACHIEVEMENT";
            case "STUDENT_GUIDANCE" -> "SCR-STUDENT-GUIDANCE-ACHIEVEMENT";
            case "DEGREE_COMPLETION" -> "SCR-DEGREE-COMPLETION-ACHIEVEMENT";
            default -> "SCR-LECTURE-EVALUATION-ACHIEVEMENT";
        };
    }

    private String databaseAchievementType(String achievementType) {
        return "LECTURE_ACHIEVEMENT".equals(achievementType) ? "LECTURE" : achievementType;
    }

    private String databaseDegreeType(String degreeType) {
        return "DOCTORATE".equalsIgnoreCase(degreeType) ? "DOCTOR" : degreeType.trim().toUpperCase();
    }

    private String snapshotBeforeDeletion(Long achievementId, EducationAchievementRow existing) {
        if (existing == null) return "achievementId=" + achievementId;
        return "achievementId=" + existing.achievementId()
                + ",achievementStatus=" + existing.achievementStatus()
                + ",deletedYn=N";
    }

    private BusinessValidationException validation(String field, String message) {
        return new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String trimToNull(String value) { return blank(value) ? null : value.trim(); }
}
