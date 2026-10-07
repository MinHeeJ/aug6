package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import kr.ac.knue.commonfoundation.storage.FileStoragePort;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped employment reads and atomic writes with immutable owner/year and complete change history. */
@Service
public class EmploymentRateAchievementService {
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardMapper guards;
    private final ObjectMapper json;
    private final FileStoragePort storage;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardMapper guards,
            ObjectMapper json,
            FileStoragePort storage) {
        this.mapper = mapper;
        this.guards = guards;
        this.json = json;
        this.storage = storage;
    }

    /** List and total share a single SQL predicate, including the union of role scopes. */
    @Transactional(readOnly = true)
    public Map<String, Object> list(Map<String, Object> query, CurrentUser user) {
        requireRole(user, "R01", "R02", "R04");
        query.put("userId", user.userId());
        query.put("roles", user.roles());
        return Map.of(
                "achievements", mapper.list(query).stream().map(this::decode).toList(),
                "page", query.get("page"), "pageSize", query.get("pageSize"),
                "totalElements", mapper.count(query),
                "managementItems", mapper.managementItems(null));
    }

    /** Detail uses the same role union and data boundary as list. */
    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id, CurrentUser user) {
        requireRole(user, "R01", "R02", "R04");
        Map<String, Object> row = existing(id, false);
        if (mapper.visible(scope(user), id) == 0) {
            throw new ForbiddenException();
        }
        return decode(row);
    }

    /** Create only; the database-generated key is used before writing history or re-reading. */
    @Transactional
    public Map<String, Object> create(EmploymentRateAchievementRequest request, CurrentUser user, String requestId) {
        requireRole(user, "R01");
        Map<String, Object> row = prepare(request, user.userId(), user, null);
        row.put("requestId", requestId);
        boolean warning = validatePeriods(row);
        persistNew(row);
        return result(row, warning, requestId);
    }

    /** Locks before checking ownership/state; year and organization are never re-derived on update. */
    @Transactional
    public Map<String, Object> update(
            Long id, EmploymentRateAchievementRequest request, CurrentUser user, String requestId) {
        requireRole(user, "R01");
        Map<String, Object> old = existing(id, true);
        if (!isAdmin(user) && !Objects.equals(old.get("teacherUserId"), user.userId())) {
            throw new ForbiddenException();
        }
        if ("EVALUATION_CONFIRMED".equals(old.get("achievementStatus"))) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        if (!"DRAFT".equals(old.get("achievementStatus"))) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 작성중 실적만 수정할 수 있습니다.");
        }
        Map<String, Object> row = prepare(request, (Long) old.get("teacherUserId"), user, old);
        row.put("achievementId", id);
        row.put("requestId", requestId);
        boolean warning = validatePeriods(row);
        checkDuplicate(row);
        try {
            mapper.update(row);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("DUPLICATE_ACHIEVEMENT: 같은 업무키의 실적이 이미 있습니다.");
        }
        auditFields(old, row, "UPDATE");
        return result(row, warning, requestId);
    }

    /** Revalidates actual owner, scope, setting and attachment evidence for an Excel row. */
    Map<String, Object> prepare(
            EmploymentRateAchievementRequest request, Long owner, CurrentUser actor, Map<String, Object> old) {
        validateRequest(request);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("teacherUserId", owner);
        String org = old == null ? unique(mapper.organizations(owner), "organizationCode")
                : old.get("organizationCode").toString();
        if (!isAdmin(actor) && !owner.equals(actor.userId())) {
            if (!actor.roles().contains("R07") || mapper.departmentScope(actor.userId(), org) == 0) {
                throw new ForbiddenException();
            }
        }
        String year;
        if (old == null) {
            List<String> years = mapper.years(org);
            if (years.isEmpty()) {
                throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 없습니다.");
            }
            year = unique(years, "evaluationYear");
        } else {
            year = old.get("evaluationYear").toString();
        }
        List<Map<String, Object>> items = mapper.managementItems(year).stream()
                .filter(item -> request.managementItemCode().trim().equals(item.get("managementItemCode")))
                .toList();
        if (items.size() != 1 || !"Y".equals(items.get(0).get("teacherEditableYn"))) {
            invalid("managementItemCode", "활성 교육영역의 수정 가능한 유일한 관리항목을 선택하세요.");
        }
        List<String> attachments = request.attachmentIds() == null ? List.of() : request.attachmentIds();
        for (String token : attachments) {
            Map<String, Object> metadata = token == null ? null : mapper.file(token);
            if (metadata == null || !Objects.equals(metadata.get("ownerId"), owner)
                    || !storage.exists(token) || storage.size(token) > 10 * 1024 * 1024) {
                invalid("attachmentIds", "실제 보존된 본인 소유의 10MB 이하 파일 참조만 사용할 수 있습니다.");
            }
        }
        row.put("organizationCode", org);
        row.put("evaluationYear", year);
        row.put("managementItemCode", request.managementItemCode().trim());
        row.put("achievementDate", request.achievementDate());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("achievementName", request.achievementName());
        row.put("achievementDetail", serialize(detail));
        row.put("attachmentRef", serialize(attachments));
        row.put("actorId", actor.userId());
        return row;
    }

    /** Existing platform period/finalization predicates are reused without widening their role policy. */
    boolean validatePeriods(Map<String, Object> row) {
        Long owner = (Long) row.get("teacherUserId");
        String year = row.get("evaluationYear").toString();
        if (guards.countEvaluationConfirmations(owner, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 대상은 변경할 수 없습니다.");
        }
        if (guards.countActiveInputPeriods(year, owner) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        return guards.countEvaluationDatePeriods(year, owner, (LocalDate) row.get("achievementDate")) == 0;
    }

    void checkDuplicate(Map<String, Object> row) {
        if (mapper.duplicate(row) > 0) {
            throw new ConflictException("DUPLICATE_ACHIEVEMENT: 같은 업무키의 실적이 이미 있습니다.");
        }
    }

    void persistNew(Map<String, Object> row) {
        checkDuplicate(row);
        row.put("managementNo", "ERA-" + UUID.randomUUID());
        try {
            mapper.insert(row);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("DUPLICATE_ACHIEVEMENT: 같은 업무키의 실적이 이미 있습니다.");
        }
        mapper.initialStatus(row);
        auditFields(null, row, "CREATE");
    }

    private void auditFields(Map<String, Object> old, Map<String, Object> row, String change) {
        for (String field : List.of("managementItemCode", "achievementDate", "achievementDetail", "attachmentRef")) {
            String before = old == null || old.get(field) == null ? null : old.get(field).toString();
            String after = row.get(field) == null ? null : row.get(field).toString();
            if (old == null || !Objects.equals(before, after)) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("targetKey", row.get("achievementId").toString());
                entry.put("fieldName", field);
                entry.put("beforeValue", before);
                entry.put("afterValue", after);
                entry.put("actorId", row.get("actorId"));
                entry.put("changeType", change);
                entry.put("requestId", row.get("requestId"));
                mapper.audit(entry);
            }
        }
    }

    private Map<String, Object> result(Map<String, Object> row, boolean warning, String requestId) {
        return Map.of("achievementId", row.get("achievementId"),
                "achievement", decode(existing((Long) row.get("achievementId"), false)),
                "occurredDateWarning", warning,
                "warningMessage", warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 허용됩니다." : "",
                "requestId", requestId);
    }

    /** R07 downloads do not reuse the individually role-gated list operation. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> downloadRows(Map<String, Object> query, CurrentUser user) {
        requireRole(user, "R01", "R02", "R04", "R07");
        query.putAll(scope(user));
        return mapper.list(query).stream().map(this::decode).toList();
    }

    /** Approved policy is absent: valid commands perform no job, item or ledger writes. */
    public void bulk(EmploymentRateBulkJobRequest request, CurrentUser user) {
        requireRole(user, "R07");
        if (request == null || request.evaluationYear() == null || request.evaluationYear().isBlank()) {
            invalid("evaluationYear", "평가년도를 입력하세요.");
        }
        if (!List.of("GENERATE", "DELETE").contains(request.actionType())) {
            invalid("actionType", "GENERATE 또는 DELETE를 선택하세요.");
        }
        throw new ConflictException("BULK_POLICY_NOT_APPROVED: 일괄 생성조건과 삭제상태 정책이 미승인입니다.");
    }

    /** Results require both executor ownership and current department scope (R09 override is explicit). */
    @Transactional(readOnly = true)
    public Map<String, Object> job(String jobId, CurrentUser user) {
        requireRole(user, "R07");
        Long id;
        try {
            id = Long.valueOf(jobId);
        } catch (NumberFormatException exception) {
            throw new NotFoundException("일괄 작업을 찾을 수 없습니다.");
        }
        Map<String, Object> job = mapper.job(id);
        if (job == null) {
            throw new NotFoundException("일괄 작업을 찾을 수 없습니다.");
        }
        if (!isAdmin(user) && (!user.userId().equals(job.get("createdBy"))
                || mapper.departmentScope(user.userId(), job.get("organizationCode").toString()) == 0)) {
            throw new ForbiddenException();
        }
        Map<String, Object> result = new LinkedHashMap<>(job);
        result.put("items", mapper.jobItems(id));
        return result;
    }

    private Map<String, Object> existing(Long id, boolean lock) {
        Map<String, Object> row = mapper.find(id, lock);
        if (row == null) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private Map<String, Object> decode(Map<String, Object> row) {
        Map<String, Object> decoded = new LinkedHashMap<>(row);
        try {
            decoded.put("attachmentIds", json.readTree(row.get("attachmentRef").toString()));
            var detail = json.readTree(row.get("achievementDetail").toString());
            decoded.put("achievementDetail", detail);
            decoded.put("achievementName", detail.path("achievementName").asText(""));
            if (row.get("achievementDate") instanceof java.sql.Date date) {
                decoded.put("achievementDate", date.toLocalDate());
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 실적 형식이 올바르지 않습니다.", exception);
        }
        return decoded;
    }

    String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("입력값 형식이 올바르지 않습니다.");
        }
    }

    static Map<String, Object> scope(CurrentUser user) {
        return Map.of("userId", user.userId(), "roles", user.roles());
    }

    static boolean isAdmin(CurrentUser user) {
        return user.roles().contains("R09");
    }

    static void requireRole(CurrentUser user, String... allowed) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || (!isAdmin(user)
                && user.roles().stream().noneMatch(List.of(allowed)::contains))) {
            throw new ForbiddenException();
        }
    }

    private static String unique(List<String> values, String field) {
        if (values.size() != 1) {
            invalid(field, "설정이 없거나 복수여서 결정할 수 없습니다.");
        }
        return values.get(0);
    }

    static void invalid(String field, String message) {
        throw new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }

    private static void validateRequest(EmploymentRateAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null || request.managementItemCode() == null || request.managementItemCode().isBlank()) {
            errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
        }
        if (request == null || request.achievementDate() == null) {
            errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("입력값을 확인하세요.", errors);
        }
    }
}
