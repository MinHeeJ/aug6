package kr.ac.knue.commonfoundation.courseoperations;

import java.time.Year;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionMapper;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped reads and atomic header/detail saves without changing legacy guard policy. */
@Service
public class CourseOperationService {
    private static final String SCREEN = "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT";
    private final CourseOperationMapper mapper;
    private final EducationAchievementGuardMapper guards;
    private final FunctionPermissionMapper permissions;

    public CourseOperationService(
            CourseOperationMapper mapper,
            EducationAchievementGuardMapper guards,
            FunctionPermissionMapper permissions) {
        this.mapper = mapper;
        this.guards = guards;
        this.permissions = permissions;
    }

    /** List and count receive exactly the same normalized filters and union of permitted roles. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(CourseOperationSearch criteria, CurrentUser user) {
        List<String> roles = allowedRoles(user, "READ");
        return new CourseOperationSearchResponse(
                mapper.list(criteria, user.userId(), roles), criteria.page(), criteria.pageSize(),
                mapper.count(criteria, user.userId(), roles));
    }

    /** A known out-of-scope row is forbidden, not a shortcut around list ownership checks. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long id, CurrentUser user) {
        List<String> roles = allowedRoles(user, "READ");
        CourseOperationRow row = required(mapper.find(id));
        if (mapper.inScope(id, user.userId(), roles) == 0) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Inserts the header key first, then detail and histories in the same transaction. */
    @Transactional
    public CourseOperationSaveResult create(CourseOperationRequest body, CurrentUser user, String requestId) {
        allowedRoles(user, "CREATE");
        validate(body);
        String year = Year.from(body.achievementDate()).toString();
        checkMutation(null, user.userId(), year, requestId);
        List<String> organizations = mapper.organizations(user.userId(), body.achievementDate());
        if (organizations.size() != 1) {
            throw invalid("organizationCode", "발생일에 유효한 단일 소속이 필요합니다.");
        }
        validateItem(body);
        String attachments = attachments(body);
        Long id = mapper.insertHeader("CO-" + UUID.randomUUID(), user.userId(), organizations.get(0),
                year, body, attachments);
        mapper.insertDetail(id, body.performanceDetails());
        mapper.initialStatus(id, user.userId());
        audit(id, null, body, attachments, user, requestId);
        return result(required(mapper.find(id)));
    }

    /** Locks and revalidates before writing; teacher, organization and evaluation year stay immutable. */
    @Transactional
    public CourseOperationSaveResult update(
            Long id, CourseOperationRequest body, CurrentUser user, String requestId) {
        allowedRoles(user, "UPDATE");
        validate(body);
        CourseOperationRow previous = required(mapper.lock(id));
        if (!user.roles().contains("R09") && !previous.teacherUserId().equals(user.userId())) {
            throw new ForbiddenException();
        }
        checkMutation(previous, previous.teacherUserId(), previous.evaluationYear(), requestId);
        validateItem(body);
        String attachments = attachments(body);
        mapper.updateHeader(id, body, attachments, user.userId());
        mapper.updateDetail(id, body.performanceDetails());
        audit(id, previous, body, attachments, user, requestId);
        return result(required(mapper.find(id)));
    }

    private void checkMutation(CourseOperationRow row, Long teacher, String year, String requestId) {
        if ((row != null && "EVALUATION_CONFIRMED".equals(row.achievementStatus()))
                || guards.countEvaluationConfirmations(teacher, year) > 0) {
            throw conflict("CONFIRMED_DATA_LOCKED", "평가확정 실적은 수정할 수 없습니다.", requestId);
        }
        if (row != null && !List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED")
                .contains(row.achievementStatus())) {
            throw new kr.ac.knue.commonfoundation.common.api.ConflictException("작성 가능한 상태가 아닙니다.");
        }
        if (guards.countActiveInputPeriods(year, teacher) == 0) {
            throw conflict("PERIOD_NOT_ACTIVE", "활성 입력기간이 아니므로 저장할 수 없습니다.", requestId);
        }
    }

    private CourseOperationSaveResult result(CourseOperationRow row) {
        boolean warning = guards.countEvaluationDatePeriods(
                row.evaluationYear(), row.teacherUserId(), row.achievementDate()) == 0;
        return new CourseOperationSaveResult(row, warning,
                warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 완료되었습니다." : null);
    }

    private List<String> allowedRoles(CurrentUser user, String function) {
        if (user == null || user.roles() == null) {
            throw new ForbiddenException();
        }
        List<String> eligible = function.equals("READ") ? List.of("R01", "R02", "R04", "R09")
                : List.of("R01", "R09");
        List<String> roles = user.roles().stream().filter(eligible::contains).toList();
        java.util.ArrayList<String> permitted = new java.util.ArrayList<>();
        for (String role : roles) {
            FunctionPermissionRow setting = permissions.findByKey(SCREEN, role, function);
            if (setting != null && "DENY".equals(setting.permissionAllowed())) {
                throw new ForbiddenException();
            }
            if (role.equals("R09") || (setting != null && "ALLOW".equals(setting.permissionAllowed()))) {
                permitted.add(role);
            }
        }
        if (permitted.isEmpty()) {
            throw new ForbiddenException();
        }
        return List.copyOf(permitted);
    }

    private void validate(CourseOperationRequest body) {
        if (body == null || body.managementItemCode() == null || body.managementItemCode().isBlank()) {
            throw invalid("managementItemCode", "관리항목코드를 입력하세요.");
        }
        if (body.achievementDate() == null) {
            throw invalid("achievementDate", "업적발생일을 입력하세요.");
        }
        if (body.performanceDetails() == null || body.performanceDetails().isBlank()) {
            throw invalid("performanceDetails", "실적내역을 입력하세요.");
        }
    }

    private void validateItem(CourseOperationRequest body) {
        if (mapper.managementItems(body.managementItemCode()) != 1) {
            throw invalid("managementItemCode", "활성 교육 관리항목이 없거나 모호합니다.");
        }
    }

    private String attachments(CourseOperationRequest body) {
        // The storage owner supplies the verified shared adapter in a separate slice.
        // Fail closed instead of persisting an unowned or nonexistent opaque reference.
        if (body.attachmentIds() != null && !body.attachmentIds().isEmpty()) {
            throw invalid("attachmentIds", "첨부 소유권·파일 존재 검증 서비스가 연결되지 않았습니다.");
        }
        return "[]";
    }

    private void audit(Long id, CourseOperationRow old, CourseOperationRequest body, String attachments,
            CurrentUser user, String requestId) {
        changed(id, old, "management_item_code", old == null ? null : old.managementItemCode(),
                body.managementItemCode(), user, requestId);
        changed(id, old, "achievement_date", old == null ? null : old.achievementDate().toString(),
                body.achievementDate().toString(), user, requestId);
        changed(id, old, "performance_detail", old == null ? null : old.performanceDetails(),
                body.performanceDetails(), user, requestId);
        changed(id, old, "attachment_ref", old == null ? null : old.attachmentRef(),
                attachments, user, requestId);
    }

    private void changed(Long id, CourseOperationRow old, String field, String before, String after,
            CurrentUser user, String requestId) {
        if (old == null || !Objects.equals(before, after)) {
            mapper.history(id, old == null ? "CREATE" : "UPDATE", field, before, after, user.userId(), requestId);
        }
    }

    private CourseOperationRow required(CourseOperationRow row) {
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private EducationAchievementConflictException conflict(String code, String message, String requestId) {
        return new EducationAchievementConflictException(code, message, requestId);
    }
}
