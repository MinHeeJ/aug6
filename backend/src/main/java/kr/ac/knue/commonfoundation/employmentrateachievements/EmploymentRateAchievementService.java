package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.common.educationachievements.EducationAchievementAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns guarded normalized writes and immutable before/after histories in one transaction. */
@Service
public class EmploymentRateAchievementService {
    static final String SCREEN = "SCR-EMPLOYMENT-RATE-ACHIEVEMENTS";
    static final String ROUTE = "/faculty/education/employment-rate-achievements";
    static final Set<String> READ = Set.of("R01", "R02", "R04");
    static final Set<String> DOWNLOAD = Set.of("R01", "R02", "R04", "R07");
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementAccessPolicy access;
    private final EducationAchievementGuardMapper guard;
    private final ObjectMapper json;

    public EmploymentRateAchievementService(EmploymentRateAchievementMapper mapper,
            EducationAchievementAccessPolicy access, EducationAchievementGuardMapper guard, ObjectMapper json) {
        this.mapper = mapper;
        this.access = access;
        this.guard = guard;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(int page, int pageSize, String year, String code, String status,
            CurrentUser user, boolean download) {
        if (download) access.requireDownload(user);
        else access.requireRead(user);
        function(user, "READ", null, download ? DOWNLOAD : READ);
        if (page < 0 || !Set.of(20, 50, 100).contains(pageSize)) {
            invalid("pageSize", "표시 건수는 20/50/100, 페이지는 0 이상이어야 합니다.");
        }
        Map<String, Object> q = new HashMap<>();
        q.put("pageSize", pageSize);
        q.put("rowOffset", (long) page * pageSize);
        q.put("userId", user.userId());
        q.put("roles", user.roles());
        q.put("download", download);
        q.put("evaluationYear", blank(year));
        q.put("managementItemCode", blank(code));
        q.put("achievementStatus", blank(status));
        return Map.of("achievements", mapper.list(q), "totalElements", mapper.count(q),
                "page", page, "pageSize", pageSize);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id, CurrentUser user) {
        access.requireRead(user);
        function(user, "READ", null, READ);
        Map<String, Object> row = find(id, false);
        access.requireReadScope(user, number(row.get("teacherUserId")));
        return row;
    }

    /** Preserves the persisted owner and year on PUT; no implicit upsert or body state change. */
    @Transactional
    public Map<String, Object> save(Long id, EmploymentRateAchievementRequest request,
            CurrentUser user, String requestId) {
        access.requireMutation(user, request.teacherUserId() == null ? user.userId() : request.teacherUserId());
        Map<String, Object> before = id == null ? null : find(id, true);
        Long owner = before == null ? (request.teacherUserId() == null ? user.userId() : request.teacherUserId())
                : number(before.get("teacherUserId"));
        access.requireMutation(user, owner);
        String year = before == null ? request.evaluationYear() : text(before.get("evaluationYear"));
        if (before != null) {
            access.requireMutableStatus(text(before.get("achievementStatus")));
            if (!Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED")
                    .contains(text(before.get("achievementStatus")))) {
                throw new ConflictException("STATUS_NOT_EDITABLE: 현재 상태에서는 수정할 수 없습니다.");
            }
        }
        Map<String, Object> row = prepare(request, owner, year, user, requestId, false);
        row.put("achievementId", id);
        function(user, id == null ? "CREATE" : "UPDATE",
                before == null ? "DRAFT" : text(before.get("achievementStatus")), Set.of("R01"));
        ensureUnique(row);
        persist(row, before);
        boolean warning = guard.countEvaluationDatePeriods(year, owner, request.achievementDate()) == 0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("achievement", find(number(row.get("achievementId")), false));
        result.put("occurredDateWarning", warning);
        result.put("warningMessage", warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 허용됩니다." : null);
        return result;
    }

    /** Importer uses this validation before writing any row, then materializes the whole set atomically. */
    Map<String, Object> prepare(EmploymentRateAchievementRequest request, Long owner, String year,
            CurrentUser user, String requestId, boolean importer) {
        if (owner == null) invalid("employeeNo", "존재하지 않는 교번입니다.");
        if (importer) access.requireDownloadScope(user, owner);
        if (year == null || !year.matches("[0-9]{4}")) invalid("evaluationYear", "평가연도를 입력하세요.");
        if (blank(request.managementItemCode()) == null) invalid("managementItemCode", "관리항목을 입력하세요.");
        if (request.achievementDate() == null) invalid("achievementDate", "발생일을 입력하세요.");
        if (guard.countActiveInputPeriods(year, owner) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        if (guard.countEvaluationConfirmations(owner, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 대상자는 변경할 수 없습니다.");
        }
        String organization = mapper.organization(owner);
        if (organization == null) invalid("teacherUserId", "현재 소속이 없습니다.");
        List<Map<String, Object>> rules = mapper.rules(request.managementItemCode().trim(), year);
        if (rules.isEmpty()) invalid("managementItemCode", "유효한 교육영역 관리항목이 아닙니다.");
        JsonNode detail = request.achievementDetail();
        if (detail != null && !detail.isObject()) invalid("achievementDetail", "상세는 JSON 객체여야 합니다.");
        for (Map<String, Object> rule : rules) {
            if (!"Y".equals(rule.get("teacherEditableYn"))) {
                invalid("managementItemCode", "직접 입력할 수 없는 관리항목입니다.");
            }
            JsonNode value = detail == null ? null : detail.get(request.managementItemCode().trim());
            if ("Y".equals(rule.get("requiredYn")) && (value == null || value.isNull()
                    || (value.isTextual() && value.asText().isBlank()))) {
                invalid("achievementDetail", "관리항목 필수값을 입력하세요.");
            }
            if (value != null && !value.isNull()) {
                boolean valid = switch (text(rule.get("dataType"))) {
                    case "NUMBER" -> value.isNumber();
                    case "BOOLEAN" -> value.isBoolean();
                    case "DATE" -> validDate(value);
                    default -> value.isTextual();
                };
                if (!valid) invalid("achievementDetail", "관리항목 자료형이 올바르지 않습니다.");
            }
        }
        String ref = blank(request.attachmentRef());
        if (ref != null && (ref.length() > 300 || ref.contains("..") || ref.contains("/") || ref.contains("\\"))) {
            invalid("attachmentRef", "비노출 첨부 참조값을 확인하세요.");
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("teacherUserId", owner);
        row.put("organizationCode", organization);
        row.put("evaluationYear", year);
        row.put("managementItemCode", request.managementItemCode().trim());
        row.put("achievementDate", request.achievementDate());
        row.put("achievementName", blank(request.achievementName()));
        row.put("achievementDetail", detail == null ? "{}" : detail.toString());
        row.put("attachmentRef", ref);
        row.put("actor", user.userId());
        row.put("requestId", requestId);
        return row;
    }

    void ensureUnique(Map<String, Object> row) {
        if (mapper.duplicates(row) > 0) throw new ConflictException("DUPLICATE_ACHIEVEMENT: 중복 실적입니다.");
    }

    void persist(Map<String, Object> row, Map<String, Object> before) {
        if (before == null) {
            row.put("achievementId", mapper.insert(row));
            mapper.statusHistory(row);
        } else if (mapper.update(row) != 1) {
            throw new ConflictException("STATUS_NOT_EDITABLE: 실적이 변경되었습니다.");
        }
        for (String field : List.of("teacherUserId", "evaluationYear", "managementItemCode", "achievementDate",
                "achievementName", "achievementDetail", "attachmentRef")) {
            String old = before == null ? null : text(before.get(field));
            String updated = text(row.get(field));
            if (before == null || !Objects.equals(old, updated)) {
                mapper.history(row, field, old, updated, before == null ? "CREATE" : "UPDATE");
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> job(String id, CurrentUser user) {
        access.requireExcel(user);
        function(user, "READ", null, Set.of("R07"));
        Map<String, Object> row = mapper.job(id);
        if (row == null) throw new NotFoundException("일괄 작업이 없습니다.");
        access.requireUploadOwner(user, number(row.get("requesterUserId")));
        row.put("items", mapper.jobItems(id));
        return row;
    }

    /** No guessed eligibility/deletion policy may create a job until an approval exists. */
    public void bulk(EmploymentRateBulkJobRequest request, CurrentUser user) {
        access.requireExcel(user);
        function(user, "EXECUTE", "DRAFT", Set.of("R07"));
        throw new ConflictException("BULK_POLICY_NOT_APPROVED: 생성·삭제 정책이 승인되지 않았습니다.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> preview(EmploymentRateBulkJobRequest request, CurrentUser user) {
        access.requireExcel(user);
        function(user, "READ", null, Set.of("R07"));
        Map<String, Object> targets = list(0, 20, request.evaluationYear(), null, null, user, true);
        return Map.of("executable", false, "reason", "생성·삭제 정책 미승인", "targets", targets);
    }

    void function(CurrentUser user, String type, String status, Set<String> roles) {
        access.requireFunction(user, SCREEN, ROUTE, type, status, roles);
    }

    private Map<String, Object> find(Long id, boolean lock) {
        Map<String, Object> row = mapper.find(id, lock);
        if (row == null) throw new NotFoundException("취업률 실적이 없습니다.");
        return row;
    }

    static void invalid(String field, String message) {
        throw new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static String text(Object value) {
        return value == null ? null : value.toString();
    }

    static Long number(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private boolean validDate(JsonNode node) {
        try {
            LocalDate.parse(node.asText());
            return node.isTextual();
        } catch (RuntimeException invalid) {
            return false;
        }
    }
}
