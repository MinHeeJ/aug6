package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enforces the common BASIC-79 mutation order and records lecture status transitions atomically.
 */
@Service
public class LectureAchievementFoundationService {
    static final String SCREEN_ID = "SCR-LECTURE-ACHIEVEMENT-MGMT";
    private static final Set<String> REJECTION_STATUSES = Set.of("DEPARTMENT_REJECTED", "CERTIFICATION_RETURNED");
    private final LectureAchievementFoundationMapper mapper;

    public LectureAchievementFoundationService(LectureAchievementFoundationMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Applies function permission, data scope, active input period, finalization lock, and management-item checks
     * before a later phase writes an achievement row. An out-of-period occurrence is returned as a warning.
     */
    @Transactional(readOnly = true)
    public List<String> validateMutation(EducationAchievementMutationContext context) {
        List<ValidationError> fields = validateContext(context);
        if (!fields.isEmpty()) throw new BusinessValidationException("교육영역 실적 입력값을 확인하세요.", fields);
        CurrentUser user = context.currentUser();
        requireFunctionPermission(user, context.functionType());
        if (context.achievementId() != null && mapper.countLectureScope(context.achievementId(), user.userId()) == 0) {
            throw new ForbiddenException();
        }
        if (mapper.countActiveInputPeriod(context.evaluationYear(), context.organizationCode()) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 현재 입력기간에는 저장할 수 없습니다.");
        }
        if (context.achievementId() != null) {
            LectureEvaluationAchievementState state = mapper.findLectureForUpdate(context.achievementId());
            if (state == null) throw new NotFoundException("강의실적 실적을 찾을 수 없습니다.");
            requireNotFinalized(state);
        }
        if (mapper.countActiveManagementItem(context.evaluationYear(), context.managementItemCode(), context.occurredDate()) == 0) {
            throw new BusinessValidationException("활성 관리항목 설정을 찾을 수 없습니다.", List.of(new ValidationError("managementItemCode", "활성 관리항목을 선택하세요.")));
        }
        return List.of("업적발생일이 평가대상 기간 밖인지 확인하세요.");
    }

    /**
     * Changes the lecture status only along the BASIC-79 state graph and persists its immutable history.
     */
    @Transactional
    public void transitionLecture(Long achievementId, EducationAchievementTransitionRequest request, CurrentUser user) {
        List<ValidationError> fields = validateTransition(achievementId, request, user);
        if (!fields.isEmpty()) throw new BusinessValidationException("상태 전이 요청을 확인하세요.", fields);
        LectureEvaluationAchievementState current = mapper.findLectureForUpdate(achievementId);
        if (current == null) throw new NotFoundException("강의실적 실적을 찾을 수 없습니다.");
        requireTransitionScope(current, user);
        requireNotFinalized(current);
        String nextStatus = normalized(request.nextStatus());
        if (!allowed(current.certificationStatus(), nextStatus, user)) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 허용되지 않은 상태 전이입니다.");
        }
        if (REJECTION_STATUSES.contains(nextStatus) && blank(request.reasonCode()) && blank(request.opinion())) {
            throw new BusinessValidationException("반려 사유 또는 의견을 입력하세요.", List.of(new ValidationError("reasonCode", "반려 사유 또는 의견을 입력하세요.")));
        }
        if (mapper.updateLectureStatus(achievementId, current.certificationStatus(), nextStatus, user.userId()) != 1) {
            throw new ConflictException("상태가 변경되어 다시 조회 후 처리하세요.");
        }
        mapper.insertStatusHistory("LECTURE", achievementId, current.certificationStatus(), nextStatus,
                transitionAction(nextStatus), trimToNull(request.reasonCode()), trimToNull(request.opinion()), user.userId());
        mapper.insertChangeHistory("lecture_achievements", String.valueOf(achievementId), "UPDATE",
                "certification_status", current.certificationStatus(), nextStatus, user.userId(),
                trimToNull(request.opinion()) == null ? "인증상태 전이" : trimToNull(request.opinion()));
    }

