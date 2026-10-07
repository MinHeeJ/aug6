package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped FR-030 reads and atomic header/detail/history writes using the existing education guards. */
@Service
public class CourseOperationService {
    private static final Set<String> EDITABLE = Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED");
    private final CourseOperationMapper mapper;
    private final EducationAchievementGuardService guard;
    private final EducationAchievementGuardMapper scope;
    private final ObjectMapper json;

    public CourseOperationService(
            CourseOperationMapper mapper,
            EducationAchievementGuardService guard,
            EducationAchievementGuardMapper scope,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.scope = scope;
        this.json = json;
    }

    /** List/count receive identical normalized criteria and the caller's complete role set. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(CourseOperationSearchCriteria criteria, CurrentUser user) {
        requireRole(user, false);
        if (criteria.page() < 0 || !List.of(20, 50, 100).contains(criteria.pageSize())) {
            throw invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        CourseOperationSearchCriteria normalized = new CourseOperationSearchCriteria(
                criteria.page(), criteria.pageSize(), trim(criteria.managementNo()), trim(criteria.teacherName()),
                trim(criteria.managementItemCode()), trim(criteria.achievementStatus()));
        return new CourseOperationSearchResponse(
                mapper.list(normalized, user.userId(), user.roles(), (long) normalized.page() * normalized.pageSize()),
                normalized.page(), normalized.pageSize(), mapper.count(normalized, user.userId(), user.roles()));
    }

    /** A detail read applies the same union of owner/department/certification scopes as a list. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        CourseOperationRow row = existing(id, false);
        requireScope(user, row.teacherUserId());
        return row;
    }

    /** Creates DRAFT after guards, using the header's generated key before writing its detail. */
    @Transactional
    public CourseOperationSaveResult create(CourseOperationRequest request, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(request);
        String year = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation warning = guard.validateMutation(
                user, new EducationAchievementMutationContext(user.userId(), year, request.achievementDate()));
        validateManagementItem(request.managementItemCode());
        String organization = mapper.organization(user.userId());
        if (organization == null) {
            throw new ForbiddenException();
        }
        Map<String, Object> command = command(request, user, requestId);
        command.put("managementNo", "CO-" + UUID.randomUUID());
        command.put("evaluationYear", year);
        command.put("organizationCode", organization);
        mapper.insertHeader(command);
        mapper.insertDetail(command);
        mapper.insertStatusHistory(command);
        CourseOperationRow saved = existing(((Number) command.get("achievementId")).longValue(), false);
        audit(command, null, saved);
        return new CourseOperationSaveResult(saved, warning.warning(), warning.message());
    }

    /** Locks the header, preserves its evaluation year, and rejects noneditable or other-owned rows before writes. */
    @Transactional
    public CourseOperationSaveResult update(
            Long id, CourseOperationRequest request, CurrentUser user, String requestId) {
        requireRole(user, true);
        CourseOperationRow before = existing(id, true);
        if (!Objects.equals(before.teacherUserId(), user.userId())) {
            throw new ForbiddenException();
        }
        if (!EDITABLE.contains(before.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 제출·인증·평가확정 실적은 수정할 수 없습니다.");
        }
        validate(request);
        OccurredDateValidation warning = guard.validateMutation(user, new EducationAchievementMutationContext(
                before.teacherUserId(), before.evaluationYear(), request.achievementDate()));
        validateManagementItem(request.managementItemCode());
        Map<String, Object> command = command(request, user, requestId);
        command.put("achievementId", id);
        if (mapper.updateHeader(command) != 1 || mapper.updateDetail(command) != 1) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 실적 상태가 변경되었습니다.");
        }
        CourseOperationRow saved = existing(id, false);
        audit(command, before, saved);
        return new CourseOperationSaveResult(saved, warning.warning(), warning.message());
    }

    private CourseOperationRow existing(Long id, boolean lock) {
        CourseOperationRow row = mapper.find(id, lock);
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireScope(CurrentUser user, Long owner) {
        // Independent branches are intentional: multiple roles grant the union, not first-role precedence.
        if (user.roles().contains("R01") && Objects.equals(user.userId(), owner)) {
            return;
        }
        if (user.roles().contains("R02") && scope.countSharedActiveOrganization(user.userId(), owner) > 0) {
            return;
        }
        if (user.roles().contains("R04") && scope.countCertificationScope(user.userId(), owner) > 0) {
            return;
        }
        throw new ForbiddenException();
    }

    static void requireRole(CurrentUser user, boolean write) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || (write ? !user.roles().contains("R01")
                : user.roles().stream().noneMatch(Set.of("R01", "R02", "R04")::contains))) {
            throw new ForbiddenException();
        }
    }

    private void validateManagementItem(String code) {
        if (mapper.countManagementItem(code.trim()) != 1) {
            throw invalid("managementItemCode", "사용 중인 유일한 관리항목을 선택하세요.");
        }
    }

    private void validate(CourseOperationRequest request) {
        if (request == null) {
            throw invalid("body", "실적 정보를 입력하세요.");
        }
        if (trim(request.managementItemCode()) == null || request.managementItemCode().trim().length() > 50) {
            throw invalid("managementItemCode", "관리항목을 입력하세요.");
        }
        if (request.achievementDate() == null) {
            throw invalid("achievementDate", "업적발생일을 입력하세요.");
        }
        if (trim(request.performanceDetails()) == null) {
            throw invalid("performanceDetails", "실적내역을 입력하세요.");
        }
    }

    private Map<String, Object> command(CourseOperationRequest request, CurrentUser user, String requestId) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("teacherUserId", user.userId());
        command.put("managementItemCode", request.managementItemCode().trim());
        command.put("achievementDate", request.achievementDate());
        command.put("performanceDetails", request.performanceDetails().trim());
        String attachment = trim(request.attachmentRef());
        if (request.attachmentIds() != null && !request.attachmentIds().isEmpty()) {
            if (attachment != null) {
                throw invalid("attachmentIds", "첨부 참조와 첨부 목록을 동시에 입력할 수 없습니다.");
            }
            attachment = serialize(request.attachmentIds());
        }
        if (attachment != null && attachment.length() > 300) {
            throw invalid("attachmentIds", "첨부 참조의 전체 길이는 300자 이하여야 합니다.");
        }
        command.put("attachmentRef", attachment);
        command.put("requestId", requestId);
        return command;
    }

    private void audit(Map<String, Object> command, CourseOperationRow before, CourseOperationRow saved) {
        // Whole-row snapshots retain all changed fields, not just the visible performance text.
        command.put("changeType", before == null ? "CREATE" : "UPDATE");
        command.put("beforeValue", before == null ? null : serialize(before));
        command.put("afterValue", serialize(saved));
        mapper.insertChangeHistory(command);
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw invalid("body", "실적 정보를 직렬화할 수 없습니다.");
        }
    }

    private static BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
