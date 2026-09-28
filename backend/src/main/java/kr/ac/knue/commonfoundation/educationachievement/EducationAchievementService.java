package kr.ac.knue.commonfoundation.educationachievement;

import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic43.BusinessTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies education-achievement access, period, confirmation-lock, detail persistence, and state-history invariants.
 */
@Service
public class EducationAchievementService {
    private static final Set<String> TYPES = Set.of("LECTURE_EVALUATION", "LECTURE_ACHIEVEMENT", "STUDENT_GUIDANCE", "DEGREE_COMPLETION");
    private final EducationAchievementMapper mapper;

    public EducationAchievementService(EducationAchievementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EducationAchievementSearchResponse list(String achievementType, int page, int size, CurrentUser user) {
        requireRole(user);
        validateType(achievementType);
        int safePage = Math.max(page, 0);
        int safeSize = Set.of(20, 50, 100).contains(size) ? size : 20;
        Long facultyScope = user.roles().contains("R01") ? user.userId() : null;
        List<EducationAchievementRow> rows = mapper.list(achievementType, facultyScope, safeSize, safePage * safeSize);
        if ("DEGREE_COMPLETION".equals(achievementType)) {
            rows = rows.stream()
                    .map(row -> row.withDegreeCompletionDetails(mapper.findDegreeCompletionDetails(row.achievementId())))
                    .toList();
        } else if ("STUDENT_GUIDANCE".equals(achievementType)) {
            rows = rows.stream()
                    .map(row -> row.withStudentGuidanceDetails(mapper.findStudentGuidanceDetails(row.achievementId())))
                    .toList();
        }
        return new EducationAchievementSearchResponse(rows, safePage, safeSize,
                mapper.count(achievementType, facultyScope));
    }

    /**
     * Saves a new or existing achievement only during the reusable education input period.
     * Degree details are replaced atomically with their owning master so a partial detail update cannot persist.
     */
    @Transactional
    public EducationAchievementRow save(EducationAchievementSaveRequest request, CurrentUser user, String requestId) {
        requireRole(user);
        validateType(request.achievementType());
        validateDegreeDetails(request);
        validateStudentGuidanceDetails(request);
        if (mapper.hasActiveEducationInputPeriod() == 0) {
            throw new ForbiddenException("교육영역 실적 입력기간이 아닙니다.");
        }
        EducationAchievementRow saved;
        String changeType;
        String beforeStatus = null;
        if (request.achievementId() == null) {
            saved = mapper.insert(request, user.userId(), user.userId(), "교육영역 실적 등록");
            changeType = "CREATE";
        } else {
            EducationAchievementRow existing = mapper.findById(request.achievementId());
            if (existing == null) throw new NotFoundException("교육영역 실적을 찾을 수 없습니다.");
            if (user.roles().contains("R01") && !user.userId().equals(existing.facultyUserId())) throw new ForbiddenException();
            if ("EVALUATION_CONFIRMED".equals(existing.certificationStatus())) {
                throw new ConfirmedDataLockedException();
            }
            if (!existing.achievementType().equals(request.achievementType())) {
                throw new BusinessValidationException("실적유형은 변경할 수 없습니다.",
                        List.of(new ValidationError("achievementType", "기존 실적유형과 같아야 합니다.")));
            }
            mapper.update(request, user.userId(), "교육영역 실적 수정");
            saved = mapper.findById(request.achievementId());
            changeType = "UPDATE";
            beforeStatus = existing.certificationStatus();
        }
        if ("DEGREE_COMPLETION".equals(request.achievementType())) {
            mapper.deleteDegreeCompletionDetails(saved.achievementId());
            for (DegreeCompletionDetail detail : request.degreeCompletionDetails()) {
                mapper.insertDegreeCompletionDetail(saved.achievementId(), detail, user.userId());
            }
            saved = saved.withDegreeCompletionDetails(mapper.findDegreeCompletionDetails(saved.achievementId()));
        } else if ("STUDENT_GUIDANCE".equals(request.achievementType())) {
            mapper.deleteStudentGuidanceDetails(saved.achievementId());
            for (StudentGuidanceDetail detail : request.studentGuidanceDetails()) {
                mapper.insertStudentGuidanceDetail(saved.achievementId(), detail, user.userId());
            }
            saved = saved.withStudentGuidanceDetails(mapper.findStudentGuidanceDetails(saved.achievementId()));
        }
        mapper.insertChangeHistory(String.valueOf(saved.achievementId()), changeType, beforeStatus, saved.certificationStatus(), user.userId(),
                "CREATE".equals(changeType) ? "교육영역 실적 등록" : "교육영역 실적 수정", requestId);
        return saved;
    }

    /** Transitions an owned or authorized achievement and records the required append-only status history. */
    @Transactional
    public EducationAchievementRow transition(Long achievementId, BusinessTransitionRequest request, CurrentUser user, String requestId) {
        requireRole(user);
        if (achievementId == null || achievementId <= 0) {
            throw new BusinessValidationException("상태전이 대상이 올바르지 않습니다.", List.of(new ValidationError("achievementId", "실적을 선택하세요.")));
        }
        EducationAchievementRow current = mapper.findById(achievementId);
        if (current == null) throw new NotFoundException("교육영역 실적을 찾을 수 없습니다.");
        if (user.roles().contains("R01") && !user.userId().equals(current.facultyUserId())) throw new ForbiddenException();
        if ("EVALUATION_CONFIRMED".equals(current.certificationStatus())) throw new ConfirmedDataLockedException();
        String toStatus = actionToStatus(request);
        if (("DEPARTMENT_REJECTED".equals(toStatus) || "CERTIFICATION_RETURNED".equals(toStatus))
                && ((request.reasonCode() == null || request.reasonCode().isBlank()) && (request.opinion() == null || request.opinion().isBlank()))) {
            throw new BusinessValidationException("반려 또는 미승인 사유를 입력하세요.", List.of(new ValidationError("opinion", "사유 또는 의견을 입력하세요.")));
        }
        String role = user.roles().stream().filter(value -> Set.of("R01", "R02", "R04").contains(value)).findFirst().orElseThrow(ForbiddenException::new);
        if (mapper.allowedTransition(current.certificationStatus(), toStatus, role) == 0
                || mapper.updateStatus(achievementId, current.certificationStatus(), toStatus, user.userId()) == 0) {
            throw new ConflictException("INVALID_STATE_TRANSITION");
        }
        String reason = request.opinion() == null || request.opinion().isBlank() ? "교육영역 실적 상태전이" : request.opinion().trim();
        mapper.insertChangeHistory(String.valueOf(achievementId), "UPDATE", current.certificationStatus(), toStatus, user.userId(), reason, requestId);
        mapper.insertStatusHistory(achievementId, current.certificationStatus(), toStatus, user.userId(), reason);
        return mapper.findById(achievementId);
    }

    /**
     * Performs a physical-row-preserving deletion and records a correlated common change-history entry.
     * Final-evaluation data is immutable for every role, including the owning faculty member.
     */
    @Transactional
    public EducationAchievementDeleteResponse delete(Long achievementId, String deleteReason, CurrentUser user, String requestId) {
        requireRole(user);
        if (achievementId == null || achievementId <= 0) {
            throw new BusinessValidationException("삭제 대상이 올바르지 않습니다.", List.of(new ValidationError("achievementId", "실적을 선택하세요.")));
        }
        if (deleteReason == null || deleteReason.isBlank()) {
            throw new BusinessValidationException("삭제 사유를 입력하세요.", List.of(new ValidationError("deleteReason", "삭제 사유를 입력하세요.")));
        }
        EducationAchievementRow existing = mapper.findById(achievementId);
        if (existing == null) throw new NotFoundException("교육영역 실적을 찾을 수 없습니다.");
        if (user.roles().contains("R01") && !user.userId().equals(existing.facultyUserId())) throw new ForbiddenException();
        if ("EVALUATION_CONFIRMED".equals(existing.certificationStatus())) throw new ConfirmedDataLockedException();
        if (mapper.logicalDelete(achievementId, user.userId(), deleteReason.trim()) == 0) {
            throw new ConflictException("LOGICAL_DELETE_CONFLICT");
        }
        mapper.insertChangeHistory(String.valueOf(achievementId), "DELETE", existing.certificationStatus(), "DELETED",
                user.userId(), deleteReason.trim(), requestId);
        return new EducationAchievementDeleteResponse(achievementId, "Y", java.time.OffsetDateTime.now(), user.userId(),
                deleteReason.trim(), new EducationAchievementDeleteResponse.Audit("DELETE", existing.certificationStatus(),
                "DELETED", user.userId(), requestId));
    }

    private void validateDegreeDetails(EducationAchievementSaveRequest request) {
        if ("DEGREE_COMPLETION".equals(request.achievementType())
                && (request.degreeCompletionDetails() == null || request.degreeCompletionDetails().isEmpty())) {
            throw new BusinessValidationException("석·박사 배출 실적에는 지도학생 세부내역이 필요합니다.",
                    List.of(new ValidationError("degreeCompletionDetails", "지도학생 세부내역을 하나 이상 입력하세요.")));
        }
    }

    private void validateStudentGuidanceDetails(EducationAchievementSaveRequest request) {
        if (!"STUDENT_GUIDANCE".equals(request.achievementType())) return;
        if (request.studentGuidanceDetails() == null || request.studentGuidanceDetails().isEmpty()) {
            throw new BusinessValidationException("학생지도 실적에는 지도학생 세부내역이 필요합니다.",
                    List.of(new ValidationError("studentGuidanceDetails", "지도학생 세부내역을 하나 이상 입력하세요.")));
        }
        for (StudentGuidanceDetail detail : request.studentGuidanceDetails()) {
            if (detail.guidanceEndDate().isBefore(detail.guidanceStartDate())) {
                throw new BusinessValidationException("지도기간이 올바르지 않습니다.",
                        List.of(new ValidationError("studentGuidanceDetails", "지도 종료일은 시작일보다 빠를 수 없습니다.")));
            }
        }
    }

    private void requireRole(CurrentUser user) {
        if (user == null) throw new kr.ac.knue.commonfoundation.common.api.UnauthenticatedException();
        if (user.roles().stream().noneMatch(role -> Set.of("R01", "R02", "R04").contains(role))) throw new ForbiddenException();
    }

    private void validateType(String value) {
        if (value == null || !TYPES.contains(value.trim())) {
            throw new BusinessValidationException("실적유형이 올바르지 않습니다.", List.of(new ValidationError("achievementType", "지원하는 교육영역 실적유형을 선택하세요.")));
        }
    }

    private String actionToStatus(BusinessTransitionRequest request) {
        if (request == null || request.actionType() == null || request.actionType().isBlank()) {
            throw new BusinessValidationException("처리구분을 선택하세요.", List.of(new ValidationError("actionType", "처리구분을 선택하세요.")));
        }
        return switch (request.actionType().trim().toUpperCase()) {
            case "SUBMIT" -> "SUBMITTED";
            case "CONFIRM" -> "DEPARTMENT_CONFIRMED";
            case "REJECT" -> "DEPARTMENT_REJECTED";
            case "CERTIFY" -> "CERTIFIED";
            case "RETURN" -> "CERTIFICATION_RETURNED";
            default -> throw new BusinessValidationException("처리구분이 올바르지 않습니다.", List.of(new ValidationError("actionType", "지원하는 처리구분을 선택하세요.")));
        };
    }
}
