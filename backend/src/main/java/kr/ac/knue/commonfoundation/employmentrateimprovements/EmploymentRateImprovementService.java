package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.common.storage.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped reads and atomic header/detail/audit writes without changing legacy education behavior. */
@Service
public class EmploymentRateImprovementService {
    private static final String SCREEN = "SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardMapper guard;
    private final FunctionPermissionService functions;
    private final FileStoragePort storage;
    private final ObjectMapper json;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper,
            EducationAchievementGuardMapper guard,
            FunctionPermissionService functions,
            FileStoragePort storage,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.functions = functions;
        this.storage = storage;
        this.json = json;
    }

    /** List/count use identical dynamic scope and filters; options come from operational settings. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            EmploymentRateImprovementSearchCriteria criteria, CurrentUser user) {
        requireRole(user, false);
        if (criteria.page() < 0 || !List.of(20, 50, 100).contains(criteria.pageSize())) {
            invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        permission(user, "READ", "DRAFT");
        String year = criteria.evaluationYear() == null
                ? String.valueOf(LocalDate.now().getYear()) : criteria.evaluationYear();
        if (!year.matches("[0-9]{4}")) invalid("evaluationYear", "평가연도는 YYYY 형식입니다.");
        List<EmploymentRateImprovementManagementItem> options = mapper.organizations(user.userId()).stream()
                .flatMap(org -> mapper.managementItems(year, org).stream()).distinct().toList();
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(criteria, user.userId(), user.roles()), criteria.page(), criteria.pageSize(),
                mapper.count(criteria, user.userId(), user.roles()), options);
    }

    /** Enforces the same union of owner, department and certification scopes as list. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        EmploymentRateImprovementRow row = find(id, false);
        requireScope(user, row.teacherUserId());
        permission(user, "READ", row.achievementStatus());
        return row;
    }

    /** Collection POST always inserts a new DRAFT row using the generated header key before detail insertion. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(body);
        mapper.lockOwner(user.userId());
        String year = String.valueOf(body.achievementDate().getYear());
        guardMutation(user.userId(), year, null);
        permission(user, "CREATE", "DRAFT");
        List<String> organizations = mapper.organizations(user.userId());
        if (organizations.size() != 1) invalid("organizationCode", "유일한 활성 소속이 필요합니다.");
        String organization = organizations.get(0);
        validateItem(body.managementItemCode(), year, organization);
        validateFiles(body.attachmentIds(), user.userId());
        Map<String, Object> values = values(body, user.userId(), requestId);
        values.put("teacherUserId", user.userId());
        values.put("organizationCode", organization);
        values.put("evaluationYear", year);
        values.put("managementNo", "ERI-" + UUID.randomUUID());
        mapper.insertHeader(values);
        Long id = ((Number) Objects.requireNonNull(values.get("achievementId"))).longValue();
        mapper.insertDetail(id, body);
        mapper.insertStatus(id, user.userId(), requestId);
        EmploymentRateImprovementRow saved = find(id, false);
        mapper.insertHistory(id, "CREATE", null, serialize(saved), user.userId(), requestId);
        return result(saved);
    }

    /** Locks existing data, preserves its owner/year, and snapshots every old/new header and detail value. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(
            Long id, EmploymentRateImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        EmploymentRateImprovementRow old = find(id, true);
        if (!isAdmin(user) && !Objects.equals(old.teacherUserId(), user.userId())) throw new ForbiddenException();
        mapper.lockOwner(old.teacherUserId());
        guardMutation(old.teacherUserId(), old.evaluationYear(), old.achievementStatus());
        permission(user, "UPDATE", old.achievementStatus());
        validate(body);
        validateItem(body.managementItemCode(), old.evaluationYear(), old.organizationCode());
        validateFiles(body.attachmentIds(), old.teacherUserId());
        Map<String, Object> values = values(body, user.userId(), requestId);
        values.put("achievementId", id);
        if (mapper.updateHeader(values) != 1) throw new ConflictException("DATA_NOT_EDITABLE: 수정 불가 상태입니다.");
        mapper.updateDetail(id, body);
        EmploymentRateImprovementRow saved = find(id, false);
        mapper.insertHistory(id, "UPDATE", serialize(old), serialize(saved), user.userId(), requestId);
        return result(saved);
    }

    /** The finalization conflict precedes function evaluation so it cannot become a generic 403. */
    private void guardMutation(Long owner, String year, String status) {
        if (guard.countActiveInputPeriods(year, owner) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        if ("EVALUATION_CONFIRMED".equals(status) || !mapper.lockFinalizations(owner, year).isEmpty()
                || guard.countEvaluationConfirmations(owner, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        if (status != null && !List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED").contains(status)) {
            throw new ConflictException("DATA_NOT_EDITABLE: 수정 불가 상태입니다.");
        }
    }

    private EmploymentRateImprovementSaveResult result(EmploymentRateImprovementRow row) {
        boolean warning = guard.countEvaluationDatePeriods(
                row.evaluationYear(), row.teacherUserId(), row.achievementDate()) == 0;
        return new EmploymentRateImprovementSaveResult(row, warning,
                warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 완료되었습니다." : null);
    }

    private void validate(EmploymentRateImprovementRequest body) {
        if (body == null) invalid("body", "실적을 입력하세요.");
        if (body.managementItemCode() == null || body.managementItemCode().isBlank()) {
            invalid("managementItemCode", "관리항목을 선택하세요.");
        }
        if (body.achievementDate() == null) invalid("achievementDate", "업적발생일을 입력하세요.");
        LocalDate start = body.specialLectureStartDate();
        LocalDate end = body.specialLectureEndDate();
        if ((start == null) != (end == null) || (start != null && end.isBefore(start))) {
            invalid("specialLectureEndDate", "특강 시작일과 종료일을 함께 입력하고 기간 순서를 확인하세요.");
        }
    }

    private void validateItem(String code, String year, String organization) {
        if (mapper.managementItems(year, organization).stream()
                .noneMatch(item -> item.managementItemCode().equals(code.trim()))) {
            invalid("managementItemCode", "활성 교육영역의 연도·소속별 관리항목이 아니거나 모호한 코드입니다.");
        }
    }

    private void validateFiles(List<String> ids, Long owner) {
        if (ids == null) return;
        for (String id : ids) {
            try {
                if (id == null || storage.find(id, owner) == null) invalid("attachmentIds", "본인 소유의 실제 파일만 참조하세요.");
                storage.read(id, owner);
            } catch (IOException | IllegalArgumentException exception) {
                invalid("attachmentIds", "첨부 파일을 확인할 수 없습니다.");
            }
        }
    }

    private Map<String, Object> values(EmploymentRateImprovementRequest body, Long actor, String requestId) {
        Map<String, Object> values = new HashMap<>();
        values.put("managementItemCode", body.managementItemCode().trim());
        values.put("achievementDate", body.achievementDate());
        values.put("attachmentJson", serialize(body.attachmentIds() == null ? List.of() : body.attachmentIds()));
        values.put("userId", actor);
        values.put("requestId", requestId);
        return values;
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize achievement snapshot", exception);
        }
    }

    private EmploymentRateImprovementRow find(Long id, boolean lock) {
        EmploymentRateImprovementRow row = mapper.find(id, lock);
        if (row == null) throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        return row;
    }

    private void requireScope(CurrentUser user, Long owner) {
        if (isAdmin(user)
                || (user.roles().contains("R01") && Objects.equals(owner, user.userId()))
                || (user.roles().contains("R02") && guard.countSharedActiveOrganization(user.userId(), owner) > 0)
                || (user.roles().contains("R04") && guard.countCertificationScope(user.userId(), owner) > 0)) return;
        throw new ForbiddenException();
    }

    /** R09 is explicitly admitted by the final request; other roles cannot inherit write permission. */
    public static void requireRole(CurrentUser user, boolean write) {
        if (user == null) throw new UnauthenticatedException();
        List<String> allowed = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) throw new ForbiddenException();
    }

    private boolean isAdmin(CurrentUser user) {
        return user.roles().contains("R09");
    }

    private void permission(CurrentUser user, String function, String status) {
        for (String role : user.roles()) {
            if (!List.of("R01", "R02", "R04", "R09").contains(role)) continue;
            try {
                if (functions.evaluate(new FunctionPermissionEvaluateRequest(
                        SCREEN, role, function, status, String.valueOf(user.userId()))).allowed()) return;
            } catch (ForbiddenException exception) {
                // An admitted role with ALLOW may still authorize a multi-role caller.
            }
        }
        throw new ForbiddenException();
    }

    private void invalid(String field, String message) {
        throw new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }
}
