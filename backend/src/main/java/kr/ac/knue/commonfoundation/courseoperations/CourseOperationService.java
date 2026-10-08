package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.common.educationachievements.EducationAchievementAccessPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Course operation reads and atomic header/detail/audit writes through foundation authorization. */
@Service
public class CourseOperationService {
    private static final Set<String> READ = Set.of("R01", "R02", "R04");
    private static final Set<String> WRITE = Set.of("R01");
    private static final Set<String> EDITABLE = Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED");
    private final CourseOperationMapper mapper;
    private final EducationAchievementAccessPolicy access;
    private final EducationAchievementGuardMapper guard;
    private final ObjectMapper json;

    public CourseOperationService(
            CourseOperationMapper mapper,
            EducationAchievementAccessPolicy access,
            EducationAchievementGuardMapper guard,
            ObjectMapper json) {
        this.mapper = mapper;
        this.access = access;
        this.guard = guard;
        this.json = json;
    }

    /** List and count share the same normalized filters and union of admitted scopes. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(CourseOperationSearchCriteria criteria, CurrentUser user) {
        access.requireRead(user);
        function(user, "READ", null, READ);
        if (criteria.page() < 0 || !List.of(20, 50, 100).contains(criteria.pageSize())) {
            invalid("pageSize", "페이지와 표시 건수를 확인하세요.");
        }
        CourseOperationSearchCriteria safe = new CourseOperationSearchCriteria(
                criteria.page(), criteria.pageSize(), (long) criteria.page() * criteria.pageSize(),
                trim(criteria.managementItemCode()), trim(criteria.evaluationYear()),
                trim(criteria.achievementStatus()), trim(criteria.teacherName()));
        return new CourseOperationSearchResponse(
                mapper.list(safe, user.userId(), user.roles()), safe.page(), safe.pageSize(),
                mapper.count(safe, user.userId(), user.roles()));
    }

    /** Detail enforces the same union of ownership/department/certification scopes as list SQL. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long id, CurrentUser user) {
        access.requireRead(user);
        function(user, "READ", null, READ);
        CourseOperationRow row = required(mapper.find(id));
        access.requireReadScope(user, row.teacherUserId());
        return row;
    }

    /** Creates only, using the generated header key before inserting its mandatory detail. */
    @Transactional
    public CourseOperationSaveResult create(CourseOperationRequest body, CurrentUser user, String requestId) {
        access.requireMutation(user, user == null ? null : user.userId());
        validate(body);
        String year = body.evaluationYear() == null ? mapper.activeYear(user.userId()) : body.evaluationYear();
        String organization = mapper.organization(user.userId());
        guards(user.userId(), year);
        function(user, "CREATE", "DRAFT", WRITE);
        return persist(null, normalized(body), user, requestId, user.userId(), year, organization);
    }

    /** Locks the selected row, preserves its owner/year/status, and audits full before/after snapshots. */
    @Transactional
    public CourseOperationSaveResult update(
            Long id, CourseOperationRequest body, CurrentUser user, String requestId) {
        access.requireMutation(user, user == null ? null : user.userId());
        CourseOperationRow before = required(mapper.lock(id));
        access.requireMutation(user, before.teacherUserId());
        access.requireMutableStatus(before.achievementStatus());
        if (!EDITABLE.contains(before.achievementStatus())) {
            throw new ConflictException("STATE_NOT_EDITABLE: 작성 또는 반려 상태만 수정할 수 있습니다.");
        }
        guards(before.teacherUserId(), before.evaluationYear());
        function(user, "UPDATE", before.achievementStatus(), WRITE);
        validate(body);
        return persist(before, normalized(body), user, requestId,
                before.teacherUserId(), before.evaluationYear(), before.organizationCode());
    }

    private CourseOperationSaveResult persist(
            CourseOperationRow before, CourseOperationRequest body, CurrentUser user,
            String requestId, Long owner, String year, String organization) {
        if (organization == null || mapper.editableItem(body.managementItemCode(), year, organization) == 0) {
            invalid("managementItemCode", "해당 평가연도·소속에서 입력 가능한 관리항목이 아닙니다.");
        }
        Long id = before == null ? null : before.achievementId();
        if (mapper.duplicate(owner, year, body, id) > 0) {
            throw new ConflictException("DUPLICATE_ACHIEVEMENT: 동일한 실적이 이미 있습니다.");
        }
        Map<String, Object> values = new HashMap<>();
        values.put("id", id);
        values.put("owner", owner);
        values.put("year", year);
        values.put("organization", organization);
        values.put("body", body);
        values.put("actor", user.userId());
        values.put("requestId", requestId);
        values.put("attachments", serialize(body.attachmentIds()));
        try {
            if (before == null) {
                mapper.insertHeader(values);
                id = ((Number) values.get("id")).longValue();
                mapper.insertDetail(id, body.performanceDetails());
                mapper.statusHistory(id, user.userId(), requestId);
            } else {
                if (mapper.updateHeader(values) != 1) {
                    throw new ConflictException("STATE_NOT_EDITABLE: 실적 상태가 변경되었습니다.");
                }
                mapper.updateDetail(id, body.performanceDetails());
            }
        } catch (DuplicateKeyException conflict) {
            throw new ConflictException("DUPLICATE_ACHIEVEMENT: 동일한 실적이 이미 있습니다.");
        }
        CourseOperationRow saved = required(mapper.find(id));
        mapper.changeHistory(id, before == null ? "CREATE" : "UPDATE",
                before == null ? null : serialize(before), serialize(saved), user.userId(), requestId);
        boolean warning = guard.countEvaluationDatePeriods(year, owner, body.achievementDate()) == 0;
        return new CourseOperationSaveResult(saved, warning,
                warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 허용되었습니다." : null);
    }

    private void guards(Long owner, String year) {
        if (year == null || guard.countActiveInputPeriods(year, owner) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        if (guard.countEvaluationConfirmations(owner, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 대상자의 평가가 확정되었습니다.");
        }
    }

    private void function(CurrentUser user, String type, String status, Set<String> roles) {
        access.requireFunction(user, "SCR-COURSE-OPERATIONS", "/faculty/education/course-operations",
                type, status, roles);
    }

    private CourseOperationRow required(CourseOperationRow row) {
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void validate(CourseOperationRequest body) {
        if (body == null || trim(body.managementItemCode()) == null) {
            invalid("managementItemCode", "관리항목을 입력하세요.");
        }
        if (body.achievementDate() == null) {
            invalid("achievementDate", "업적발생일을 입력하세요.");
        }
        if (trim(body.performanceDetails()) == null) {
            invalid("performanceDetails", "실적내역을 입력하세요.");
        }
        if (body.attachmentIds() != null
                && body.attachmentIds().stream().anyMatch(value -> trim(value) == null)) {
            invalid("attachmentIds", "첨부 식별자를 확인하세요.");
        }
    }

    private CourseOperationRequest normalized(CourseOperationRequest body) {
        return new CourseOperationRequest(body.managementItemCode().trim(), body.achievementDate(),
                body.performanceDetails().trim(), body.attachmentIds() == null ? List.of() : body.attachmentIds(),
                body.evaluationYear());
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("실적 이력 직렬화 실패", exception);
        }
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void invalid(String field, String message) {
        throw new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }
}
