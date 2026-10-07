package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.common.education.EducationAchievementValidation;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionMapper;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns caller-scoped reads and atomic header/detail/history mutations for employment-rate improvements. */
@Service
public class EmploymentRateImprovementService {
    public static final String SCREEN = "SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardMapper guards;
    private final FunctionPermissionMapper permissions;
    private final ObjectMapper json;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper,
            EducationAchievementGuardMapper guards,
            FunctionPermissionMapper permissions,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guards = guards;
        this.permissions = permissions;
        this.json = json;
    }

    /** Returns a union-scoped page and a count using the identical mapper predicate. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            int page, int pageSize, String managementNo, String managementItemCode,
            String certificationStatus, CurrentUser user) {
        authorize(user, false);
        requireFunction(user, "READ");
        if (page < 0 || !List.of(20, 50, 100).contains(pageSize)) {
            throw invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        var criteria = new EmploymentRateImprovementSearchCriteria(
                page, pageSize, (long) page * pageSize, clean(managementNo),
                clean(managementItemCode), clean(certificationStatus));
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(criteria, user.userId(), user.roles()), page, pageSize,
                mapper.count(criteria, user.userId(), user.roles()));
    }

    /** Detail reads use the same role union as the list, with a distinct out-of-scope 403. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long id, CurrentUser user) {
        authorize(user, false);
        requireFunction(user, "READ");
        var row = existing(mapper.find(id));
        if (mapper.visible(id, user.userId(), user.roles()) == 0) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Creates only a new DRAFT; the generated header identity precedes detail insertion. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest body, CurrentUser user, String requestId) {
        authorize(user, true);
        validate(body);
        String year = Year.from(body.achievementDate()).toString();
        mutationGuards(user.userId(), year, null, requestId);
        requireFunction(user, "CREATE");
        List<String> organizations = mapper.organizations(user.userId(), body.achievementDate());
        if (organizations.size() != 1) {
            throw invalid("achievementDate", "발생일에 유효한 소속 조직이 하나여야 합니다.");
        }
        String attachmentRef = attachments(body.attachmentIds());
        Map<String, Object> header = new HashMap<>();
        header.put("managementNo", "ERI-" + UUID.randomUUID());
        header.put("userId", user.userId());
        header.put("organizationCode", organizations.get(0));
        header.put("evaluationYear", year);
        header.put("managementItemCode", body.managementItemCode().trim());
        header.put("achievementDate", body.achievementDate());
        header.put("attachmentRef", attachmentRef);
        mapper.insertHeader(header);
        Long id = ((Number) header.get("achievementId")).longValue();
        mapper.insertDetail(id, normalized(body));
        mapper.statusHistory(id, user.userId());
        var saved = existing(mapper.find(id));
        audit(null, saved, "CREATE", user, requestId);
        return result(saved);
    }

    /** Locks first; owner, year and organization remain immutable even when occurrence dates change. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(
            Long id, EmploymentRateImprovementRequest body, CurrentUser user, String requestId) {
        authorize(user, true);
        var before = existing(mapper.lock(id));
        if (!user.roles().contains("R09") && !Objects.equals(before.teacherUserId(), user.userId())) {
            throw new ForbiddenException();
        }
        mutationGuards(before.teacherUserId(), before.evaluationYear(), before.certificationStatus(), requestId);
        requireFunction(user, "UPDATE");
        validate(body);
        String attachmentRef = attachments(body.attachmentIds());
        var command = normalized(body);
        if (mapper.updateHeader(id, command, attachmentRef, user.userId()) != 1
                || mapper.updateDetail(id, command) != 1) {
            throw new ConflictException("실적이 변경되었습니다. 다시 조회하세요.");
        }
        var saved = existing(mapper.find(id));
        audit(before, saved, "UPDATE", user, requestId);
        return result(saved);
    }

    private void mutationGuards(Long owner, String year, String status, String requestId) {
        // Finalization precedes function evaluation so the precise lock code survives generic permission failures.
        if ("EVALUATION_CONFIRMED".equals(status) || guards.countEvaluationConfirmations(owner, year) > 0) {
            throw new EducationAchievementConflictException(
                    "CONFIRMED_DATA_LOCKED", "평가확정 실적은 변경할 수 없습니다.", requestId);
        }
        if (status != null && !List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED").contains(status)) {
            throw new ConflictException("작성중 또는 반려된 실적만 수정할 수 있습니다.");
        }
        if (guards.countActiveInputPeriods(year, owner) == 0) {
            throw new EducationAchievementConflictException(
                    "PERIOD_NOT_ACTIVE", "활성 입력기간이 아니므로 저장할 수 없습니다.", requestId);
        }
    }

    /** R09 is the caller-mandated administrator override; business roles still require explicit ALLOW. */
    private void requireFunction(CurrentUser user, String function) {
        if (user.roles().contains("R09")) {
            return;
        }
        boolean allowed = false;
        for (String role : user.roles()) {
            if (!(function.equals("READ") ? List.of("R01", "R02", "R04") : List.of("R01")).contains(role)) {
                continue;
            }
            FunctionPermissionRow permission = permissions.findByKey(SCREEN, role, function);
            if (permission != null && "DENY".equals(permission.permissionAllowed())) {
                throw new ForbiddenException();
            }
            allowed |= permission != null && "ALLOW".equals(permission.permissionAllowed());
        }
        if (!allowed) {
            throw new ForbiddenException();
        }
    }

    private void authorize(CurrentUser user, boolean write) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        List<String> allowed = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) {
            throw new ForbiddenException();
        }
    }

    private void validate(EmploymentRateImprovementRequest body) {
        if (body == null || clean(body.managementItemCode()) == null) {
            throw invalid("managementItemCode", "관리항목을 입력하세요.");
        }
        if (body.achievementDate() == null) {
            throw invalid("achievementDate", "업적발생일을 입력하세요.");
        }
        if (body.managementItemCode().trim().length() > 50
                || mapper.managementItems(body.managementItemCode().trim()) != 1) {
            throw invalid("managementItemCode", "활성 교육 관리항목 코드가 없거나 모호합니다.");
        }
        EducationAchievementValidation.validateSpecialLectureDates(
                body.specialLectureStartDate(), body.specialLectureEndDate());
    }

    private EmploymentRateImprovementRequest normalized(EmploymentRateImprovementRequest body) {
        return new EmploymentRateImprovementRequest(
                body.managementItemCode().trim(), body.achievementDate(), body.specialLectureStartDate(),
                body.specialLectureEndDate(), body.mockExamQuestionPeriod(), body.attachmentIds());
    }

    private String attachments(List<String> tokens) {
        // No verified storage adapter exists in the merged foundation. Do not accept invented opaque references.
        if (tokens != null && !tokens.isEmpty()) {
            throw new NotFoundException("첨부파일 소유권과 파일 존재를 확인할 수 없습니다.");
        }
        return "[]";
    }

    private EmploymentRateImprovementSaveResult result(EmploymentRateImprovementRow saved) {
        boolean warning = guards.countEvaluationDatePeriods(
                saved.evaluationYear(), saved.teacherUserId(), saved.achievementDate()) == 0;
        return new EmploymentRateImprovementSaveResult(
                saved, warning, warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 완료되었습니다." : null);
    }

    private void audit(EmploymentRateImprovementRow before, EmploymentRateImprovementRow after,
            String type, CurrentUser user, String requestId) {
        // Complete snapshots preserve every changed command field, plus immutable identity and lifecycle metadata.
        mapper.history(after.achievementId(), type, "achievement", snapshot(before), snapshot(after),
                user.userId(), requestId);
    }

    private String snapshot(EmploymentRateImprovementRow value) {
        if (value == null) {
            return null;
        }
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("실적 변경이력 직렬화 실패", exception);
        }
    }

    private EmploymentRateImprovementRow existing(EmploymentRateImprovementRow row) {
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }
}
