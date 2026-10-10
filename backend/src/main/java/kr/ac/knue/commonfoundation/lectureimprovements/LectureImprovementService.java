package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped lecture reads and atomic parent/detail, lifecycle and complete snapshot writes. */
@Service
public class LectureImprovementService {
    public static final String SCREEN_ID = "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";
    private static final Set<String> EDITABLE = Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED");
    private final LectureImprovementMapper mapper;
    private final EducationAchievementGuardMapper guard;
    private final FunctionPermissionService permissions;
    private final ObjectMapper json;

    public LectureImprovementService(
            LectureImprovementMapper mapper,
            EducationAchievementGuardMapper guard,
            FunctionPermissionService permissions,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.permissions = permissions;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(LectureImprovementSearchCriteria input, CurrentUser user) {
        authorize(user, false);
        function(user, "READ");
        if (input.page() < 0 || !Set.of(20, 50, 100).contains(input.pageSize())
                || input.page() > Integer.MAX_VALUE / input.pageSize()) {
            invalid("pageSize", "페이지와 표시 건수를 확인하세요.");
        }
        LectureImprovementSearchCriteria criteria = new LectureImprovementSearchCriteria(
                input.page(), input.pageSize(), input.page() * input.pageSize(),
                text(input.managementNo()), text(input.teacherName()), text(input.managementItemCode()),
                text(input.achievementStatus()), text(input.evaluationYear()));
        return new LectureImprovementSearchResponse(
                mapper.list(criteria, user.userId(), user.roles()), criteria.page(), criteria.pageSize(),
                mapper.count(criteria, user.userId(), user.roles()), mapper.semesters(),
                mapper.managementItems(criteria.evaluationYear()));
    }

    @Transactional(readOnly = true)
    public LectureImprovementRow detail(Long id, CurrentUser user) {
        authorize(user, false);
        function(user, "READ");
        LectureImprovementRow row = existing(id, false);
        if (mapper.canRead(id, user.userId(), user.roles()) == 0) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Uses the insert-generated identity; no joined read happens until both parent and detail exist. */
    @Transactional
    public LectureImprovementSaveResult create(LectureImprovementRequest body, CurrentUser user, String requestId) {
        return mutate(null, body, user, requestId);
    }

    /** Locks the existing parent; owner, evaluation year and creation identity remain immutable. */
    @Transactional
    public LectureImprovementSaveResult update(
            Long id, LectureImprovementRequest body, CurrentUser user, String requestId) {
        return mutate(id, body, user, requestId);
    }

    private LectureImprovementSaveResult mutate(
            Long id, LectureImprovementRequest body, CurrentUser user, String requestId) {
        authorize(user, true);
        LectureImprovementRow before = id == null ? null : existing(id, true);
        boolean admin = user.roles().contains("R09");
        if (before != null && !admin && !before.teacherUserId().equals(user.userId())) {
            throw new ForbiddenException();
        }
        Long owner = before == null ? user.userId() : before.teacherUserId();
        String year = before == null
                ? (body.evaluationYear() == null ? String.valueOf(body.achievementDate().getYear()) : body.evaluationYear())
                : before.evaluationYear();
        if (guard.countActiveInputPeriods(year, owner) == 0) {
            conflict("PERIOD_NOT_ACTIVE", "활성 입력기간이 아니므로 저장할 수 없습니다.");
        }
        if ((before != null && "EVALUATION_CONFIRMED".equals(before.achievementStatus()))
                || guard.countEvaluationConfirmations(owner, year) > 0) {
            conflict("CONFIRMED_DATA_LOCKED", "평가확정 실적은 변경할 수 없습니다.");
        }
        if (before != null && !EDITABLE.contains(before.achievementStatus())) {
            conflict("INVALID_STATE_TRANSITION", "작성중 또는 반려 상태에서만 수정할 수 있습니다.");
        }
        function(user, before == null ? "CREATE" : "UPDATE");
        if (before != null && body.evaluationYear() != null && !year.equals(body.evaluationYear())) {
            invalid("evaluationYear", "수정 시 평가연도를 변경할 수 없습니다.");
        }
        validate(body, year, owner);
        String next = before == null ? "DRAFT" : before.achievementStatus();
        if (body.achievementStatus() != null && !body.achievementStatus().name().equals(next)) {
            if (before == null || !"SUBMITTED".equals(body.achievementStatus().name())
                    || !EDITABLE.contains(next)) {
                conflict("INVALID_STATE_TRANSITION", "일반 저장에서는 제출 또는 재제출만 허용됩니다.");
            }
            next = "SUBMITTED";
        }
        Map<String, Object> values = new HashMap<>();
        values.put("achievementId", id);
        values.put("managementNo", "LI-" + UUID.randomUUID());
        values.put("owner", owner);
        values.put("year", year);
        values.put("code", body.managementItemCode().trim());
        values.put("date", body.achievementDate());
        values.put("attachment", text(body.attachmentRef()));
        values.put("status", next);
        values.put("userId", user.userId());
        if (before == null) {
            String organization = mapper.organization(owner);
            if (organization == null) {
                throw new ForbiddenException();
            }
            values.put("organization", organization);
            mapper.insertHeader(values);
            id = ((Number) values.get("achievementId")).longValue();
            mapper.insertDetail(id, body);
        } else {
            if (mapper.updateHeader(values) != 1) {
                conflict("CONFIRMED_DATA_LOCKED", "실적 상태가 변경되어 수정할 수 없습니다.");
            }
            mapper.updateDetail(id, body);
        }
        LectureImprovementRow after = existing(id, false);
        mapper.history(id, before == null ? null : snapshot(before), snapshot(after),
                before == null ? "CREATE" : "UPDATE", user.userId(), requestId);
        if (before == null || !before.achievementStatus().equals(next)) {
            mapper.statusHistory(id, before == null ? null : before.achievementStatus(), next, user.userId(), requestId);
        }
        boolean warning = guard.countEvaluationDatePeriods(year, owner, body.achievementDate()) == 0;
        return new LectureImprovementSaveResult(after, warning,
                warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 허용됩니다." : null);
    }

    private void validate(LectureImprovementRequest body, String year, Long owner) {
        if (body.managementItemCode() == null || mapper.validManagementItem(year, body.managementItemCode().trim()) == 0) {
            invalid("managementItemCode", "교육영역의 활성 관리항목을 선택하세요.");
        }
        if (body.academicYear() == null || !body.academicYear().matches("[0-9]{4}")) {
            invalid("academicYear", "학년도는 YYYY 형식이어야 합니다.");
        }
        if (body.semester() == null || mapper.validSemester(body.academicYear(), body.semester()) == 0) {
            invalid("semester", "활성 학기 코드를 선택하세요.");
        }
        if (body.performanceContent() == null || body.performanceContent().isBlank()) {
            invalid("performanceContent", "실적내용을 입력하세요.");
        }
        if (text(body.attachmentRef()) != null && mapper.validAttachment(body.attachmentRef().trim(), owner) == 0) {
            invalid("attachmentRef", "본인의 유효한 첨부 참조를 입력하세요.");
        }
    }

    private LectureImprovementRow existing(Long id, boolean lock) {
        LectureImprovementRow row = mapper.find(id, lock);
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    public static void authorize(CurrentUser user, boolean write) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        Set<String> allowed = write ? Set.of("R01", "R09") : Set.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) {
            throw new ForbiddenException();
        }
    }

    private void function(CurrentUser user, String action) {
        // The final request explicitly retains the existing administrator bypass.
        if (user.roles().contains("R09")) {
            return;
        }
        for (String role : user.roles()) {
            if (!("READ".equals(action) ? Set.of("R01", "R02", "R04") : Set.of("R01")).contains(role)) {
                continue;
            }
            try {
                permissions.evaluate(new FunctionPermissionEvaluateRequest(SCREEN_ID, role, action, "DRAFT", null));
                return;
            } catch (ForbiddenException denied) {
                // A second allowed role may provide permission; preserve the role scope union.
            }
        }
        throw new ForbiddenException();
    }

    private String snapshot(LectureImprovementRow row) {
        try {
            return json.writeValueAsString(row);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("실적 이력 직렬화 실패", exception);
        }
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void invalid(String field, String message) {
        throw new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }

    private static void conflict(String code, String message) {
        throw new LectureImprovementConflictException(code, message);
    }
}