    private void requireFunctionPermission(CurrentUser user, String functionType) {
        for (String role : user.roles()) {
            if (mapper.countAllowedFunctionPermission(SCREEN_ID, role, functionType) > 0) return;
        }
        throw new ForbiddenException();
    }

    private void requireTransitionScope(LectureEvaluationAchievementState current, CurrentUser user) {
        requireFunctionPermission(user, "UPDATE");
        if (mapper.countLectureScope(current.achievementId(), user.userId()) == 0) throw new ForbiddenException();
    }

    private void requireNotFinalized(LectureEvaluationAchievementState state) {
        if ("EVALUATION_CONFIRMED".equals(state.certificationStatus())
                || mapper.countEvaluationConfirmedFinalization(state.ownerUserId(), state.evaluationYear()) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
    }

    private List<ValidationError> validateContext(EducationAchievementMutationContext context) {
        List<ValidationError> fields = new ArrayList<>();
        if (context == null || context.currentUser() == null) fields.add(new ValidationError("currentUser", "인증 사용자가 필요합니다."));
        if (context == null || !validYear(context.evaluationYear())) fields.add(new ValidationError("evaluationYear", "평가연도는 4자리여야 합니다."));
        if (context == null || blank(context.organizationCode())) fields.add(new ValidationError("organizationCode", "소속 조직이 필요합니다."));
        if (context == null || blank(context.managementItemCode())) fields.add(new ValidationError("managementItemCode", "관리항목을 선택하세요."));
        if (context == null || context.occurredDate() == null) fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        if (context == null || !Set.of("CREATE", "UPDATE", "DELETE").contains(context.functionType())) fields.add(new ValidationError("functionType", "변경 기능을 지정하세요."));
        return fields;
    }

    private List<ValidationError> validateTransition(Long achievementId, EducationAchievementTransitionRequest request, CurrentUser user) {
        List<ValidationError> fields = new ArrayList<>();
        if (achievementId == null || achievementId <= 0) fields.add(new ValidationError("achievementId", "실적을 선택하세요."));
        if (user == null) fields.add(new ValidationError("currentUser", "인증 사용자가 필요합니다."));
        if (request == null || blank(request.nextStatus())) fields.add(new ValidationError("nextStatus", "다음 인증상태를 선택하세요."));
        return fields;
    }

    private boolean allowed(String previousStatus, String nextStatus, CurrentUser user) {
        return switch (previousStatus) {
            case "DRAFTING" -> "SUBMITTED".equals(nextStatus) && hasRole(user, "R01");
            case "SUBMITTED" -> ("DEPARTMENT_CONFIRMED".equals(nextStatus) || "DEPARTMENT_REJECTED".equals(nextStatus)) && hasRole(user, "R02");
            case "DEPARTMENT_REJECTED", "CERTIFICATION_RETURNED" -> "SUBMITTED".equals(nextStatus) && hasRole(user, "R01");
            case "DEPARTMENT_CONFIRMED" -> ("CERTIFIED".equals(nextStatus) || "CERTIFICATION_RETURNED".equals(nextStatus)) && hasRole(user, "R04");
            case "CERTIFIED" -> "EVALUATION_CONFIRMED".equals(nextStatus) && hasRole(user, "R04");
            case "EVALUATION_CONFIRMED" -> "CERTIFIED".equals(nextStatus) && hasRole(user, "R04");
            default -> false;
        };
    }

    private boolean hasRole(CurrentUser user, String role) { return user.roles().contains(role); }
    private String transitionAction(String nextStatus) { return REJECTION_STATUSES.contains(nextStatus) ? "REJECT" : "TRANSITION"; }
    private String normalized(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private boolean validYear(String value) { return value != null && value.matches("^[0-9]{4}$"); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String trimToNull(String value) { return blank(value) ? null : value.trim(); }
}
