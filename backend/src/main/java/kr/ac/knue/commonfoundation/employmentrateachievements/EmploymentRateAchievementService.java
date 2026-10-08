package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caller-scoped employment-rate reads and atomic header, lifecycle and full-snapshot audit writes. */
@Service
public class EmploymentRateAchievementService {
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardMapper guards;
    private final ObjectMapper json;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardMapper guards,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guards = guards;
        this.json = json;
    }

    public static void requireRole(CurrentUser user, String... allowed) {
        if (user == null) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || (!user.roles().contains("R09")
                && user.roles().stream().noneMatch(Set.of(allowed)::contains))) {
            throw new ForbiddenException();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(Map<String, Object> filters, CurrentUser user, boolean download) {
        if (download) {
            requireRole(user, "R01", "R02", "R04", "R07");
        } else {
            requireRole(user, "R01", "R02", "R04");
        }
        Map<String, Object> parameters = new LinkedHashMap<>(filters);
        parameters.put("userId", user.userId());
        parameters.put("roles", user.roles());
        return Map.of("achievements", mapper.list(parameters), "totalElements", mapper.count(parameters),
                "page", filters.get("page"), "pageSize", filters.get("pageSize"),
                "managementItems", mapper.inputOptions(user.userId()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id, CurrentUser user) {
        requireRole(user, "R01", "R02", "R04");
        Map<String, Object> row = existing(mapper.find(Map.of("achievementId", id)));
        requireScope(user, ((Number) row.get("teacherUserId")).longValue());
        return row;
    }

    private Map<String, Object> existing(Map<String, Object> row) {
        if (row == null) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Scope is a union of own, department and certification grants; administrator bypass is feature-local. */
    public void requireScope(CurrentUser user, Long teacher) {
        if (user.roles().contains("R09")
                || (user.roles().contains("R01") && teacher.equals(user.userId()))
                || ((user.roles().contains("R02") || user.roles().contains("R07"))
                    && guards.countSharedActiveOrganization(user.userId(), teacher) > 0)
                || ((user.roles().contains("R04") || user.roles().contains("R07"))
                    && guards.countCertificationScope(user.userId(), teacher) > 0)) {
            return;
        }
        throw new ForbiddenException();
    }

    @Transactional
    public Map<String, Object> save(
            Long id, EmploymentRateAchievementRequest request, CurrentUser user, String requestId) {
        requireRole(user, "R01");
        Map<String, Object> before = id == null ? null : existing(mapper.lock(Map.of("achievementId", id)));
        Long teacher = before == null ? user.userId() : ((Number) before.get("teacherUserId")).longValue();
        if (!user.roles().contains("R09") && !teacher.equals(user.userId())) {
            throw new ForbiddenException();
        }
        String year = before == null ? String.valueOf(request.achievementDate().getYear())
                : (String) before.get("evaluationYear");
        if (before != null) {
            requireEditable(before);
        }
        Map<String, Object> parameters = prepare(request, teacher, year, user, requestId);
        if (id != null) {
            parameters.put("achievementId", id);
        }
        checkDuplicate(parameters);
        write(parameters, before);
        Map<String, Object> saved = existing(mapper.find(Map.of("achievementId", parameters.get("achievementId"))));
        boolean warning = guards.countEvaluationDatePeriods(year, teacher, request.achievementDate()) == 0;
        return Map.of("achievement", saved, "occurredDateWarning", warning,
                "warningMessage", warning ? "업적발생일이 평가대상 기간 밖입니다." : "");
    }

    private void requireEditable(Map<String, Object> row) {
        String state = (String) row.get("achievementStatus");
        if ("EVALUATION_CONFIRMED".equals(state)) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 변경할 수 없습니다.");
        }
        if (!Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED").contains(state)) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 현재 상태에서는 수정할 수 없습니다.");
        }
    }

    /** Revalidates DB scope, input period, finalization and dynamic field policy before any mutation. */
    public Map<String, Object> prepare(
            EmploymentRateAchievementRequest request, Long teacher, String year, CurrentUser actor, String requestId) {
        requireScope(actor, teacher);
        if (request.managementItemCode() == null || request.managementItemCode().isBlank()) {
            invalid("managementItemCode", "관리항목을 입력하세요.");
        }
        if (request.achievementDate() == null || request.achievementName() == null
                || request.achievementName().isBlank()) {
            invalid("achievementName", "발생일과 실적명을 입력하세요.");
        }
        String org = mapper.organization(teacher);
        if (org == null) {
            throw new ForbiddenException();
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("teacherUserId", teacher);
        p.put("evaluationYear", year);
        p.put("organizationCode", org);
        p.put("managementItemCode", request.managementItemCode().trim());
        p.put("achievementDate", request.achievementDate());
        p.put("achievementName", request.achievementName().trim());
        p.put("actorId", actor.userId());
        p.put("requestId", requestId);
        mapper.lockFinalizations(p);
        if (guards.countEvaluationConfirmations(teacher, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터입니다.");
        }
        if (guards.countActiveInputPeriods(year, teacher) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        Map<String, Object> setting = mapper.setting(p);
        if (setting == null) {
            invalid("managementItemCode", "교원 입력 가능한 관리항목 설정이 없습니다.");
        }
        JsonNode detail = request.achievementDetail();
        if (detail != null && !detail.isObject()) {
            invalid("achievementDetail", "상세는 객체여야 합니다.");
        }
        if (detail != null) {
            String part = String.valueOf(setting.get("teacherEditablePart"));
            Set<String> fields = Set.of(part.split("[,|]"));
            detail.fieldNames().forEachRemaining(field -> {
                if (!fields.contains(field)) {
                    invalid("achievementDetail", "관리항목 설정에서 허용되지 않은 필드입니다: " + field);
                }
            });
        }
        String attachment = request.attachmentRef();
        if (attachment != null && !attachment.isBlank()) {
            invalid("attachmentRef", "외부 첨부 참조는 허용하지 않습니다. 등록된 첨부 연동이 필요합니다.");
        }
        p.put("attachmentRef", null);
        p.put("achievementDetail", detail == null ? "{}" : detail.toString());
        return p;
    }

    public void checkDuplicate(Map<String, Object> parameters) {
        if (mapper.duplicates(parameters) > 0) {
            throw new ConflictException("DUPLICATE_DATA: 동일 실적이 이미 존재합니다.");
        }
    }

    /** Caller owns the transaction; upload commits use this after all rows have been revalidated. */
    public void write(Map<String, Object> parameters, Map<String, Object> before) {
        if (before == null) {
            mapper.insert(parameters);
            mapper.statusHistory(parameters);
        } else if (mapper.update(parameters) != 1) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 저장 중 상태가 변경되었습니다.");
        }
        Map<String, Object> audit = new LinkedHashMap<>(parameters);
        audit.put("targetKey", String.valueOf(parameters.get("achievementId")));
        audit.put("changeType", before == null ? "CREATE" : "UPDATE");
        audit.put("beforeValue", before == null ? null : serialize(before));
        audit.put("afterValue", serialize(parameters));
        mapper.audit(audit);
    }

    public String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("입력값을 처리할 수 없습니다.");
        }
    }

    public static void invalid(String field, String message) {
        throw new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }

    public void createBulk(EmploymentRateBulkJobRequest body, CurrentUser user) {
        requireRole(user, "R07");
        throw new ConflictException("BULK_POLICY_NOT_APPROVED: 생성 자격 및 삭제 허용 상태 정책이 승인되지 않았습니다.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> job(String id, CurrentUser user) {
        requireRole(user, "R07");
        Map<String, Object> job = mapper.job(id);
        if (job == null) {
            throw new NotFoundException("일괄 작업을 찾을 수 없습니다.");
        }
        if (!user.roles().contains("R09") && !user.userId().equals(job.get("requesterUserId"))) {
            throw new ForbiddenException();
        }
        Map<String, Object> result = new LinkedHashMap<>(job);
        result.put("items", mapper.jobItems(id));
        return result;
    }
}
