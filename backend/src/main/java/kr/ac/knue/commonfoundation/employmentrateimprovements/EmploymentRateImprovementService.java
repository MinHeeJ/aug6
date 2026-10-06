package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** FR-029 scope, validation and atomic ledger/detail/audit orchestration, without a second lifecycle system. */
@Service
public class EmploymentRateImprovementService {
    private static final String SCREEN = "SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardService guard;
    private final FunctionPermissionService permissions;
    private final ObjectMapper json;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper,
            EducationAchievementGuardService guard,
            FunctionPermissionService permissions,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.permissions = permissions;
        this.json = json;
    }

    /** List and count use the same scope union and optional predicates. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            EmploymentRateImprovementSearchCriteria criteria, CurrentUser user) {
        requireRole(user, false);
        requireFunction(user, "READ");
        if (criteria.page() < 0 || !List.of(20, 50, 100).contains(criteria.pageSize())) {
            throw invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        var safe = new EmploymentRateImprovementSearchCriteria(
                criteria.page(), criteria.pageSize(), (long) criteria.page() * criteria.pageSize(),
                trim(criteria.managementNo()), trim(criteria.teacherName()),
                trim(criteria.managementItemCode()), trim(criteria.certificationStatus()));
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(safe, user.userId(), user.roles()), safe.page(), safe.pageSize(),
                mapper.count(safe, user.userId(), user.roles()), mapper.availableManagementItems());
    }

    /** Detail enforces the same scope as list, independently of UI and menu permissions. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        requireFunction(user, "READ");
        var row = existing(id, false);
        if (mapper.visible(id, user.userId(), user.roles()) == 0) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Creates the header with a generated key before inserting detail and immutable history. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(body);
        body = normalize(body);
        String year = Year.from(body.achievementDate()).toString();
        OccurredDateValidation warning = guard.validateMutation(
                user, new EducationAchievementMutationContext(user.userId(), year, body.achievementDate()));
        requireFunction(user, "CREATE");
        validateManagementItem(body, year);
        validateAttachments(body, null);
        List<String> organizations = mapper.organizations(user.userId());
        if (organizations.size() != 1) {
            throw invalid("organizationCode", "대상 교원의 유효 소속이 하나로 결정되어야 합니다.");
        }
        Map<String, Object> values = new HashMap<>();
        values.put("managementNo", "ERI-" + UUID.randomUUID());
        values.put("userId", user.userId());
        values.put("organizationCode", organizations.get(0));
        values.put("year", year);
        values.put("body", body);
        Map<String, Object> detail = new HashMap<>();
        detail.put("specialLectureStartDate", body.specialLectureStartDate());
        detail.put("specialLectureEndDate", body.specialLectureEndDate());
        detail.put("mockExamQuestionPeriod", body.mockExamQuestionPeriod());
        values.put("detail", serialize(detail));
        mapper.insertHeader(values);
        Long id = ((Number) values.get("id")).longValue();
        mapper.insertDetail(id, body);
        mapper.statusHistory(id, user.userId());
        var saved = existing(id, false);
        mapper.changeHistory(id, "CREATE", null, serialize(saved), user.userId(), requestId);
        return new EmploymentRateImprovementSaveResult(saved, warning.warning(), warning.message());
    }

    /** Locks the source and preserves its year, owner, attachments and lifecycle; failure rolls back all writes. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EmploymentRateImprovementSaveResult update(
            Long id, EmploymentRateImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(body);
        body = normalize(body);
        var before = existing(id, true);
        if (!user.userId().equals(before.teacherUserId())) {
            throw new ForbiddenException();
        }
        // Shared guard deliberately checks input period before evaluation finalization.
        OccurredDateValidation warning = guard.validateMutation(user, new EducationAchievementMutationContext(
                before.teacherUserId(), before.evaluationYear(), body.achievementDate()));
        if ("EVALUATION_CONFIRMED".equals(before.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        if (!"DRAFT".equals(before.achievementStatus())) {
            throw new ConflictException("STATUS_NOT_EDITABLE: 작성중 실적만 수정할 수 있습니다.");
        }
        requireFunction(user, "UPDATE");
        validateManagementItem(body, before.evaluationYear());
        validateAttachments(body, before);
        if (mapper.updateHeader(id, body, user.userId()) != 1) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 실적 상태가 변경되었습니다.");
        }
        mapper.updateDetail(id, body);
        var saved = existing(id, false);
        mapper.changeHistory(id, "UPDATE", serialize(before), serialize(saved), user.userId(), requestId);
        return new EmploymentRateImprovementSaveResult(saved, warning.warning(), warning.message());
    }

    private EmploymentRateImprovementRow existing(Long id, boolean lock) {
        var row = lock ? mapper.lock(id) : mapper.find(id);
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireRole(CurrentUser user, boolean write) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        List<String> allowed = write ? List.of("R01") : List.of("R01", "R02", "R04");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireFunction(CurrentUser user, String function) {
        for (String role : user.roles()) {
            if (!List.of("R01", "R02", "R04").contains(role)
                    || (!"READ".equals(function) && !"R01".equals(role))) {
                continue;
            }
            try {
                permissions.evaluate(new FunctionPermissionEvaluateRequest(SCREEN, role, function, "DRAFT", null));
                return;
            } catch (ForbiddenException denied) {
                // A denied role must not narrow another allowed role's function scope.
            }
        }
        throw new ForbiddenException();
    }

    private void validate(EmploymentRateImprovementRequest body) {
        if (body == null || trim(body.managementItemCode()) == null) {
            throw invalid("managementItemCode", "관리항목을 입력하세요.");
        }
        if (body.achievementDate() == null) {
            throw invalid("achievementDate", "업적발생일을 입력하세요.");
        }
        if ((body.specialLectureStartDate() == null) != (body.specialLectureEndDate() == null)
                || (body.specialLectureStartDate() != null
                && body.specialLectureEndDate().isBefore(body.specialLectureStartDate()))) {
            throw invalid("specialLectureEndDate", "특강 시작일과 종료일을 함께 입력하고 순서를 확인하세요.");
        }
    }

    private void validateManagementItem(EmploymentRateImprovementRequest body, String year) {
        var items = mapper.managementItems(body.managementItemCode().trim(), year);
        if (items.size() != 1 || !"Y".equals(items.get(0).get("teacherEditableYn"))) {
            throw invalid("managementItemCode", "평가연도의 활성·확정·입력가능 관리항목을 하나로 지정하세요.");
        }
        var item = items.get(0);
        String value = trim(body.mockExamQuestionPeriod());
        String type = String.valueOf(item.get("dataType"));
        boolean periodValue = ("TEXT".equals(type) || "DATE".equals(type))
                && body.specialLectureStartDate() != null;
        if ("Y".equals(item.get("requiredYn")) && value == null && !periodValue) {
            throw invalid("mockExamQuestionPeriod", "관리항목의 필수 실적값을 입력하세요.");
        }
        try {
            if (value != null && "NUMBER".equals(type)) {
                new java.math.BigDecimal(value);
            } else if (value != null && "DATE".equals(type)) {
                LocalDate.parse(value);
            } else if (value != null && "BOOLEAN".equals(type)
                    && !List.of("true", "false").contains(value)) {
                throw new IllegalArgumentException();
            } else if (List.of("CODE", "FILE").contains(type)) {
                throw invalid("managementItemCode", "코드·파일 관리항목은 공통 입력 계약 연결이 필요합니다.");
            }
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            throw invalid("mockExamQuestionPeriod", "관리항목 데이터 형식을 확인하세요.");
        }
    }

    private void validateAttachments(EmploymentRateImprovementRequest body, EmploymentRateImprovementRow before) {
        // The parallel Excel step owns FileStoragePort. Do not pretend an arbitrary token is a stored file.
        if (body.attachmentIds() != null
                && (before == null ? !body.attachmentIds().isEmpty()
                : !body.attachmentIds().equals(before.attachmentIds()))) {
            throw invalid("attachmentIds", "첨부 저장·소유권 계약 연결 전에는 신규 첨부 참조를 저장할 수 없습니다.");
        }
    }

    private EmploymentRateImprovementRequest normalize(EmploymentRateImprovementRequest body) {
        return new EmploymentRateImprovementRequest(
                body.managementItemCode().trim(), body.achievementDate(),
                body.specialLectureStartDate(), body.specialLectureEndDate(),
                trim(body.mockExamQuestionPeriod()), body.attachmentIds());
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw invalid("body", "실적 정보를 변환하지 못했습니다.");
        }
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
