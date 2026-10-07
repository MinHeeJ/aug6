package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Course-operation rules and atomic persistence; existing education ledgers remain untouched. */
@Service
public class CourseOperationService {
    private final CourseOperationMapper mapper;
    private final EducationAchievementGuardMapper guard;
    private final ObjectMapper json;

    public CourseOperationService(CourseOperationMapper mapper, EducationAchievementGuardMapper guard,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.json = json;
    }

    /** Applies identical scope/filter predicates to list and total; roles contribute a union of scopes. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(CourseOperationSearchCriteria criteria, CurrentUser user) {
        requireRole(user, false);
        if (criteria.page() < 0 || !List.of(20, 50, 100).contains(criteria.pageSize())
                || (long) criteria.page() * criteria.pageSize() > Integer.MAX_VALUE) {
            throw invalid("pageSize", "페이지 및 표시 건수를 확인하세요. 20/50/100건만 지원합니다.");
        }
        CourseOperationSearchCriteria safe = new CourseOperationSearchCriteria(
                criteria.page(), criteria.pageSize(), criteria.page() * criteria.pageSize(),
                clean(criteria.managementNo()), clean(criteria.teacherName()),
                clean(criteria.managementItemCode()), clean(criteria.achievementStatus()));
        return new CourseOperationSearchResponse(
                mapper.list(safe, user.userId(), user.roles()), safe.page(), safe.pageSize(),
                mapper.count(safe, user.userId(), user.roles()), mapper.managementItems());
    }

    /** Enforces the same scope as list while keeping absent/type-mismatched/deleted rows out. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        CourseOperationRow row = found(mapper.find(id));
        if (mapper.visible(id, user.userId(), user.roles()) == 0) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Header generated key is obtained before detail insertion; histories commit with both rows. */
    @Transactional
    public CourseOperationSaveResult create(CourseOperationRequest request, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(request);
        List<String> organizations = mapper.organizations(user.userId());
        if (organizations.size() != 1) {
            throw invalid("organizationCode", "활성 소속 조직이 없거나 모호합니다.");
        }
        String organization = organizations.get(0);
        List<String> years = mapper.years(organization);
        if (years.isEmpty()) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        if (years.size() != 1) {
            throw invalid("evaluationYear", "활성 교육영역 입력기간이 모호합니다.");
        }
        String year = years.get(0);
        checkItem(request.managementItemCode(), year);
        checkAttachments(request, null);
        OccurredDateValidation warning = guard(year, user.userId(), request);
        Map<String, Object> values = values(request, user.userId());
        values.put("organization", organization);
        values.put("year", year);
        values.put("managementNo", "CO-" + UUID.randomUUID());
        mapper.insertHeader(values);
        Long id = ((Number) values.get("id")).longValue();
        mapper.insertDetail(id, request.performanceDetails());
        mapper.initialStatus(id, user.userId(), requestId);
        audit(id, "CREATE", null, request, user.userId(), requestId);
        return result(found(mapper.find(id)), warning, requestId);
    }

