package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped reads and atomic employment-improvement header/detail/history commands. */
@Service
public class EmploymentRateImprovementService {
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardService guard;
    private final EducationAchievementGuardMapper scope;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper,
            EducationAchievementGuardService guard,
            EducationAchievementGuardMapper scope) {
        this.mapper = mapper;
        this.guard = guard;
        this.scope = scope;
    }

    /** Applies identical scope and filters to rows and total; options come from teacher-editable settings. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            int page, int pageSize, String item, String state, CurrentUser user) {
        requireRole(user, false);
        if (page < 0 || !List.of(20, 50, 100).contains(pageSize)) {
            invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        var criteria = new EmploymentRateImprovementSearchCriteria(
                page, pageSize, (long) page * pageSize, normalize(item), normalize(state));
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(criteria, user.userId(), user.roles()), page, pageSize,
                mapper.count(criteria, user.userId(), user.roles()),
                mapper.managementItems(user.userId(), null));
    }

    /** Detail reads use the union of own, department and certification scopes, just as list does. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        var row = existing(id, false);
        requireScope(row, user);
        return row;
    }

    /** Creates only; generated header key is used before inserting the detail and histories. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest body, CurrentUser user, String trace) {
        return save(null, body, user, trace);
    }

    /** Keeps the persisted evaluation year and owner immutable while changing editable business fields. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(
            Long id, EmploymentRateImprovementRequest body, CurrentUser user, String trace) {
        return save(id, body, user, trace);
    }

    private EmploymentRateImprovementSaveResult save(
            Long id, EmploymentRateImprovementRequest body, CurrentUser user, String trace) {
        requireRole(user, true);
        validate(body);
        var old = id == null ? null : existing(id, true);
        if (old != null && !admin(user) && !old.teacherUserId().equals(user.userId())) {
            throw new ForbiddenException();
        }
        Long owner = old == null ? user.userId() : old.teacherUserId();
        String year = old == null ? String.valueOf(body.achievementDate().getYear()) : old.evaluationYear();
        // Serialize competing feature writes for the target and lock existing finalization records.
        mapper.lockTeacher(owner);
        mapper.lockFinalizations(owner, year);
        OccurredDateValidation warning;
        if (admin(user)) {
            if (scope.countActiveInputPeriods(year, owner) == 0) {
                throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
            }
            if (scope.countEvaluationConfirmations(owner, year) > 0) {
                throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적입니다.");
            }
            warning = scope.countEvaluationDatePeriods(year, owner, body.achievementDate()) == 0
                    ? OccurredDateValidation.outsideEvaluationPeriod() : OccurredDateValidation.accepted();
        } else {
            warning = guard.validateMutation(user,
                    new EducationAchievementMutationContext(owner, year, body.achievementDate()));
        }
        if (old != null && !List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED")
                .contains(old.achievementStatus())) {
            String code = "EVALUATION_CONFIRMED".equals(old.achievementStatus())
                    ? "CONFIRMED_DATA_LOCKED" : "INVALID_STATE_TRANSITION";
            throw new ConflictException(code + ": 현재 상태는 수정할 수 없습니다.");
        }
        if (mapper.allowedItem(owner, year, body.managementItemCode().trim()) == 0) {
            invalid("managementItemCode", "해당 평가연도에 교원이 입력할 수 있는 관리항목이 아닙니다.");
        }
        Map<String, Object> values = new HashMap<>();
        values.put("id", id);
        values.put("owner", owner);
        values.put("year", year);
        values.put("body", body);
        values.put("actor", user.userId());
        values.put("trace", trace);
        if (old == null) {
            String organization = mapper.organization(owner);
            if (organization == null) throw new ForbiddenException();
            values.put("organization", organization);
            mapper.insertHeader(values);
            id = ((Number) values.get("id")).longValue();
            mapper.insertDetail(id, body);
            mapper.statusHistory(id, user.userId(), trace);
        } else {
            if (mapper.updateHeader(values) != 1) {
                throw new ConflictException("INVALID_STATE_TRANSITION: 변경 중 상태가 바뀌었습니다.");
            }
            mapper.updateDetail(id, body);
        }
        var saved = existing(id, false);
        audit(id, old, saved, user.userId(), trace);
        return new EmploymentRateImprovementSaveResult(saved, warning.warning(), warning.message());
    }

    private void audit(Long id, EmploymentRateImprovementRow old,
            EmploymentRateImprovementRow saved, Long actor, String trace) {
        Map<String, Object> before = fields(old);
        for (var entry : fields(saved).entrySet()) {
            Object previous = before.get(entry.getKey());
            if (old == null || !Objects.equals(previous, entry.getValue())) {
                mapper.changeHistory(id, old == null ? "CREATE" : "UPDATE", entry.getKey(),
                        previous == null ? null : previous.toString(),
                        entry.getValue() == null ? null : entry.getValue().toString(), actor, trace);
            }
        }
    }

    private Map<String, Object> fields(EmploymentRateImprovementRow row) {
        Map<String, Object> values = new HashMap<>();
        if (row == null) return values;
        values.put("management_item_code", row.managementItemCode());
        values.put("achievement_date", row.achievementDate());
        values.put("special_lecture_start_date", row.specialLectureStartDate());
        values.put("special_lecture_end_date", row.specialLectureEndDate());
        values.put("mock_exam_question_period", row.mockExamQuestionPeriod());
        values.put("attachment_ref", row.attachmentRef());
        return values;
    }

    private EmploymentRateImprovementRow existing(Long id, boolean lock) {
        var row = mapper.find(id, lock);
        if (row == null) throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        return row;
    }

    private void requireScope(EmploymentRateImprovementRow row, CurrentUser user) {
        if (admin(user)
                || (user.roles().contains("R01") && row.teacherUserId().equals(user.userId()))
                || (user.roles().contains("R02")
                    && scope.countSharedActiveOrganization(user.userId(), row.teacherUserId()) > 0)
                || (user.roles().contains("R04")
                    && scope.countCertificationScope(user.userId(), row.teacherUserId()) > 0)) return;
        throw new ForbiddenException();
    }

    static void requireRole(CurrentUser user, boolean write) {
        if (user == null) throw new UnauthenticatedException();
        var allowed = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) {
            throw new ForbiddenException();
        }
    }

    private boolean admin(CurrentUser user) {
        return user.roles().contains("R09");
    }

    private void validate(EmploymentRateImprovementRequest body) {
        if (body == null) invalid("body", "입력값이 필요합니다.");
        if (normalize(body.managementItemCode()) == null) invalid("managementItemCode", "관리항목은 필수입니다.");
        if (body.achievementDate() == null) invalid("achievementDate", "업적발생일은 필수입니다.");
        if ((body.specialLectureStartDate() == null) != (body.specialLectureEndDate() == null)
                || (body.specialLectureStartDate() != null
                    && body.specialLectureEndDate().isBefore(body.specialLectureStartDate()))) {
            invalid("specialLectureEndDate", "특강 시작일과 종료일을 입력하고 기간 순서를 확인하세요.");
        }
    }

    private void invalid(String field, String message) {
        throw new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
