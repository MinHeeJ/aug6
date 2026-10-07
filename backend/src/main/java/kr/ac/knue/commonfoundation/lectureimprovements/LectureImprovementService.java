package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.common.storage.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped lecture-improvement reads and atomic header/detail/snapshot writes. */
@Service
public class LectureImprovementService {
    private static final String SCREEN = "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";
    private final LectureImprovementMapper mapper;
    private final EducationAchievementGuardMapper guards;
    private final FunctionPermissionService permissions;
    private final FileStoragePort files;
    private final ObjectMapper json;
    private final Validator validator;

    public LectureImprovementService(LectureImprovementMapper mapper,
            EducationAchievementGuardMapper guards, FunctionPermissionService permissions,
            FileStoragePort files, ObjectMapper json, Validator validator) {
        this.mapper = mapper;
        this.guards = guards;
        this.permissions = permissions;
        this.files = files;
        this.json = json;
        this.validator = validator;
    }

    /** Uses the identical mapper predicate for the rows and total, including multi-role scope union. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(LectureImprovementSearchCriteria criteria, CurrentUser user) {
        requireRole(user, false);
        if (criteria.page() < 0 || !Set.of(20, 50, 100).contains(criteria.pageSize())) {
            invalid("pageSize", "페이지와 표시 건수를 확인하세요.");
        }
        function(user, "READ", "DRAFT");
        return new LectureImprovementSearchResponse(
                mapper.list(criteria, user.userId(), user.roles()).stream().map(this::materialize).toList(),
                criteria.page(), criteria.pageSize(), mapper.count(criteria, user.userId(), user.roles()),
                mapper.managementItems(user.userId(), String.valueOf(LocalDate.now().getYear())));
    }

    /** Checks detail ownership with the same allowed role union used by list. */
    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id, CurrentUser user) {
        requireRole(user, false);
        Map<String, Object> row = find(id, false);
        scope(user, ((Number) row.get("teacherUserId")).longValue());
        function(user, "READ", (String) row.get("achievementStatus"));
        return materialize(row);
    }

    /** Creates only, using the generated header key before writing detail and initial history. */
    @Transactional
    public LectureImprovementSaveResult create(LectureImprovementRequest body, CurrentUser user, String requestId) {
        return mutate(null, body, user, requestId);
    }

    /** Updates only; owner/year are immutable, and a locked non-DRAFT row cannot be mutated. */
    @Transactional
    public LectureImprovementSaveResult update(Long id, LectureImprovementRequest body,
            CurrentUser user, String requestId) {
        return mutate(id, body, user, requestId);
    }

    private LectureImprovementSaveResult mutate(Long id, LectureImprovementRequest body,
            CurrentUser user, String requestId) {
        requireRole(user, true);
        var errors = validator.validate(body).stream()
                .map(v -> new ValidationError(v.getPropertyPath().toString(), v.getMessage())).toList();
        if (!errors.isEmpty()) throw new BusinessValidationException("입력값을 확인하세요.", errors);
        Map<String, Object> before = id == null ? null : find(id, true);
        Long owner = before == null ? user.userId() : ((Number) before.get("teacherUserId")).longValue();
        if (!user.roles().contains("R09") && !owner.equals(user.userId())) throw new ForbiddenException();
        String year = before == null ? String.valueOf(body.achievementDate().getYear())
                : (String) before.get("evaluationYear");
        if (guards.countActiveInputPeriods(year, owner) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        // Business 409 precedes function-permission evaluation, which otherwise reports 403 on confirmed data.
        if ((before != null && "EVALUATION_CONFIRMED".equals(before.get("achievementStatus")))
                || guards.countEvaluationConfirmations(owner, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        if (before != null && !"DRAFT".equals(before.get("achievementStatus"))) {
            throw new ConflictException("작성중 실적만 수정할 수 있습니다.");
        }
        function(user, before == null ? "CREATE" : "UPDATE",
                before == null ? "DRAFT" : (String) before.get("achievementStatus"));
        String code = body.managementItemCode().trim();
        long matches = mapper.managementItems(owner, year).stream()
                .filter(m -> code.equals(m.get("managementItemCode"))).count();
        if (matches != 1) invalid("managementItemCode", "활성 교육영역 관리항목을 선택하세요. 중복 항목은 사용할 수 없습니다.");
        List<String> attachmentIds = body.attachmentIds() == null ? List.of() : body.attachmentIds();
        for (String fileId : attachmentIds) {
            try {
                if (files.find(fileId, owner) == null) invalid("attachmentIds", "본인 소유의 실제 첨부파일만 참조할 수 있습니다.");
            } catch (IOException | IllegalArgumentException exception) {
                invalid("attachmentIds", "본인 소유의 실제 첨부파일만 참조할 수 있습니다.");
            }
        }
        Map<String, Object> values = new HashMap<>();
        values.put("id", id);
        values.put("body", new LectureImprovementRequest(code, body.achievementDate(),
                body.achievementContent().trim(), body.academicYear(), body.semester(), attachmentIds));
        values.put("actor", user.userId());
        values.put("owner", owner);
        values.put("year", year);
        values.put("requestId", requestId);
        values.put("attachments", serialize(attachmentIds));
        if (before == null) {
            String organization = mapper.organization(owner);
            if (organization == null) invalid("teacherUserId", "활성 소속 조직이 필요합니다.");
            values.put("organization", organization);
            values.put("managementNo", "LI-" + UUID.randomUUID());
            mapper.insertHeader(values);
            mapper.insertDetail(values);
            mapper.insertStatus(values);
        } else {
            if (mapper.updateHeader(values) != 1) throw new ConflictException("실적 상태가 변경되었습니다.");
            mapper.updateDetail(values);
        }
        Map<String, Object> saved = materialize(find(((Number) values.get("id")).longValue(), false));
        values.put("changeType", before == null ? "CREATE" : "UPDATE");
        values.put("before", before == null ? null : serialize(materialize(before)));
        values.put("after", serialize(saved));
        mapper.insertHistory(values);
        boolean warning = guards.countEvaluationDatePeriods(year, owner, body.achievementDate()) == 0;
        return new LectureImprovementSaveResult(saved, warning,
                warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 완료되었습니다." : null);
    }

    private Map<String, Object> find(Long id, boolean lock) {
        Map<String, Object> row = mapper.find(id, lock);
        if (row == null) throw new NotFoundException("강의개선 실적이 없습니다.");
        return row;
    }

    private void scope(CurrentUser user, Long owner) {
        if (user.roles().contains("R09")
                || (user.roles().contains("R01") && owner.equals(user.userId()))
                || (user.roles().contains("R02") && guards.countSharedActiveOrganization(user.userId(), owner) > 0)
                || (user.roles().contains("R04") && guards.countCertificationScope(user.userId(), owner) > 0)) return;
        throw new ForbiddenException();
    }

    private void function(CurrentUser user, String operation, String status) {
        // The explicit request-level administrator bypass applies only to this feature.
        if (user.roles().contains("R09")) return;
        for (String role : user.roles()) {
            if (!("R01".equals(role) || ("READ".equals(operation) && Set.of("R02", "R04").contains(role)))) continue;
            try {
                if (permissions.evaluate(new FunctionPermissionEvaluateRequest(
                        SCREEN, role, operation, status, null)).allowed()) return;
            } catch (ForbiddenException exception) {
                // Another admitted role may grant the operation.
            }
        }
        throw new ForbiddenException();
    }

    static void requireRole(CurrentUser user, boolean write) {
        if (user == null) throw new UnauthenticatedException();
        Set<String> allowed = write ? Set.of("R01", "R09") : Set.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) throw new ForbiddenException();
    }

    private Map<String, Object> materialize(Map<String, Object> row) {
        Map<String, Object> copy = new HashMap<>(row);
        if (copy.get("attachmentIds") instanceof String raw) {
            try {
                copy.put("attachmentIds", json.readValue(raw, new TypeReference<List<String>>() {}));
            } catch (IOException exception) {
                throw new IllegalStateException("첨부 참조 형식이 올바르지 않습니다.", exception);
            }
        }
        return copy;
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalStateException("변경이력 직렬화에 실패했습니다.", exception);
        }
    }

    private void invalid(String field, String message) {
        throw new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }
}
