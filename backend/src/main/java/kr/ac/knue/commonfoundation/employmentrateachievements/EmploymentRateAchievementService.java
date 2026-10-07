package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns source mutation and field audits in one transaction; bulk execution is deliberately disabled. */
@Service
public class EmploymentRateAchievementService {
    private static final String SCREEN = "SCR-EMPLOYMENT-RATE-ACHIEVEMENT";
    private static final Set<String> EDITABLE = Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED");
    private static final List<String> FIELDS = List.of("managementNo", "achievementType", "teacherUserId",
            "organizationCode", "evaluationYear", "managementItemCode", "achievementDate", "achievementName",
            "attachmentRef", "achievementStatus", "deletedYn");
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardMapper guards;
    private final FunctionPermissionMapper functions;
    private final ObjectMapper json;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardMapper guards,
            FunctionPermissionMapper functions,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guards = guards;
        this.functions = functions;
        this.json = json;
    }

    /** Exact endpoint roles with the final user's explicit R09 administrative override. */
    public static void requireRoles(CurrentUser user, String... roles) {
        if (user == null || user.userId() == null) throw new UnauthenticatedException();
        if (user.roles() == null || (!user.roles().contains("R09")
                && user.roles().stream().noneMatch(Set.of(roles)::contains))) throw new ForbiddenException();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(int page, int pageSize, CurrentUser user) {
        requireRoles(user, "R01", "R02", "R04");
        Map<String, Object> scope = scope(page, pageSize, user, false);
        return Map.of("achievements", mapper.list(scope).stream().map(this::present).toList(),
                "page", page, "pageSize", pageSize, "totalElements", mapper.count(scope));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id, CurrentUser user) {
        requireRoles(user, "R01", "R02", "R04");
        Map<String, Object> row = existing(id, false);
        requireReadScope(user, number(row.get("teacherUserId")), false);
        requireFunction(user, "READ", "R01", "R02", "R04");
        return present(row);
    }

    /** Download obeys the same union as list, adding only R07's mapped certification organizations. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> downloadRows(int page, int pageSize, CurrentUser user) {
        requireRoles(user, "R01", "R02", "R04", "R07");
        return mapper.list(scope(page, pageSize, user, true)).stream().map(this::present).toList();
    }

    /** Derives immutable owner/year and a single date-valid organization, then uses the generated PK for audits. */
    @Transactional
    public Map<String, Object> create(EmploymentRateAchievementRequest input, CurrentUser user, String requestId) {
        requireRoles(user, "R01");
        requestId = RequestIds.normalize(requestId);
        validate(input);
        Long teacher = user.userId();
        String year = String.format(java.util.Locale.ROOT, "%04d", input.achievementDate().getYear());
        mutationGuards(teacher, year, "DRAFT", requestId);
        requireFunction(user, "CREATE", "R01");
        List<String> organizations = mapper.organizations(teacher, input.achievementDate());
        if (organizations.size() != 1) throw invalid("organizationCode", "발생일에 유효한 단일 조직이 필요합니다.");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("teacherUserId", teacher);
        row.put("evaluationYear", year);
        row.put("organizationCode", organizations.get(0));
        row.put("managementNo", "ER-" + UUID.randomUUID());
        row.put("achievementType", "EMPLOYMENT_RATE_ACHIEVEMENT");
        row.put("achievementStatus", "DRAFT");
        row.put("deletedYn", "N");
        values(row, input, user);
        try {
            mapper.insert(row);
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            throw new ConflictException("동일 실적이 이미 존재합니다.");
        }
        if (row.get("achievementId") == null) throw new IllegalStateException("Missing generated achievement key");
        mapper.statusHistory(row);
        audit(Map.of(), row, "CREATE", user, requestId);
        return saved(row, teacher, year, input);
    }

    /** Locks the source row. The request cannot change owner, organization, year or status. */
    @Transactional
    public Map<String, Object> update(
            Long id, EmploymentRateAchievementRequest input, CurrentUser user, String requestId) {
        requireRoles(user, "R01");
        requestId = RequestIds.normalize(requestId);
        validateRequired(input);
        Map<String, Object> before = existing(id, true);
        Long teacher = number(before.get("teacherUserId"));
        if (!admin(user) && !teacher.equals(user.userId())) throw new ForbiddenException();
        String year = before.get("evaluationYear").toString();
        mutationGuards(teacher, year, Objects.toString(before.get("achievementStatus"), ""), requestId);
        requireFunction(user, "UPDATE", "R01");
        validate(input);
        Map<String, Object> row = new LinkedHashMap<>(before);
        values(row, input, user);
        try {
            if (mapper.update(row) != 1) throw new ConflictException("실적 상태가 변경되어 수정할 수 없습니다.");
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            throw new ConflictException("동일 실적이 이미 존재합니다.");
        }
        audit(before, row, "UPDATE", user, requestId);
        return saved(row, teacher, year, input);
    }

    /** Authorized, well-formed requests always fail closed without creating a job or changing source data. */
    public Map<String, Object> createBulk(EmploymentRateBulkJobRequest input, CurrentUser user, String requestId) {
        requireRoles(user, "R07");
        List<ValidationError> errors = new java.util.ArrayList<>();
        if (input == null || input.evaluationYear() == null || !input.evaluationYear().matches("[0-9]{4}")) {
            errors.add(new ValidationError("evaluationYear", "평가연도 YYYY를 입력하세요."));
        }
        if (input == null || !Set.of("GENERATE", "DELETE").contains(Objects.toString(input.actionType(), ""))) {
            errors.add(new ValidationError("actionType", "GENERATE 또는 DELETE 작업을 입력하세요."));
        }
        if (!errors.isEmpty()) throw new BusinessValidationException("일괄 요청이 올바르지 않습니다.", errors);
        requireFunction(user, "EXECUTE", "R07");
        throw new ConflictException("일괄 생성·삭제 정책이 승인되지 않아 실행하지 않았습니다. 원천과 작업 자료는 변경되지 않았습니다.");
    }

    /** Existing results are owner-scoped and every target remains inside the requester's mapped data scope. */
    @Transactional(readOnly = true)
    public Map<String, Object> job(String id, CurrentUser user) {
        requireRoles(user, "R07");
        requireFunction(user, "READ", "R07");
        Map<String, Object> row = mapper.job(id);
        if (row == null) throw new NotFoundException("일괄 작업이 없습니다.");
        if (!admin(user) && !user.userId().equals(number(row.get("requestedBy")))) throw new ForbiddenException();
        List<Map<String, Object>> items = mapper.jobItems(id);
        for (Map<String, Object> item : items) {
            if (!admin(user) && guards.countCertificationScope(user.userId(), number(item.get("targetUserId"))) == 0) {
                throw new ForbiddenException();
            }
        }
        Map<String, Object> result = new LinkedHashMap<>(row);
        result.put("items", items);
        Object condition = result.remove("targetConditionJson");
        if (condition != null) {
            try {
                result.put("targetCondition", json.readValue(
                        condition.toString(), new TypeReference<Map<String, Object>>() { }));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Invalid stored target condition", exception);
            }
        }
        return result;
    }

    /** Separate function check for controller-owned Excel upload orchestration. */
    public void authorizeUpload(CurrentUser user) {
        requireRoles(user, "R07");
        requireFunction(user, "CREATE", "R07");
    }

    private Map<String, Object> scope(int page, int pageSize, CurrentUser user, boolean download) {
        if (page < 0 || !Set.of(20, 50, 100).contains(pageSize)) throw invalid("pageSize", "페이지 크기는 20, 50, 100입니다.");
        Set<String> allowed = download ? Set.of("R01", "R02", "R04", "R07") : Set.of("R01", "R02", "R04");
        Set<String> effective = permittedRoles(user, "READ", allowed);
        boolean certification = effective.contains("R04") || effective.contains("R07");
        if (!admin(user) && effective.equals(Set.of("R07")) && mapper.countMappedOrganizations(user.userId()) == 0) {
            throw new ForbiddenException();
        }
        return Map.of("admin", admin(user), "self", effective.contains("R01"),
                "department", effective.contains("R02"), "certification", certification,
                "userId", user.userId(), "pageSize", pageSize, "offset", (long) page * pageSize);
    }

    private void requireReadScope(CurrentUser user, Long target, boolean download) {
        if (admin(user)) return;
        Set<String> effective = permittedRoles(user, "READ",
                download ? Set.of("R01", "R02", "R04", "R07") : Set.of("R01", "R02", "R04"));
        if (effective.contains("R01") && user.userId().equals(target)) return;
        if (effective.contains("R02") && guards.countSharedActiveOrganization(user.userId(), target) > 0) return;
        if ((effective.contains("R04") || effective.contains("R07"))
                && guards.countCertificationScope(user.userId(), target) > 0) return;
        throw new ForbiddenException();
    }

    private void requireFunction(CurrentUser user, String function, String... allowed) {
        permittedRoles(user, function, Set.of(allowed));
    }

    private Set<String> permittedRoles(CurrentUser user, String function, Set<String> allowed) {
        if (admin(user)) return allowed;
        Set<String> permitted = new java.util.HashSet<>();
        for (String role : user.roles()) {
            if (!allowed.contains(role)) continue;
            var permission = functions.findByKey(SCREEN, role, function);
            if (permission != null && "DENY".equals(permission.permissionAllowed())) throw new ForbiddenException();
            if (permission != null && "ALLOW".equals(permission.permissionAllowed())) permitted.add(role);
        }
        if (permitted.isEmpty()) throw new ForbiddenException();
        return permitted;
    }

    private void mutationGuards(Long teacher, String year, String status, String requestId) {
        if ("EVALUATION_CONFIRMED".equals(status) || guards.countEvaluationConfirmations(teacher, year) > 0) {
            throw new EducationAchievementConflictException("CONFIRMED_DATA_LOCKED", "평가확정 실적은 수정할 수 없습니다.", requestId);
        }
        if (guards.countActiveInputPeriods(year, teacher) == 0) {
            throw new EducationAchievementConflictException("PERIOD_NOT_ACTIVE", "활성 입력기간이 아닙니다.", requestId);
        }
        if (!EDITABLE.contains(status)) throw new ConflictException("작성 또는 반려 실적만 수정할 수 있습니다.");
    }

    /** Required fields are reported together before a target lookup or persistence-dependent validation. */
    private void validateRequired(EmploymentRateAchievementRequest input) {
        List<ValidationError> errors = new java.util.ArrayList<>();
        if (input == null || input.managementItemCode() == null || input.managementItemCode().isBlank()) {
            errors.add(new ValidationError("managementItemCode", "관리항목 코드를 입력하세요."));
        }
        if (input == null || input.achievementDate() == null) {
            errors.add(new ValidationError("achievementDate", "YYYY-MM-DD 날짜가 필요합니다."));
        }
        if (!errors.isEmpty()) throw new BusinessValidationException("실적 입력이 올바르지 않습니다.", errors);
    }

    private void validate(EmploymentRateAchievementRequest input) {
        validateRequired(input);
        if (input.managementItemCode() == null || input.managementItemCode().isBlank()
                || input.managementItemCode().length() > 50
                || mapper.countActiveItems(input.managementItemCode().trim()) != 1) {
            throw invalid("managementItemCode", "유일한 활성 교육 관리항목을 선택하세요.");
        }
        if (input.achievementDate() == null || input.achievementDate().getYear() < 1
                || input.achievementDate().getYear() > 9999) throw invalid("achievementDate", "YYYY-MM-DD 날짜가 필요합니다.");
        // No owned-file resolver is implemented in this slice. Fail closed, rather than persist unverified tokens.
        if (input.attachmentIds() != null && !input.attachmentIds().isEmpty()) {
            throw invalid("attachmentIds", "첨부 파일 소유권·존재 검증 어댑터가 없어 첨부 참조를 저장할 수 없습니다.");
        }
    }

    private void values(Map<String, Object> row, EmploymentRateAchievementRequest input, CurrentUser user) {
        row.put("managementItemCode", input.managementItemCode().trim());
        row.put("achievementDate", input.achievementDate().toString());
        row.put("achievementName", input.achievementName());
        row.put("attachmentRef", "[]");
        row.put("updatedBy", user.userId());
    }

    private Map<String, Object> existing(Long id, boolean lock) {
        if (id == null || id <= 0) throw invalid("achievementId", "유효한 실적 식별자가 필요합니다.");
        Map<String, Object> row = mapper.find(id, lock);
        if (row == null) throw new NotFoundException("실적이 없습니다.");
        return row;
    }

    private Map<String, Object> saved(
            Map<String, Object> row, Long teacher, String year, EmploymentRateAchievementRequest input) {
        boolean warning = guards.countEvaluationDatePeriods(year, teacher, input.achievementDate()) == 0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("achievement", present(existing(number(row.get("achievementId")), false)));
        result.put("occurredDateWarning", warning);
        result.put("warningMessage", warning ? "업적발생일이 평가기간 밖에 있습니다." : null);
        return result;
    }

    private Map<String, Object> present(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        try {
            result.put("attachmentIds", json.readValue(Objects.toString(row.get("attachmentRef"), "[]"),
                    new TypeReference<List<String>>() { }));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid stored attachment references", exception);
        }
        return result;
    }

    private void audit(Map<String, Object> before, Map<String, Object> after, String type,
            CurrentUser user, String requestId) {
        for (String field : FIELDS) {
            String oldValue = Objects.toString(before.get(field), null);
            String newValue = Objects.toString(after.get(field), null);
            if (!"CREATE".equals(type) && Objects.equals(oldValue, newValue)) continue;
            Map<String, Object> history = new LinkedHashMap<>();
            history.put("targetKey", after.get("achievementId").toString());
            history.put("changeType", type);
            history.put("fieldName", field);
            history.put("beforeValue", oldValue);
            history.put("afterValue", newValue);
            history.put("changedBy", user.userId());
            history.put("requestId", requestId);
            history.put("changeReason", "취업률 실적 " + type);
            mapper.history(history);
        }
    }

    private static Long number(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(value.toString());
    }

    private static boolean admin(CurrentUser user) {
        return user.roles().contains("R09");
    }

    private static BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }
}
