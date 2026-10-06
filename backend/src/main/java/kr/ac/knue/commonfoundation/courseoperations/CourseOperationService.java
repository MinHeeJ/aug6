package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
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

/** Owns FR-030 scope checks and atomic header/detail/full-snapshot audit transactions. */
@Service
public class CourseOperationService {
    private static final String SCREEN = "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT";
    private static final Set<String> READ_ROLES = Set.of("R01", "R02", "R04");
    private static final Set<String> EDITABLE = Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED");
    private final CourseOperationMapper mapper;
    private final EducationAchievementGuardService guard;
    private final FunctionPermissionService permissions;
    private final ObjectMapper json;

    public CourseOperationService(
            CourseOperationMapper mapper,
            EducationAchievementGuardService guard,
            FunctionPermissionService permissions,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.permissions = permissions;
        this.json = json;
    }

    /** Lists union-scoped results and totals with identical dynamic filters. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(CourseOperationSearchCriteria criteria, CurrentUser user) {
        requireRole(user, READ_ROLES);
        requireFunction(user, "READ");
        if (criteria.page() < 0 || !Set.of(20, 50, 100).contains(criteria.pageSize())) {
            invalid("pageSize", "페이지와 표시 건수를 확인하세요.");
        }
        CourseOperationSearchCriteria normalized = new CourseOperationSearchCriteria(
                criteria.page(), criteria.pageSize(), (long) criteria.page() * criteria.pageSize(),
                normalize(criteria.managementNo()), normalize(criteria.teacherName()),
                normalize(criteria.managementItemCode()), normalize(criteria.achievementStatus()));
        return new CourseOperationSearchResponse(
                mapper.list(normalized, user.userId(), user.roles()),
                normalized.page(), normalized.pageSize(),
                mapper.count(normalized, user.userId(), user.roles()));
    }

    /** Applies the same ownership boundary to direct IDs as to the scoped list. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long id, CurrentUser user) {
        requireRole(user, READ_ROLES);
        requireFunction(user, "READ");
        CourseOperationRow row = required(mapper.find(id));
        if (mapper.canRead(id, user.userId(), user.roles()) == 0) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Create-only: the generated header key is used before any join-based readback. */
    @Transactional
    public CourseOperationSaveResult create(CourseOperationRequest request, CurrentUser user, String requestId) {
        requireRole(user, Set.of("R01"));
        requireFunction(user, "CREATE");
        validate(request);
        String year = String.valueOf(request.achievementDate().getYear());
        lockContext(user.userId(), year);
        OccurredDateValidation warning = guard.validateMutation(
                user, new EducationAchievementMutationContext(user.userId(), year, request.achievementDate()));
        validateManagement(request, year);
        List<String> attachments = validatedAttachments(request.attachmentIds(), null);
        String organization = mapper.organization(user.userId());
        if (organization == null || organization.isBlank()) {
            invalid("managementItemCode", "교원의 활성 소속 정보가 없어 등록할 수 없습니다.");
        }
        Map<String, Object> command = new HashMap<>();
        command.put("managementNo", "CO-" + UUID.randomUUID());
        command.put("userId", user.userId());
        command.put("organizationCode", organization);
        command.put("evaluationYear", year);
        command.put("code", request.managementItemCode().trim());
        command.put("date", request.achievementDate());
        command.put("detailJson", serialize(Map.of("performanceDetails", request.performanceDetails())));
        command.put("attachmentsJson", serialize(attachments));
        requireWritten(mapper.insertHeader(command));
        Object key = command.get("achievementId");
        if (!(key instanceof Number generated)) {
            throw new IllegalStateException("저장 식별자를 가져오지 못했습니다.");
        }
        Long id = generated.longValue();
        requireWritten(mapper.insertDetail(id, request.performanceDetails()));
        requireWritten(mapper.insertInitialStatus(id, user.userId()));
        requireWritten(mapper.insertHistory(id, "CREATE", null, requiredSnapshot(id), user.userId(), requestId));
        return result(required(mapper.find(id)), warning);
    }

