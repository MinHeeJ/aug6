package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns atomic FR-032 writes, shared lifecycle guards and union-of-role read scope. */
@Service
public class EmploymentRateAchievementService {
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardService guard;
    private final EducationAchievementGuardMapper guardQueries;
    private final EmploymentRateExcelRepository excel;
    private final FileStoragePort storage;
    private final ObjectMapper json;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardService guard,
            EducationAchievementGuardMapper guardQueries,
            EmploymentRateExcelRepository excel,
            FileStoragePort storage,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.guardQueries = guardQueries;
        this.excel = excel;
        this.storage = storage;
        this.json = json;
    }

    /** List and total use the same already-normalized filter and union-of-scope map. */
    @Transactional(readOnly = true)
    public Map<String, Object> list(CurrentUser user, Map<String, String> filters, int page, int size) {
        require(user, "R01", "R02", "R04");
        if (page < 0 || !List.of(20, 50, 100).contains(size)) {
            invalid("pageSize", "페이지와 표시 건수(20/50/100)를 확인하세요.");
        }
        Map<String, Object> q = criteria(user, filters);
        q.put("pageSize", size);
        q.put("pageOffset", (long) page * size);
        return Map.of("achievements", mapper.list(q).stream().map(this::normalize).toList(),
                "totalElements", mapper.count(q), "page", page, "pageSize", size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(CurrentUser user, long id) {
        require(user, "R01", "R02", "R04");
        Map<String, Object> row = existing(id, false);
        requireScope(user, row);
        return normalize(row);
    }

    /** Export deliberately omits pagination while retaining every filter and scope. */
    @Transactional(readOnly = true)
    public byte[] download(CurrentUser user, Map<String, String> filters) {
        require(user, "R01", "R02", "R04", "R07");
        List<List<String>> grid = new ArrayList<>();
        grid.add(List.of("관리번호", "교번", "평가연도", "관리항목코드", "업적발생일", "실적명", "상태"));
        for (Map<String, Object> row : mapper.list(criteria(user, filters))) {
            grid.add(List.of(text(row, "managementNo"), text(row, "employeeNo"), text(row, "evaluationYear"),
                    text(row, "managementItemCode"), text(row, "achievementDate"), text(row, "achievementName"),
                    text(row, "achievementStatus")));
        }
        return new EmploymentRateWorkbookCodec().write(grid);
    }

    /** Row lock and shared guards precede mutations; evaluation year stays fixed on update. */
    @Transactional
    public Map<String, Object> save(
            CurrentUser user, EmploymentRateAchievementRequest request, Long id, String requestId) {
        require(user, "R01");
        validate(request);
        // Same fixed lock order as Excel commit prevents guard/config changes racing a write.
        excel.lockGuards();
        Map<String, Object> old = id == null ? null : existing(id, true);
        if (old != null && !user.userId().equals(((Number) old.get("teacherUserId")).longValue())) {
            throw new ForbiddenException();
        }
        if (old != null && !List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED")
                .contains(text(old, "achievementStatus"))) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 현재 상태에서는 수정할 수 없습니다.");
        }
        String year = old == null ? String.valueOf(request.achievementDate().getYear())
                : text(old, "evaluationYear");
        var warning = guard.validateMutation(user,
                new EducationAchievementMutationContext(user.userId(), year, request.achievementDate()));
        validateFields(request, user.userId(), year);
        Map<String, Object> saved = persist(user, request, id, user.userId(), year, requestId, old);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("achievement", saved);
        result.put("achievementDateWarning", warning.warning());
        result.put("occurredDateWarning", warning.warning());
        result.put("warningMessage", warning.message());
        return result;
    }

    /** R07 imports recheck target scope and shared period/finalization queries, not the R01-only guard. */
    @Transactional
    public Map<String, Object> createImported(CurrentUser user, EmploymentRateAchievementRequest request,
            long teacherUserId, String evaluationYear, String requestId) {
        require(user, "R07");
        validate(request);
        if (guardQueries.countCertificationScope(user.userId(), teacherUserId) == 0) throw new ForbiddenException();
        if (guardQueries.countActiveInputPeriods(evaluationYear, teacherUserId) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        if (guardQueries.countEvaluationConfirmations(teacherUserId, evaluationYear) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 대상입니다.");
        }
        validateFields(request, teacherUserId, evaluationYear);
        if (excel.countDuplicate(teacherUserId, evaluationYear, request.managementItemCode(),
                request.achievementDate()) > 0) throw new ConflictException("DUPLICATE: 중복 실적입니다.");
        return persist(user, request, null, teacherUserId, evaluationYear, requestId, null);
    }

    private Map<String, Object> persist(CurrentUser user, EmploymentRateAchievementRequest request,
            Long id, long teacher, String year, String requestId, Map<String, Object> old) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("achievementId", id);
        row.put("teacherUserId", teacher);
        row.put("evaluationYear", year);
        row.put("managementItemCode", request.managementItemCode().trim());
        row.put("achievementDate", request.achievementDate());
        row.put("achievementName", request.achievementName());
        row.put("achievementDetail", request.achievementDetail());
        row.put("attachmentRef", request.attachmentRef());
        row.put("actorId", user.userId());
        row.put("requestId", requestId);
        if (id == null) {
            String org = mapper.organization(teacher);
            if (org == null) throw new ForbiddenException();
            row.put("organizationCode", org);
            row.put("managementNo", "ER-" + UUID.randomUUID());
            mapper.insert(row);
            id = ((Number) row.get("achievementId")).longValue();
            mapper.statusHistory(id, user.userId(), requestId);
        } else if (mapper.update(row) != 1) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 실적 상태가 변경되었습니다.");
        }
        Map<String, Object> saved = normalize(existing(id, false));
        mapper.history(id, old == null ? null : encode(normalize(old)), encode(saved),
                user.userId(), requestId, old == null ? "CREATE" : "UPDATE");
        return saved;
    }

    /** Minimal approved bulk schema. No caller can assert that an unresolved policy is approved. */
    public record BulkRequest(String evaluationYear, Map<String, Object> targetConditionJson,
                              String actionType, Boolean confirmed) { }

    @Transactional
    public void createBulk(CurrentUser user, BulkRequest request) {
        require(user, "R07");
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || request.evaluationYear() == null
                || !request.evaluationYear().matches("[0-9]{4}")) {
            fields.add(new ValidationError("evaluationYear", "YYYY 평가연도가 필요합니다."));
        }
        if (request == null || request.actionType() == null
                || !List.of("GENERATE", "DELETE").contains(request.actionType())) {
            fields.add(new ValidationError("actionType", "GENERATE 또는 DELETE를 선택하세요."));
        }
        if (request == null || request.targetConditionJson() == null) {
            fields.add(new ValidationError("targetConditionJson", "대상 조건이 필요합니다."));
        }
        if (!fields.isEmpty()) throw new BusinessValidationException("일괄 작업 입력값을 확인하세요.", fields);
        throw new ConflictException("OQ-83-01: 생성·삭제 정책 미승인으로 작업을 접수하지 않습니다.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> preview(CurrentUser user, Map<String, String> filters) {
        require(user, "R07");
        return Map.of("candidates", mapper.list(criteria(user, filters)), "policyApproved", false,
                "policyMessage", "OQ-83-01 미확정: 실행 자격과 삭제 가능 여부는 판정하지 않습니다.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> job(CurrentUser user, String id) {
        require(user, "R07");
        Map<String, Object> row = mapper.job(id, user.userId());
        if (row == null) throw new NotFoundException("범위 내 작업을 찾을 수 없습니다.");
        row = normalize(row);
        row.put("items", mapper.jobItems(id));
        row.put("seed", "Y".equals(row.get("seedYn")));
        return row;
    }

    private void validate(EmploymentRateAchievementRequest r) {
        if (r == null || r.managementItemCode() == null || r.managementItemCode().isBlank()) {
            invalid("managementItemCode", "관리항목을 입력하세요.");
        }
        if (r.achievementDate() == null) invalid("achievementDate", "업적발생일을 입력하세요.");
    }

    private void validateFields(EmploymentRateAchievementRequest r, long teacher, String year) {
        // Item membership is anchored to the evaluation year, not an out-of-period occurred date.
        if (excel.countItem(r.managementItemCode().trim(), year, LocalDate.parse(year + "-06-30")) != 1) {
            invalid("managementItemCode", "활성 관리항목이 유일하게 일치해야 합니다.");
        }
        if (r.attachmentRef() != null && !r.attachmentRef().isBlank()
                && !storage.exists(teacher, r.attachmentRef())) invalid("attachmentRef", "본인 첨부 참조를 확인하세요.");
    }

    private Map<String, Object> existing(long id, boolean lock) {
        Map<String, Object> row = mapper.find(id, lock);
        if (row == null) throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        return row;
    }

    private void requireScope(CurrentUser user, Map<String, Object> row) {
        long owner = ((Number) row.get("teacherUserId")).longValue();
        if (user.roles().contains("R01") && user.userId() == owner) return;
        if (user.roles().contains("R02") && guardQueries.countSharedActiveOrganization(user.userId(), owner) > 0) return;
        if (user.roles().contains("R04") && guardQueries.countCertificationScope(user.userId(), owner) > 0) return;
        throw new ForbiddenException();
    }

    private Map<String, Object> criteria(CurrentUser user, Map<String, String> filters) {
        Map<String, Object> q = new LinkedHashMap<>();
        for (String key : List.of("managementNo", "teacherName", "managementItemCode", "certificationStatus",
                "evaluationYear", "organizationCode")) {
            String value = filters.get(key);
            if (value != null && !value.isBlank()) q.put(key, value.trim());
        }
        q.put("userId", user.userId());
        q.put("roles", user.roles());
        return q;
    }

    public static void require(CurrentUser user, String... roles) {
        if (user == null || user.userId() == null) throw new UnauthenticatedException();
        if (user.roles() == null || user.roles().stream().noneMatch(List.of(roles)::contains)) {
            throw new ForbiddenException();
        }
    }

    private void invalid(String field, String message) {
        throw new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private String text(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString();
    }

    private Map<String, Object> normalize(Map<String, Object> source) {
        Map<String, Object> row = new LinkedHashMap<>(source);
        row.replaceAll((k, v) -> v instanceof java.sql.Date d ? d.toLocalDate().toString()
                : v instanceof java.sql.Timestamp t ? t.toLocalDateTime().toString() : v);
        return row;
    }

    private String encode(Map<String, Object> row) {
        try { return json.writeValueAsString(row); }
        catch (Exception e) { throw new IllegalStateException("실적 이력 직렬화 실패", e); }
    }
}
