package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped course reads and atomic header/detail/history writes, preserving evaluation identity. */
@Service
public class CourseOperationService {
    public static final String SCREEN_ID = "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT";
    private static final Set<String> EDITABLE = Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED");
    private final CourseOperationMapper mapper;
    private final EducationAchievementGuardMapper guard;
    private final FunctionPermissionService permissions;
    private final EducationAchievementStatusTransitionPolicy statusPolicy;
    private final ObjectMapper json;

    public CourseOperationService(
            CourseOperationMapper mapper,
            EducationAchievementGuardMapper guard,
            FunctionPermissionService permissions,
            EducationAchievementStatusTransitionPolicy statusPolicy,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.permissions = permissions;
        this.statusPolicy = statusPolicy;
        this.json = json;
    }

    /** List and total use identical dynamic filters and union-of-role scope predicates. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(CourseOperationSearchCriteria input, CurrentUser user) {
        requireRole(user, false);
        requireFunction(user, "READ");
        if (input.page() < 0 || !List.of(20, 50, 100).contains(input.pageSize())) {
            throw validation("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        CourseOperationSearchCriteria criteria = new CourseOperationSearchCriteria(
                input.page(), input.pageSize(), (long) input.page() * input.pageSize(),
                text(input.managementNo()), text(input.teacherName()),
                text(input.managementItemCode()), text(input.achievementStatus()));
        return new CourseOperationSearchResponse(
                mapper.list(criteria, user.userId(), user.roles()), criteria.page(), criteria.pageSize(),
                mapper.count(criteria, user.userId(), user.roles()), mapper.managementItems());
    }

    /** Detail applies the same boundary as list, rather than trusting a selected client row. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        requireFunction(user, "READ");
        CourseOperationRow row = find(id, false);
        if (mapper.countScope(id, user.userId(), user.roles()) == 0) throw new ForbiddenException();
        return row;
    }

    /** Inserts the header using its generated key before the detail and full audit snapshot. */
    @Transactional
    public CourseOperationSaveResult create(CourseOperationRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(body);
        String year = body.evaluationYear() == null
                ? String.valueOf(body.achievementDate().getYear()) : body.evaluationYear();
        boolean warning = guardMutation(year, user.userId(), body);
        requireFunction(user, "CREATE");
        if (body.achievementStatus() != null && body.achievementStatus() != EducationAchievementStatus.DRAFT) {
            throw conflict("INVALID_STATE_TRANSITION", "신규 실적은 작성중 상태로만 등록합니다.");
        }
        String organization = mapper.findOrganization(user.userId());
        if (organization == null) throw new ForbiddenException();
        String attachments = attachments(body, user, null);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("managementNo", "CO-" + UUID.randomUUID());
        header.put("teacherUserId", user.userId());
        header.put("organizationCode", organization);
        header.put("evaluationYear", year);
        header.put("managementItemCode", body.managementItemCode().trim());
        header.put("achievementDate", body.achievementDate());
        header.put("attachments", attachments);
        header.put("userId", user.userId());
        mapper.insertHeader(header);
        Long id = ((Number) header.get("achievementId")).longValue();
        mapper.insertDetail(id, body.performanceDetails().trim());
        CourseOperationRow saved = find(id, false);
        mapper.insertStatusHistory(id, null, "DRAFT", user.userId(), requestId);
        mapper.insertChangeHistory(id, "CREATE", null, snapshot(saved), user.userId(), requestId);
        return result(saved, warning);
    }

    /** Locks before validation; never moves the evaluation year or writes a partially valid update. */
    @Transactional
    public CourseOperationSaveResult update(Long id, CourseOperationRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        CourseOperationRow before = find(id, true);
        // Explicit request override: R09 bypasses role/scope/function admission, not lifecycle or period guards.
        if (!user.roles().contains("R09") && !before.teacherUserId().equals(user.userId())) {
            throw new ForbiddenException();
        }
        validate(body);
        boolean warning = guardMutation(before.evaluationYear(), before.teacherUserId(), body);
        if ("EVALUATION_CONFIRMED".equals(before.achievementStatus())) {
            throw conflict("CONFIRMED_DATA_LOCKED", "평가확정 실적은 수정할 수 없습니다.");
        }
        if (!EDITABLE.contains(before.achievementStatus())) {
            throw conflict("INVALID_STATE_TRANSITION", "작성중 또는 반려 실적만 수정할 수 있습니다.");
        }
        requireFunction(user, "UPDATE");
        if (body.evaluationYear() != null && !before.evaluationYear().equals(body.evaluationYear())) {
            throw validation("evaluationYear", "수정 시 평가연도를 변경할 수 없습니다.");
        }
        String status = nextStatus(before, body, user);
        String attachments = attachments(body, user, before);
        if (mapper.updateHeader(id, body, status, attachments, user.userId()) != 1) {
            throw conflict("INVALID_STATE_TRANSITION", "실적 상태가 변경되었습니다. 다시 조회하세요.");
        }
        mapper.updateDetail(id, body.performanceDetails().trim());
        CourseOperationRow saved = find(id, false);
        if (!status.equals(before.achievementStatus())) {
            mapper.insertStatusHistory(id, before.achievementStatus(), status, user.userId(), requestId);
        }
        mapper.insertChangeHistory(id, "UPDATE", snapshot(before), snapshot(saved), user.userId(), requestId);
        return result(saved, warning);
    }