    /** Update-only: preserves the original evaluation year and rejects all non-editable lifecycle states. */
    @Transactional
    public CourseOperationSaveResult update(
            Long id, CourseOperationRequest request, CurrentUser user, String requestId) {
        requireRole(user, Set.of("R01"));
        requireFunction(user, "UPDATE");
        validate(request);
        CourseOperationRow existing = required(mapper.lock(id));
        if (!user.userId().equals(existing.teacherUserId())) {
            throw new ForbiddenException();
        }
        lockContext(existing.teacherUserId(), existing.evaluationYear());
        OccurredDateValidation warning = guard.validateMutation(user, new EducationAchievementMutationContext(
                existing.teacherUserId(), existing.evaluationYear(), request.achievementDate()));
        // Period and finalization guards precede row-state checks, preserving the shared error priority.
        if ("EVALUATION_CONFIRMED".equals(existing.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        if (!EDITABLE.contains(existing.achievementStatus())) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 현재 상태에서는 수정할 수 없습니다.");
        }
        validateManagement(request, existing.evaluationYear());
        List<String> attachments = validatedAttachments(request.attachmentIds(), existing);
        String before = requiredSnapshot(id);
        requireWritten(mapper.updateHeader(
                id, request.managementItemCode().trim(), request.achievementDate(),
                serialize(Map.of("performanceDetails", request.performanceDetails())), serialize(attachments),
                user.userId()));
        requireWritten(mapper.updateDetail(id, request.performanceDetails()));
        requireWritten(mapper.insertHistory(id, "UPDATE", before, requiredSnapshot(id), user.userId(), requestId));
        return result(required(mapper.find(id)), warning);
    }

    private void lockContext(Long owner, String year) {
        // Owner FOR UPDATE also serializes new finalization FK insertions; existing finalizations are share-locked.
        if (mapper.lockOwner(owner) == null) {
            throw new NotFoundException("실적 대상자를 찾을 수 없습니다.");
        }
        if (mapper.lockInputPeriods(owner, year).isEmpty()) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        mapper.lockFinalizations(owner, year);
    }

    private void requireRole(CurrentUser user, Set<String> roles) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || user.roles().stream().noneMatch(roles::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireFunction(CurrentUser user, String function) {
        for (String role : user.roles()) {
            if (!("READ".equals(function) ? READ_ROLES.contains(role) : "R01".equals(role))) {
                continue;
            }
            try {
                // Lifecycle locking belongs to the ordered domain guard, not the permission evaluator's 403.
                if (permissions.evaluate(new FunctionPermissionEvaluateRequest(
                        SCREEN, role, function, "DRAFT", null)).allowed()) {
                    return;
                }
            } catch (ForbiddenException denied) {
                // Another approved role can grant this read function; do not narrow multi-role scope.
            }
        }
        throw new ForbiddenException();
    }

    private void validate(CourseOperationRequest request) {
        if (request == null) {
            invalid("body", "실적 정보를 입력하세요.");
        }
        if (normalize(request.managementItemCode()) == null || request.managementItemCode().trim().length() > 50) {
            invalid("managementItemCode", "유효한 관리항목을 입력하세요.");
        }
        if (request.achievementDate() == null) {
            invalid("achievementDate", "업적발생일을 입력하세요.");
        }
        if (normalize(request.performanceDetails()) == null) {
            invalid("performanceDetails", "실적내역을 입력하세요.");
        }
    }

    private void validateManagement(CourseOperationRequest request, String year) {
        List<CourseOperationManagementRule> rules = mapper.managementRules(request.managementItemCode().trim(), year);
        if (rules.size() != 1) {
            invalid("managementItemCode", "평가연도의 활성 교육 관리항목을 하나로 식별할 수 없습니다.");
        }
        CourseOperationManagementRule rule = rules.get(0);
        if (!"Y".equals(rule.teacherEditableYn())) {
            invalid("managementItemCode", "교원이 입력할 수 없는 관리항목입니다.");
        }
        if ("Y".equals(rule.requiredYn()) && normalize(request.performanceDetails()) == null) {
            invalid("performanceDetails", "필수 관리항목 값을 입력하세요.");
        }
        try {
            switch (rule.dataType()) {
                case "TEXT" -> { }
                case "NUMBER" -> new BigDecimal(request.performanceDetails().trim());
                case "DATE" -> LocalDate.parse(request.performanceDetails().trim());
                case "BOOLEAN" -> {
                    if (!Set.of("true", "false").contains(request.performanceDetails().trim())) {
                        invalid("performanceDetails", "참 또는 거짓 값이 필요합니다.");
                    }
                }
                default -> invalid("performanceDetails", "이 관리항목의 코드·파일 입력 매핑이 설정되지 않았습니다.");
            }
        } catch (NumberFormatException | java.time.format.DateTimeParseException exception) {
            invalid("performanceDetails", "관리항목의 데이터 형식과 일치하지 않습니다.");
        }
    }

    private List<String> validatedAttachments(List<String> requested, CourseOperationRow existing) {
        List<String> values = requested == null
                ? (existing == null ? List.of() : existing.attachmentIds()) : requested;
        // No attachment storage contract is merged yet. Fail closed on new references; preserve owned existing ones.
        // This operation never writes files, so there is no new-file rollback compensation to perform here.
        if (values.stream().anyMatch(value -> value == null || value.isBlank())
                || values.stream().distinct().count() != values.size()
                || (!values.isEmpty() && (existing == null || !existing.attachmentIds().containsAll(values)))) {
            invalid("attachmentIds", "새 첨부 참조는 파일 저장·소유권 검증 연동 후 사용할 수 있습니다.");
        }
        return List.copyOf(values);
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("실적 입력값을 변환하지 못했습니다.", exception);
        }
    }

    private String requiredSnapshot(Long id) {
        String snapshot = mapper.snapshot(id);
        if (snapshot == null) {
            throw new IllegalStateException("실적 변경 이력을 생성하지 못했습니다.");
        }
        return snapshot;
    }

    private CourseOperationRow required(CourseOperationRow row) {
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireWritten(int count) {
        if (count != 1) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 실적 변경 조건이 달라졌습니다.");
        }
    }

    private CourseOperationSaveResult result(CourseOperationRow row, OccurredDateValidation warning) {
        return new CourseOperationSaveResult(row, warning.warning(), warning.message());
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void invalid(String field, String message) {
        throw new BusinessValidationException("강좌 개설·운영 실적 입력값을 확인하세요.",
                List.of(new ValidationError(field, message)));
    }
}
