package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.educationachievements.EducationAchievementAccessPolicy;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns caller-scoped reads and atomic header/detail/history writes for lecture improvements. */
@Service
public class LectureImprovementService {
    private static final String SCREEN = "SCR-LECTURE-IMPROVEMENTS";
    private static final String ROUTE = "/faculty/education/lecture-improvements";
    private static final Set<String> READ = Set.of("R01", "R02", "R04");
    private final LectureImprovementMapper mapper;
    private final EducationAchievementAccessPolicy access;
    private final EducationAchievementGuardMapper guards;
    private final ObjectMapper json;

    public LectureImprovementService(LectureImprovementMapper mapper, EducationAchievementAccessPolicy access,
            EducationAchievementGuardMapper guards, ObjectMapper json) {
        this.mapper = mapper;
        this.access = access;
        this.guards = guards;
        this.json = json;
    }

    /** Applies the same normalized filters and role-scope union to rows and total. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(int page, int pageSize, String code, String status, CurrentUser user) {
        access.requireRead(user);
        access.requireFunction(user, SCREEN, ROUTE, "READ", null, READ);
        if (page < 0 || !Set.of(20, 50, 100).contains(pageSize)) {
            throw invalid("pageSize", "페이지와 표시 건수를 확인하세요.");
        }
        var criteria = new LectureImprovementSearchCriteria(page, pageSize, (long) page * pageSize,
                normalize(code), normalize(status));
        return new LectureImprovementSearchResponse(mapper.list(criteria, user.userId(), user.roles()),
                page, pageSize, mapper.count(criteria, user.userId(), user.roles()));
    }

    /** Detail admission uses the same union of scopes as the list SQL. */
    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long id, CurrentUser user) {
        access.requireRead(user);
        access.requireFunction(user, SCREEN, ROUTE, "READ", null, READ);
        LectureImprovementRow row = requireRow(mapper.find(id));
        access.requireReadScope(user, row.teacherUserId());
        return row;
    }

    /** Inserts the header using its generated key before detail and immutable initial status history. */
    @Transactional
    public LectureImprovementSaveResult create(LectureImprovementRequest body, CurrentUser user, String requestId) {
        access.requireMutation(user, user.userId());
        validate(body);
        String year = body.evaluationYear() == null ? body.academicYear().toString() : body.evaluationYear();
        mapper.lockTeacher(user.userId());
        String organization = mapper.organization(user.userId());
        boolean warning = guard(user.userId(), year, body);
        access.requireFunction(user, SCREEN, ROUTE, "CREATE", "DRAFT", Set.of("R01"));
        validateReferences(body, year, organization);
        Map<String, Object> command = command(body, user.userId(), organization, year, user, requestId);
        try {
            mapper.insertHeader(command);
        } catch (DuplicateKeyException duplicate) {
            throw new ConflictException("DUPLICATE_ACHIEVEMENT: 동일한 실적이 이미 있습니다.");
        }
        Long id = ((Number) command.get("id")).longValue();
        mapper.insertDetail(id, body);
        mapper.insertStatusHistory(id, user.userId(), requestId);
        LectureImprovementRow saved = requireRow(mapper.find(id));
        audit(null, saved, user, requestId);
        return result(saved, warning);
    }

    /** Locks before validation; owner and evaluation year are immutable while academic year is detail data. */
    @Transactional
    public LectureImprovementSaveResult update(Long id, LectureImprovementRequest body,
            CurrentUser user, String requestId) {
        access.requireMutation(user, user.userId());
        LectureImprovementRow old = requireRow(mapper.lock(id));
        access.requireMutation(user, old.teacherUserId());
        access.requireMutableStatus(old.achievementStatus());
        if (!Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED").contains(old.achievementStatus())) {
            throw new ConflictException("STATUS_NOT_EDITABLE: 작성 또는 반려 상태에서만 수정할 수 있습니다.");
        }
        validate(body);
        if (body.evaluationYear() != null && !old.evaluationYear().equals(body.evaluationYear())) {
            throw invalid("evaluationYear", "수정 시 평가연도는 변경할 수 없습니다.");
        }
        mapper.lockTeacher(old.teacherUserId());
        boolean warning = guard(old.teacherUserId(), old.evaluationYear(), body);
        access.requireFunction(user, SCREEN, ROUTE, "UPDATE", old.achievementStatus(), Set.of("R01"));
        validateReferences(body, old.evaluationYear(), old.organizationCode());
        Map<String, Object> command = command(body, old.teacherUserId(), old.organizationCode(),
                old.evaluationYear(), user, requestId);
        command.put("id", id);
        try {
            if (mapper.updateHeader(command) != 1) {
                throw new ConflictException("STATUS_NOT_EDITABLE: 실적 상태가 변경되었습니다.");
            }
        } catch (DuplicateKeyException duplicate) {
            throw new ConflictException("DUPLICATE_ACHIEVEMENT: 동일한 실적이 이미 있습니다.");
        }
        mapper.updateDetail(id, body);
        LectureImprovementRow saved = requireRow(mapper.find(id));
        audit(old, saved, user, requestId);
        return result(saved, warning);
    }

    // Reuse the existing period/finalization queries, without the legacy guard's narrower R09 admission.
    private boolean guard(Long teacher, String year, LectureImprovementRequest body) {
        if (guards.countActiveInputPeriods(year, teacher) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        if (guards.countEvaluationConfirmations(teacher, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 대상자의 평가가 확정되었습니다.");
        }
        return guards.countEvaluationDatePeriods(year, teacher, body.achievementDate()) == 0;
    }

    private void validateReferences(LectureImprovementRequest body, String year, String organization) {
        if (mapper.validSemester(body.semester().toString()) == 0) {
            throw invalid("semester", "사용 가능한 학기 코드를 선택하세요.");
        }
        if (organization == null || mapper.validManagementItem(body.managementItemCode().trim(), year,
                organization) == 0) {
            throw invalid("managementItemCode", "해당 평가연도·소속에서 입력 가능한 관리항목을 선택하세요.");
        }
    }

    private void validate(LectureImprovementRequest body) {
        if (body == null) throw invalid("body", "실적 정보를 입력하세요.");
        if (normalize(body.managementItemCode()) == null) throw invalid("managementItemCode", "관리항목은 필수입니다.");
        if (body.achievementDate() == null) throw invalid("achievementDate", "발생일은 필수입니다.");
        if (normalize(body.achievementContent()) == null) throw invalid("achievementContent", "실적내용은 필수입니다.");
        if (body.academicYear() == null || body.academicYear() < 2000 || body.academicYear() > 9999) {
            throw invalid("academicYear", "학년도는 2000 이상 네 자리 정수입니다.");
        }
        if (body.semester() == null || body.semester() < 1 || body.semester() > 2) {
            throw invalid("semester", "학기는 1 또는 2입니다.");
        }
        if (body.evaluationYear() != null && !body.evaluationYear().matches("[0-9]{4}")) {
            throw invalid("evaluationYear", "평가연도는 네 자리입니다.");
        }
    }

    private Map<String, Object> command(LectureImprovementRequest body, Long teacher, String organization,
            String year, CurrentUser user, String requestId) {
        Map<String, Object> command = new HashMap<>();
        command.put("teacherUserId", teacher);
        command.put("organizationCode", organization);
        command.put("evaluationYear", year);
        command.put("managementItemCode", body.managementItemCode().trim());
        command.put("achievementDate", body.achievementDate());
        command.put("attachmentIds", serialize(body.attachmentIds() == null ? List.of() : body.attachmentIds()));
        command.put("userId", user.userId());
        command.put("requestId", requestId);
        return command;
    }

    private void audit(LectureImprovementRow old, LectureImprovementRow saved, CurrentUser user, String requestId) {
        Map<String, Object> before = old == null ? Map.of() : values(old);
        values(saved).forEach((field, value) -> {
            Object previous = before.get(field);
            if (old == null || !Objects.equals(previous, value)) {
                mapper.insertChangeHistory(saved.achievementId(), old == null ? "CREATE" : "UPDATE", field,
                        previous == null ? null : serialize(previous), serialize(value), user.userId(), requestId);
            }
        });
    }

    private Map<String, Object> values(LectureImprovementRow row) {
        return Map.of("managementItemCode", row.managementItemCode(), "achievementDate", row.achievementDate().toString(),
                "achievementContent", row.achievementContent(), "academicYear", row.academicYear(),
                "semester", row.semester(), "attachmentIds", row.attachmentIds(), "evaluationYear", row.evaluationYear());
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw invalid("body", "실적 입력값을 확인하세요.");
        }
    }

    private LectureImprovementRow requireRow(LectureImprovementRow row) {
        if (row == null) throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        return row;
    }

    private LectureImprovementSaveResult result(LectureImprovementRow row, boolean warning) {
        return new LectureImprovementSaveResult(row, warning,
                warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 완료되었습니다." : null);
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