    public static void requireRole(CurrentUser user, boolean write) {
        if (user == null || user.userId() == null) throw new UnauthenticatedException();
        if (user.roles() == null || user.roles().stream().noneMatch(
                role -> role.equals("R09")
                        || (write ? role.equals("R01") : List.of("R01", "R02", "R04").contains(role)))) {
            throw new ForbiddenException();
        }
    }

    private void requireFunction(CurrentUser user, String function) {
        if (user.roles().contains("R09")) return;
        List<String> allowed = function.equals("READ") ? List.of("R01", "R02", "R04") : List.of("R01");
        for (String role : user.roles()) {
            if (!allowed.contains(role)) continue;
            try {
                permissions.evaluate(new FunctionPermissionEvaluateRequest(SCREEN_ID, role, function, "DRAFT", null));
                return;
            } catch (ForbiddenException denied) {
                // Another admitted role may grant this operation; do not narrow multi-role reads.
            }
        }
        throw new ForbiddenException();
    }

    private boolean guardMutation(String year, Long owner, CourseOperationRequest body) {
        if (guard.countActiveInputPeriods(year, owner) == 0) {
            throw conflict("PERIOD_NOT_ACTIVE", "활성 입력기간이 아닙니다.");
        }
        if (guard.countEvaluationConfirmations(owner, year) > 0) {
            throw conflict("CONFIRMED_DATA_LOCKED", "평가확정 실적은 변경할 수 없습니다.");
        }
        if (mapper.countManagementItem(body.managementItemCode().trim()) == 0) {
            throw validation("managementItemCode", "활성 교육영역 관리항목을 선택하세요.");
        }
        return guard.countEvaluationDatePeriods(year, owner, body.achievementDate()) == 0;
    }

    private String nextStatus(CourseOperationRow before, CourseOperationRequest body, CurrentUser user) {
        EducationAchievementStatus next = body.achievementStatus();
        if (next == null || next.name().equals(before.achievementStatus())) return before.achievementStatus();
        if (next != EducationAchievementStatus.SUBMITTED) {
            throw conflict("INVALID_STATE_TRANSITION", "교원은 제출 또는 재제출만 요청할 수 있습니다.");
        }
        try {
            statusPolicy.transition(new EducationAchievementStatusTransitionRequest(
                    "COURSE_OPERATION", before.achievementId(),
                    EducationAchievementStatus.valueOf(before.achievementStatus()), next,
                    "SUBMIT", null, null, user.userId(), LocalDateTime.now()));
        } catch (ConflictException invalid) {
            throw conflict("INVALID_STATE_TRANSITION", "허용되지 않은 상태 전이입니다.");
        }
        return next.name();
    }

    private String attachments(CourseOperationRequest body, CurrentUser user, CourseOperationRow before) {
        List<String> tokens = body.attachmentIds() != null ? body.attachmentIds()
                : text(body.attachmentRef()) == null ? List.of() : List.of(body.attachmentRef().trim());
        if (tokens.size() > 20 || tokens.stream().distinct().count() != tokens.size()) {
            throw validation("attachmentIds", "첨부 참조는 중복 없이 최대 20개입니다.");
        }
        for (String token : tokens) {
            if (token == null || token.isBlank() || token.length() > 200) {
                throw validation("attachmentIds", "올바른 첨부 참조를 입력하세요.");
            }
            if (before != null && before.attachmentIds().contains(token)) continue;
            if (mapper.countAttachment(token, user.userId()) == 0) {
                throw validation("attachmentIds", "본인이 보관한 유효한 첨부 참조가 아닙니다.");
            }
        }
        if (tokens.isEmpty()) return null;
        String serialized = snapshot(tokens);
        if (serialized.length() > 300) throw validation("attachmentIds", "첨부 참조의 총 길이가 너무 깁니다.");
        return serialized;
    }

    private CourseOperationRow find(Long id, boolean lock) {
        CourseOperationRow row = mapper.find(id, lock);
        if (row == null) throw new NotFoundException("강좌 운영 실적을 찾을 수 없습니다.");
        return row;
    }

    private void validate(CourseOperationRequest body) {
        if (body == null) throw validation("body", "실적 정보를 입력하세요.");
        if (text(body.managementItemCode()) == null) throw validation("managementItemCode", "관리항목을 선택하세요.");
        if (body.achievementDate() == null) throw validation("achievementDate", "업적발생일을 입력하세요.");
        if (text(body.performanceDetails()) == null) throw validation("performanceDetails", "실적내역을 입력하세요.");
        if (body.evaluationYear() != null && !body.evaluationYear().matches("[0-9]{4}")) {
            throw validation("evaluationYear", "평가연도는 YYYY 형식입니다.");
        }
    }

    private String snapshot(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("실적 이력 직렬화 실패", invalid);
        }
    }

    private CourseOperationSaveResult result(CourseOperationRow row, boolean warning) {
        return new CourseOperationSaveResult(row, warning,
                warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 허용됩니다." : null);
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessValidationException validation(String field, String message) {
        return new BusinessValidationException("실적 입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }

    private CourseOperationConflictException conflict(String code, String message) {
        return new CourseOperationConflictException(code, message);
    }
}