    /** Locks the source row before ownership/state/period checks; its year and owner never change. */
    @Transactional
    public CourseOperationSaveResult update(Long id, CourseOperationRequest request,
            CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(request);
        CourseOperationRow old = found(mapper.lock(id));
        if (!isAdmin(user) && !old.teacherUserId().equals(user.userId())) {
            throw new ForbiddenException();
        }
        if ("EVALUATION_CONFIRMED".equals(old.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        if (!List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED").contains(old.achievementStatus())) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 수정 가능한 상태가 아닙니다.");
        }
        OccurredDateValidation warning = guard(old.evaluationYear(), old.teacherUserId(), request);
        checkItem(request.managementItemCode(), old.evaluationYear());
        checkAttachments(request, old);
        Map<String, Object> values = values(request, user.userId());
        values.put("id", id);
        mapper.updateHeader(values);
        mapper.updateDetail(id, request.performanceDetails());
        audit(id, "UPDATE", old, request, user.userId(), requestId);
        return result(found(mapper.find(id)), warning, requestId);
    }

    private OccurredDateValidation guard(String year, Long owner, CourseOperationRequest request) {
        if (guard.countActiveInputPeriods(year, owner) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        if (guard.countEvaluationConfirmations(owner, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        return guard.countEvaluationDatePeriods(year, owner, request.achievementDate()) == 0
                ? OccurredDateValidation.outsideEvaluationPeriod() : OccurredDateValidation.accepted();
    }

    private void checkItem(String code, String year) {
        List<CourseOperationManagementItem> candidates = mapper.managementItems().stream()
                .filter(item -> item.managementItemCode().equals(code.trim()) && item.evaluationYear().equals(year))
                .toList();
        if (candidates.size() != 1) {
            throw invalid("managementItemCode", "활성 교육 관리항목이 없거나 모호합니다.");
        }
        if (!"Y".equals(candidates.get(0).teacherEditableYn())) {
            throw new ForbiddenException();
        }
    }

    private void checkAttachments(CourseOperationRequest request, CourseOperationRow old) {
        List<String> ids = attachments(request);
        // No approved teacher upload/metadata adapter is available. Never accept invented file tokens.
        // Existing references may only be retained unchanged, not assigned to a different achievement.
        if (!ids.isEmpty() && (old == null || !ids.equals(old.attachmentIds()))) {
            throw invalid("attachmentIds", "검증 가능한 첨부 업로드 계약이 없습니다. 기존 참조만 유지할 수 있습니다.");
        }
    }

    private Map<String, Object> values(CourseOperationRequest request, Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", request.managementItemCode().trim());
        result.put("date", request.achievementDate());
        result.put("attachments", serialize(attachments(request)));
        result.put("userId", userId);
        return result;
    }

    private void audit(Long id, String type, CourseOperationRow old, CourseOperationRequest request,
            Long actor, String requestId) {
        history(id, type, "managementItemCode", old == null ? null : old.managementItemCode(),
                request.managementItemCode().trim(), actor, requestId);
        history(id, type, "achievementDate", old == null ? null : old.achievementDate().toString(),
                request.achievementDate().toString(), actor, requestId);
        history(id, type, "performanceDetails", old == null ? null : old.performanceDetails(),
                request.performanceDetails(), actor, requestId);
        history(id, type, "attachmentIds", old == null ? null : serialize(old.attachmentIds()),
                serialize(attachments(request)), actor, requestId);
    }

    private void history(Long id, String type, String field, String before, String after,
            Long actor, String requestId) {
        if (!Objects.equals(before, after)) {
            mapper.history(id, type, field, before, after, actor, requestId);
        }
    }

    private CourseOperationSaveResult result(CourseOperationRow row, OccurredDateValidation warning, String requestId) {
        return new CourseOperationSaveResult(row.achievementId(), row, warning.warning(), warning.message(), requestId);
    }

    private CourseOperationRow found(CourseOperationRow row) {
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    static void requireRole(CurrentUser user, boolean write) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        List<String> allowed = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) {
            throw new ForbiddenException();
        }
    }

    private boolean isAdmin(CurrentUser user) {
        return user.roles().contains("R09");
    }

    private void validate(CourseOperationRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            throw invalid("body", "실적 정보를 입력하세요.");
        }
        if (clean(request.managementItemCode()) == null) {
            errors.add(new ValidationError("managementItemCode", "관리항목을 선택하세요."));
        }
        if (request.achievementDate() == null) {
            errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
        }
        if (clean(request.performanceDetails()) == null) {
            errors.add(new ValidationError("performanceDetails", "실적내역을 입력하세요."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("입력값을 확인하세요.", errors);
        }
    }

    private List<String> attachments(CourseOperationRequest request) {
        return request.attachmentIds() == null ? List.of() : request.attachmentIds();
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw invalid("attachmentIds", "첨부 참조 형식이 올바르지 않습니다.");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }
}
