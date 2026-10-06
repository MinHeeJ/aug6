package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import kr.ac.knue.commonfoundation.excel.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import kr.ac.knue.commonfoundation.taskgaps.EmploymentRateBulkConditions;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns FR-032 scoped reads and atomic create/update plus complete before/after audit snapshots. */
@Service
public class EmploymentRateAchievementService {
    private static final String SCREEN = "SCR-EMPLOYMENT-RATE-ACHIEVEMENT";
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardService guards;
    private final FunctionPermissionService permissions;
    private final FileStoragePort storage;
    private final ObjectMapper json;
    private final EmploymentRateWorkbook workbook;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardService guards,
            FunctionPermissionService permissions,
            FileStoragePort storage,
            ObjectMapper json,
            EmploymentRateWorkbook workbook) {
        this.mapper = mapper;
        this.guards = guards;
        this.permissions = permissions;
        this.storage = storage;
        this.json = json;
        this.workbook = workbook;
    }

    /** List and count share the exact mapper predicate and role-scope union. */
    @Transactional(readOnly = true)
    public Map<String, Object> list(Map<String, Object> query, CurrentUser user) {
        authorize(user, "READ", List.of("R01", "R02", "R04"));
        List<String> readRoles = user.roles().stream().filter(List.of("R01", "R02", "R04")::contains).toList();
        return Map.of(
                "achievements", mapper.list(query, user.userId(), readRoles).stream().map(this::row).toList(),
                "page", query.get("page"), "pageSize", query.get("pageSize"),
                "totalElements", mapper.count(query, user.userId(), readRoles));
    }

    /** Detail checks the same scope as listing; a guessed ID cannot disclose another teacher's row. */
    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id, CurrentUser user) {
        authorize(user, "READ", List.of("R01", "R02", "R04"));
        Map<String, Object> value = existing(id, false);
        List<String> readRoles = user.roles().stream().filter(List.of("R01", "R02", "R04")::contains).toList();
        if (mapper.visible(id, user.userId(), readRoles) == 0) throw new ForbiddenException();
        return row(value);
    }

    /** Create is distinct from update; generated keys are read before any detail/audit query. */
    @Transactional
    public Map<String, Object> create(EmploymentRateAchievementRequest request, CurrentUser user, String trace) {
        requireRole(user, List.of("R01"));
        mapper.lockMutationGuards();
        String year = Integer.toString(request.achievementDate().getYear());
        OccurredDateValidation warning = guards.validateMutation(user,
                new EducationAchievementMutationContext(user.userId(), year, request.achievementDate()));
        authorize(user, "CREATE", List.of("R01"));
        validateItem(request, year, user.userId());
        String organization = mapper.organization(user.userId());
        if (organization == null) throw new ForbiddenException();
        Map<String, Object> values = values(request, user.userId());
        values.put("managementNo", "ER-" + UUID.randomUUID());
        values.put("teacherUserId", user.userId());
        values.put("organizationCode", organization);
        values.put("evaluationYear", year);
        try {
            mapper.insert(values);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("DUPLICATE: 동일한 취업률 실적이 존재합니다.");
        }
        Long id = ((Number) values.get("achievementId")).longValue();
        Map<String, Object> saved = row(existing(id, false));
        mapper.initialStatus(id, user.userId());
        mapper.history(id, "CREATE", null, serialize(saved), user.userId(), trace);
        return saved(saved, warning);
    }

    /** Keeps the original evaluation year and blocks non-draft mutations without partial audit writes. */
    @Transactional
    public Map<String, Object> update(Long id, EmploymentRateAchievementRequest request,
            CurrentUser user, String trace) {
        requireRole(user, List.of("R01"));
        mapper.lockMutationGuards();
        Map<String, Object> before = row(existing(id, true));
        if (!user.userId().equals(((Number) before.get("teacherUserId")).longValue())) {
            throw new ForbiddenException();
        }
        String year = (String) before.get("evaluationYear");
        OccurredDateValidation warning = guards.validateMutation(user,
                new EducationAchievementMutationContext(user.userId(), year, request.achievementDate()));
        if (!"DRAFT".equals(before.get("certificationStatus"))) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 작성중 실적만 수정할 수 있습니다.");
        }
        authorize(user, "UPDATE", List.of("R01"));
        validateItem(request, year, user.userId());
        Map<String, Object> values = values(request, user.userId());
        values.put("achievementId", id);
        try {
            if (mapper.update(values) != 1) {
                throw new ConflictException("CONFIRMED_DATA_LOCKED: 실적 상태가 변경되었습니다.");
            }
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("DUPLICATE: 동일한 취업률 실적이 존재합니다.");
        }
        Map<String, Object> after = row(existing(id, false));
        mapper.history(id, "UPDATE", serialize(before), serialize(after), user.userId(), trace);
        return saved(after, warning);
    }

    /** Produces actual XLSX bytes from all matching rows without changing business state. */
    @Transactional(readOnly = true)
    public ExcelDownloadFile download(Map<String, Object> query, CurrentUser user) {
        authorize(user, "READ", List.of("R01", "R02", "R04", "R07"));
        Map<String, Object> all = new HashMap<>(query);
        all.remove("pageSize");
        List<List<String>> cells = new ArrayList<>();
        cells.add(List.of("관리번호", "성명", "관리항목코드", "업적발생일", "실적명", "인증상태"));
        for (Map<String, Object> value : mapper.list(all, user.userId(), user.roles())) {
            Map<String, Object> item = row(value);
            cells.add(List.of(text(item, "managementNo"), text(item, "teacherName"),
                    text(item, "managementItemCode"), text(item, "achievementDate"),
                    text(item, "achievementName"), text(item, "certificationStatus")));
        }
        return new ExcelDownloadFile("employment-rate-achievements.xlsx", EmploymentRateWorkbook.MIME,
                workbook.table(cells));
    }

    /** Pending OQ policy is fail-closed before any job or item insert. */
    @Transactional
    public Map<String, Object> createBulk(EmploymentRateBulkJobRequest request, CurrentUser user) {
        authorize(user, "EXECUTE", List.of("R07"));
        EmploymentRateBulkConditions.query(request.evaluationYear(), request.targetCondition());
        throw new ConflictException("POLICY_NOT_APPROVED: OQ-83-01 생성·삭제 정책 승인 전에는 실행할 수 없습니다.");
    }

    /** Preview is a scoped existing-ledger count, not an approval or invented generation target list. */
    @Transactional(readOnly = true)
    public Map<String, Object> preview(EmploymentRateBulkJobRequest request, CurrentUser user) {
        authorize(user, "EXECUTE", List.of("R07"));
        Map<String, Object> query = EmploymentRateBulkConditions.query(
                request.evaluationYear(), request.targetCondition());
        return Map.of("evaluationYear", request.evaluationYear(), "actionType", request.actionType(),
                "existingAchievementCount", mapper.count(query, user.userId(), List.of("R07")),
                "policyApproved", false);
    }

    /** A result is visible only to its R07 owner; seeded jobs follow the same ownership rule. */
    @Transactional(readOnly = true)
    public Map<String, Object> job(String id, CurrentUser user) {
        authorize(user, "READ", List.of("R07"));
        Map<String, Object> job = mapper.job(id, user.userId());
        if (job == null) throw new NotFoundException("일괄 작업을 찾을 수 없습니다.");
        Map<String, Object> result = row(job);
        result.put("items", mapper.jobItems(id, user.userId()));
        return result;
    }

    private Map<String, Object> existing(Long id, boolean lock) {
        Map<String, Object> value = mapper.find(id, lock);
        if (value == null) throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        return value;
    }

    private void validateItem(EmploymentRateAchievementRequest request, String year, Long owner) {
        List<Map<String, Object>> rules = mapper.itemRules(request.managementItemCode().trim(), year);
        if (rules.size() != 1) throw invalid("managementItemCode", "활성 관리항목이 없거나 모호합니다.");
        Map<String, Object> rule = rules.get(0);
        if (!"Y".equals(rule.get("editableYn"))) throw new ForbiddenException();
        String value = request.achievementName() == null ? "" : request.achievementName().trim();
        if (value.isEmpty() && "Y".equals(rule.get("requiredYn"))) {
            throw invalid("achievementName", "관리항목의 필수값을 입력하세요.");
        }
        if (!value.isEmpty()) {
            try {
                boolean valid = switch (String.valueOf(rule.get("dataType"))) {
                    case "TEXT" -> true;
                    case "NUMBER" -> { new java.math.BigDecimal(value); yield true; }
                    case "DATE" -> { LocalDate.parse(value); yield true; }
                    case "BOOLEAN" -> List.of("true", "false", "Y", "N").contains(value);
                    case "FILE" -> storage.isOwned(value, owner);
                    default -> false;
                };
                if (!valid) throw invalid("achievementName", "관리항목 자료형을 확인하세요.");
            } catch (IllegalArgumentException | java.time.DateTimeException exception) {
                throw invalid("achievementName", "관리항목 자료형을 확인하세요.");
            }
        }
        if (request.attachmentIds() != null) {
            for (String attachment : request.attachmentIds()) {
                if (!storage.isOwned(attachment, owner)) throw invalid("attachmentIds", "본인 첨부 참조만 사용하세요.");
            }
        }
    }

    private Map<String, Object> values(EmploymentRateAchievementRequest request, Long actor) {
        Map<String, Object> values = new HashMap<>();
        values.put("actor", actor);
        values.put("managementItemCode", request.managementItemCode().trim());
        values.put("achievementDate", request.achievementDate());
        values.put("achievementName", request.achievementName() == null ? "" : request.achievementName().trim());
        values.put("attachmentIds", serialize(request.attachmentIds() == null ? List.of() : request.attachmentIds()));
        return values;
    }

    private Map<String, Object> saved(Map<String, Object> value, OccurredDateValidation warning) {
        return Map.of("achievement", value, "occurredDateWarning", warning.warning(),
                "warningMessage", warning.message() == null ? "" : warning.message());
    }

    private void requireRole(CurrentUser user, List<String> allowed) {
        if (user == null) throw new UnauthenticatedException();
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) throw new ForbiddenException();
    }

    private void authorize(CurrentUser user, String function, List<String> allowed) {
        requireRole(user, allowed);
        for (String role : user.roles()) {
            if (!allowed.contains(role)) continue;
            try {
                permissions.evaluate(new FunctionPermissionEvaluateRequest(SCREEN, role, function, "DRAFT", null));
                return;
            } catch (ForbiddenException exception) {
                // Try every admitted role rather than narrowing a multi-role principal to the first role.
            }
        }
        throw new ForbiddenException();
    }

    private Map<String, Object> row(Map<String, Object> source) {
        Map<String, Object> value = new LinkedHashMap<>(source);
        value.replaceAll((key, item) -> item instanceof java.sql.Date date ? date.toLocalDate()
                : item instanceof java.sql.Timestamp date ? date.toLocalDateTime() : item);
        for (String field : List.of("attachmentIds", "achievementDetail", "targetCondition")) {
            if (value.get(field) instanceof String encoded) {
                try { value.put(field, json.readTree(encoded)); }
                catch (JsonProcessingException exception) {
                    throw new IllegalStateException("Invalid stored JSON", exception);
                }
            }
        }
        return value;
    }

    private String serialize(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Snapshot serialization failed", exception);
        }
    }

    private String text(Map<String, Object> value, String key) {
        return value.get(key) == null ? "" : value.get(key).toString();
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException("취업률 실적 입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }
}
