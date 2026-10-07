package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.common.education.EducationAchievementValidation;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped reads and atomic header/detail/history writes without changing legacy guard policies. */
@Service
public class LectureImprovementService {
    private static final String SCREEN = "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";
    private final LectureImprovementMapper mapper;
    private final EducationAchievementGuardMapper guard;
    private final FunctionPermissionMapper permissions;

    public LectureImprovementService(LectureImprovementMapper mapper,
            EducationAchievementGuardMapper guard, FunctionPermissionMapper permissions) {
        this.mapper = mapper;
        this.guard = guard;
        this.permissions = permissions;
    }

    /** Union scope is enforced in identical list/count predicates. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(LectureImprovementSearch search, CurrentUser user) {
        authorize(user, "READ");
        return new LectureImprovementSearchResponse(mapper.list(search, user.userId(), user.roles()),
                search.page(), search.pageSize(), mapper.count(search, user.userId(), user.roles()));
    }

    /** Detail access uses exactly the list ownership boundary, including multi-role union. */
    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long id, CurrentUser user) {
        authorize(user, "READ");
        LectureImprovementRow row = existing(id, false);
        if (mapper.visible(id, user.userId(), user.roles()) == 0) throw new ForbiddenException();
        return row;
    }

    /** POST always inserts, obtaining the generated header key before inserting its detail. */
    @Transactional
    public LectureImprovementSaveResult create(LectureImprovementRequest body, CurrentUser user, String requestId) {
        return mutate(null, body, user, requestId);
    }

    /** PUT locks the current row, preserves identity/evaluation year, and rechecks guards before writing. */
    @Transactional
    public LectureImprovementSaveResult update(Long id, LectureImprovementRequest body,
            CurrentUser user, String requestId) {
        return mutate(id, body, user, requestId);
    }

    private LectureImprovementSaveResult mutate(Long id, LectureImprovementRequest body,
            CurrentUser user, String requestId) {
        requireRole(user, id == null ? "CREATE" : "UPDATE");
        validate(body);
        LectureImprovementRow old = id == null ? null : existing(id, true);
        if (old != null && !old.teacherUserId().equals(user.userId()) && !user.roles().contains("R09")) {
            throw new ForbiddenException();
        }
        Long owner = old == null ? user.userId() : old.teacherUserId();
        String year = old == null ? String.valueOf(body.achievementDate().getYear()) : old.evaluationYear();
        if ((old != null && "EVALUATION_CONFIRMED".equals(old.achievementStatus()))
                || guard.countEvaluationConfirmations(owner, year) > 0) {
            throw conflict("CONFIRMED_DATA_LOCKED", "평가확정 실적은 수정할 수 없습니다.", requestId);
        }
        if (old != null && !List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED")
                .contains(old.achievementStatus())) {
            throw new ConflictException("현재 상태에서는 실적을 수정할 수 없습니다.");
        }
        if (guard.countActiveInputPeriods(year, owner) == 0) {
            throw conflict("PERIOD_NOT_ACTIVE", "활성 입력기간이 아닙니다.", requestId);
        }
        authorize(user, id == null ? "CREATE" : "UPDATE");
        if (mapper.managementItems(body.managementItemCode().trim()) != 1) {
            throw invalid("managementItemCode", "활성 관리항목이 없거나 코드가 모호합니다.");
        }
        // Storage is owned by a sibling step. Never accept unverifiable references as real files.
        if (body.attachmentIds() != null && !body.attachmentIds().isEmpty()) {
            throw new NotFoundException("첨부 소유권과 실제 파일을 확인할 수 없습니다.");
        }
        String organization = old == null ? organization(owner, body) : old.organizationCode();
        Map<String, Object> values = new HashMap<>();
        values.put("id", id);
        values.put("managementNo", "LI-" + UUID.randomUUID());
        values.put("owner", owner);
        values.put("organization", organization);
        values.put("year", year);
        values.put("code", body.managementItemCode().trim());
        values.put("date", body.achievementDate());
        values.put("attachments", "[]");
        values.put("userId", user.userId());
        String semester = EducationAchievementValidation.semesterCode(body.semester());
        if (old == null) {
            mapper.insertHeader(values);
            id = ((Number) values.get("id")).longValue();
            mapper.insertDetail(id, body, semester);
            mapper.statusHistory(id, user.userId());
        } else {
            mapper.updateHeader(values);
            mapper.updateDetail(id, body, semester);
        }
        LectureImprovementRow saved = existing(id, false);
        Map<String, String> before = old == null ? Map.of() : fields(old);
        fields(saved).forEach((field, value) -> {
            if (old == null || !Objects.equals(before.get(field), value)) {
                mapper.history(saved.achievementId(), old == null ? "CREATE" : "UPDATE", field,
                        before.get(field), value, user.userId(), requestId);
            }
        });
        boolean warning = guard.countEvaluationDatePeriods(year, owner, body.achievementDate()) == 0;
        return new LectureImprovementSaveResult(saved, warning,
                warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 허용됩니다." : null);
    }

    private Map<String, String> fields(LectureImprovementRow row) {
        return Map.of("management_item_code", row.managementItemCode(), "achievement_date",
                row.achievementDate().toString(), "performance_content", row.achievementContent(),
                "academic_year", row.academicYear().toString(), "semester_code", row.semester().toString(),
                "attachment_ref", row.attachmentRef() == null ? "[]" : row.attachmentRef());
    }

    private String organization(Long owner, LectureImprovementRequest body) {
        List<String> organizations = mapper.organizations(owner, body.achievementDate());
        if (organizations.size() != 1) throw invalid("achievementDate", "발생일의 유효 조직이 없거나 복수입니다.");
        return organizations.get(0);
    }

    private LectureImprovementRow existing(Long id, boolean lock) {
        LectureImprovementRow row = lock ? mapper.lock(id) : mapper.find(id);
        if (row == null) throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        return row;
    }

    /** Role and function permission are separate from the route's menu permission. */
    private void authorize(CurrentUser user, String action) {
        requireRole(user, action);
        if (user.roles().contains("R09")) return;
        boolean allowed = false;
        for (String role : user.roles()) {
            if (!allowedRoles(action).contains(role)) continue;
            var permission = permissions.findByKey(SCREEN, role, action);
            if (permission != null && "DENY".equals(permission.permissionAllowed())) throw new ForbiddenException();
            allowed |= permission != null && "ALLOW".equals(permission.permissionAllowed());
        }
        if (!allowed) throw new ForbiddenException();
    }

    private List<String> allowedRoles(String action) {
        return "READ".equals(action) ? List.of("R01", "R02", "R04", "R09") : List.of("R01", "R09");
    }

    private void requireRole(CurrentUser user, String action) {
        if (user == null) throw new UnauthenticatedException();
        if (user.roles() == null || user.roles().stream().noneMatch(allowedRoles(action)::contains)) {
            throw new ForbiddenException();
        }
    }

    private void validate(LectureImprovementRequest body) {
        if (body == null) throw invalid("body", "입력값이 필요합니다.");
        if (body.managementItemCode() == null || body.managementItemCode().isBlank()
                || body.managementItemCode().length() > 50) throw invalid("managementItemCode", "관리항목을 확인하세요.");
        if (body.achievementDate() == null) throw invalid("achievementDate", "발생일이 필요합니다.");
        if (body.achievementContent() == null || body.achievementContent().isBlank()) {
            throw invalid("achievementContent", "실적내용이 필요합니다.");
        }
        if (body.academicYear() == null || body.academicYear() < 2000) {
            throw invalid("academicYear", "학년도는 2000 이상이어야 합니다.");
        }
        EducationAchievementValidation.semesterCode(body.semester());
        if (body.attachmentIds() != null) {
            if (body.attachmentIds().stream().anyMatch(token -> token == null || token.isBlank())) {
                throw invalid("attachmentIds", "첨부 참조가 비어 있습니다.");
            }
            try {
                String encoded = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(body.attachmentIds());
                if (encoded.length() > 300) throw invalid("attachmentIds", "첨부 참조 길이를 초과했습니다.");
            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
                throw invalid("attachmentIds", "첨부 참조 형식이 올바르지 않습니다.");
            }
        }
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private EducationAchievementConflictException conflict(String code, String message, String requestId) {
        return new EducationAchievementConflictException(code, message, requestId);
    }
}
