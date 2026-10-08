package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.educationachievements.EducationAchievementAccessPolicy;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped reads and atomic normalized header/detail/history writes for employment improvements. */
@Service
public class EmploymentRateImprovementService {
    private static final String SCREEN = "SCR-EMPLOYMENT-RATE-IMPROVEMENTS";
    private static final String ROUTE = "/faculty/education/employment-rate-improvements";
    private static final Set<String> READ = Set.of("R01", "R02", "R04");
    private static final Set<String> WRITE = Set.of("R01");
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementAccessPolicy access;
    private final EducationAchievementGuardMapper guards;
    private final ObjectMapper json;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper, EducationAchievementAccessPolicy access,
            EducationAchievementGuardMapper guards, ObjectMapper json) {
        this.mapper = mapper;
        this.access = access;
        this.guards = guards;
        this.json = json;
    }

    /** List and count share the same SQL predicate including the union of role scopes. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            EmploymentRateImprovementSearchCriteria criteria, CurrentUser user) {
        access.requireRead(user);
        function(user, "READ", null, READ);
        if (criteria.page() < 0 || !List.of(20, 50, 100).contains(criteria.pageSize())) {
            throw invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        var safe = new EmploymentRateImprovementSearchCriteria(
                criteria.page(), criteria.pageSize(), (long) criteria.page() * criteria.pageSize(),
                trim(criteria.managementItemCode()), trim(criteria.evaluationYear()),
                trim(criteria.achievementStatus()), trim(criteria.teacherName()));
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(safe, user.userId(), user.roles()), safe.page(), safe.pageSize(),
                mapper.count(safe, user.userId(), user.roles()));
    }

    /** Detail enforces the same union of scopes as the list, never just role admission. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long id, CurrentUser user) {
        access.requireRead(user);
        function(user, "READ", null, READ);
        var row = required(mapper.find(id));
        access.requireReadScope(user, row.teacherUserId());
        return row;
    }

    /** POST always creates; generated keys are captured before the detail or joined read. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest request, CurrentUser user, String requestId) {
        access.requireMutation(user, user == null ? null : user.userId());
        validate(request);
        String year = request.evaluationYear() == null
                ? String.valueOf(request.achievementDate().getYear()) : request.evaluationYear();
        mapper.lockTeacher(user.userId());
        boolean warning = guard(user.userId(), year, request);
        function(user, "CREATE", "DRAFT", WRITE);
        var write = command(null, request, user.userId(), year, user, requestId);
        validateItemAndDuplicate(write);
        if (mapper.insertHeader(write) != 1 || write.getAchievementId() == null) {
            throw new ConflictException("실적 대상자의 활성 소속을 확인하세요.");
        }
        mapper.insertDetail(write);
        var saved = required(mapper.find(write.getAchievementId()));
        mapper.insertStatusHistory(saved.achievementId(), user.userId(), requestId);
        history(null, saved, user, requestId);
        return result(saved, warning);
    }

    /** PUT retains owner/year/status and locks the existing row before validating all mutations. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(
            Long id, EmploymentRateImprovementRequest request, CurrentUser user, String requestId) {
        access.requireMutation(user, user == null ? null : user.userId());
        var existing = required(mapper.lock(id));
        access.requireMutation(user, existing.teacherUserId());
        access.requireMutableStatus(existing.achievementStatus());
        if (!Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED")
                .contains(existing.achievementStatus())) {
            throw new ConflictException("STATUS_NOT_EDITABLE: 현재 상태에서는 수정할 수 없습니다.");
        }
        validate(request);
        mapper.lockTeacher(existing.teacherUserId());
        boolean warning = guard(existing.teacherUserId(), existing.evaluationYear(), request);
        function(user, "UPDATE", existing.achievementStatus(), WRITE);
        var write = command(id, request, existing.teacherUserId(), existing.evaluationYear(), user, requestId);
        validateItemAndDuplicate(write);
        if (mapper.updateHeader(write) != 1 || mapper.updateDetail(write) != 1) {
            throw new ConflictException("실적 변경 상태를 확인하세요.");
        }
        var saved = required(mapper.find(id));
        history(existing, saved, user, requestId);
        return result(saved, warning);
    }

    private boolean guard(Long teacherId, String year, EmploymentRateImprovementRequest request) {
        if (guards.countActiveInputPeriods(year, teacherId) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        mapper.lockFinalizations(teacherId, year);
        if (guards.countEvaluationConfirmations(teacherId, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 변경할 수 없습니다.");
        }
        return guards.countEvaluationDatePeriods(year, teacherId, request.achievementDate()) == 0;
    }

    private void function(CurrentUser user, String action, String status, Set<String> roles) {
        access.requireFunction(user, SCREEN, ROUTE, action, status, roles);
    }

    private void validate(EmploymentRateImprovementRequest request) {
        if (request == null) {
            throw invalid("body", "실적을 입력하세요.");
        }
        if (trim(request.managementItemCode()) == null) {
            throw invalid("managementItemCode", "관리항목을 입력하세요.");
        }
        if (request.achievementDate() == null) {
            throw invalid("achievementDate", "업적발생일을 입력하세요.");
        }
        if (request.evaluationYear() != null && !request.evaluationYear().matches("[0-9]{4}")) {
            throw invalid("evaluationYear", "평가연도는 YYYY 형식이어야 합니다.");
        }
        if (request.specialLectureStartDate() != null && request.specialLectureEndDate() != null
                && request.specialLectureEndDate().isBefore(request.specialLectureStartDate())) {
            throw invalid("specialLectureEndDate", "특강 종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    private void validateItemAndDuplicate(EmploymentRateImprovementWrite write) {
        if (mapper.countManagementItem(
                write.getRequest().managementItemCode().trim(), write.getEvaluationYear()) == 0) {
            throw invalid("managementItemCode", "해당 평가연도의 사용 가능한 교원 입력 관리항목이 아닙니다.");
        }
        if (mapper.countDuplicate(write) > 0) {
            throw new ConflictException("DUPLICATE_ACHIEVEMENT: 같은 대상·연도·관리항목·발생일 실적이 있습니다.");
        }
    }

    private EmploymentRateImprovementWrite command(
            Long id, EmploymentRateImprovementRequest request, Long teacherId,
            String year, CurrentUser user, String requestId) {
        return new EmploymentRateImprovementWrite(id, request, teacherId, year, user.userId(), requestId,
                serialize(request.achievementDetail() == null ? json.createObjectNode() : request.achievementDetail()),
                serialize(request.attachmentIds() == null ? List.of() : request.attachmentIds()));
    }

    private void history(
            EmploymentRateImprovementRow before, EmploymentRateImprovementRow after,
            CurrentUser user, String requestId) {
        var oldValues = before == null ? json.createObjectNode() : json.valueToTree(before);
        var newValues = json.valueToTree(after);
        for (String field : List.of("managementItemCode", "achievementDate", "achievementName", "achievementDetail",
                "attachmentRef", "attachmentIds", "specialLectureStartDate", "specialLectureEndDate",
                "mockExamQuestionPeriod")) {
            if (before == null || !Objects.equals(oldValues.get(field), newValues.get(field))) {
                mapper.insertChangeHistory(after.achievementId(), before == null ? "CREATE" : "UPDATE", field,
                        before == null ? null : serialize(oldValues.get(field)), serialize(newValues.get(field)),
                        user.userId(), requestId);
            }
        }
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw invalid("achievementDetail", "상세 입력값을 확인하세요.");
        }
    }

    private EmploymentRateImprovementRow required(EmploymentRateImprovementRow row) {
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private EmploymentRateImprovementSaveResult result(EmploymentRateImprovementRow row, boolean warning) {
        return new EmploymentRateImprovementSaveResult(row, warning,
                warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 완료되었습니다." : null);
    }

    private String trim(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException("실적 입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }
}
