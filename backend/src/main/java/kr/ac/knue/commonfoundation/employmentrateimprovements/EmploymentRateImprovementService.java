package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped FR-029 reads and atomic header/detail writes with immutable before/after audit snapshots. */
@Service
public class EmploymentRateImprovementService {
    private static final Set<String> EDITABLE = Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED");
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardService guard;
    private final ObjectMapper json;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper,
            EducationAchievementGuardService guard,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.json = json;
    }

    /** List and count deliberately receive identical normalized predicates and the complete role set. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            EmploymentRateImprovementCriteria criteria,
            CurrentUser user) {
        requireUser(user, false);
        if (criteria.page() < 0 || !Set.of(20, 50, 100).contains(criteria.pageSize())) {
            throw invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        var normalized = new EmploymentRateImprovementCriteria(
                criteria.page(), criteria.pageSize(), (long) criteria.page() * criteria.pageSize(),
                clean(criteria.managementNo()), clean(criteria.teacherName()),
                clean(criteria.managementItemCode()), clean(criteria.achievementStatus()));
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(normalized, user.userId(), user.roles()).stream().map(this::view).toList(),
                normalized.page(), normalized.pageSize(), mapper.count(normalized, user.userId(), user.roles()));
    }

    /** A known row outside the union of the caller's scopes returns 403, not leaked detail. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow detail(Long id, CurrentUser user) {
        requireUser(user, false);
        var row = existing(id, false);
        if (mapper.countScope(id, user.userId(), user.roles()) == 0) {
            throw new ForbiddenException();
        }
        return view(row);
    }

    /** Always creates a new DRAFT owned by the caller; generated header key precedes dependent detail insertion. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest request,
            CurrentUser user,
            String requestId) {
        return write(null, request, user, requestId);
    }

    /** Locks the typed source row before guards, retaining its original evaluation year across date changes. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(
            Long id,
            EmploymentRateImprovementRequest request,
            CurrentUser user,
            String requestId) {
        return write(id, request, user, requestId);
    }

    private EmploymentRateImprovementSaveResult write(
            Long id,
            EmploymentRateImprovementRequest request,
            CurrentUser user,
            String requestId) {
        requireUser(user, true);
        validate(request);
        var before = id == null ? null : existing(id, true);
        if (before != null && !before.teacherUserId().equals(user.userId())) {
            throw new ForbiddenException();
        }
        if (before != null && !EDITABLE.contains(before.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 제출 또는 확정된 실적은 수정할 수 없습니다.");
        }
        String year = before == null ? Year.from(request.achievementDate()).toString() : before.evaluationYear();
        var warning = guard.validateMutation(
                user, new EducationAchievementMutationContext(user.userId(), year, request.achievementDate()));
        if (mapper.countManagementItem(request.managementItemCode().trim()) != 1) {
            throw invalid("managementItemCode", "활성 관리항목이 없거나 여러 규정에 중복됩니다.");
        }
        String organization = before == null ? mapper.organization(user.userId()) : before.organizationCode();
        if (organization == null) {
            throw new ForbiddenException();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("userId", user.userId());
        values.put("requestId", requestId);
        values.put("managementNo", "ERI-" + UUID.randomUUID());
        values.put("organization", organization);
        values.put("year", year);
        values.put("code", request.managementItemCode().trim());
        values.put("date", request.achievementDate());
        values.put("start", request.specialLectureStartDate());
        values.put("end", request.specialLectureEndDate());
        values.put("period", clean(request.mockExamQuestionPeriod()));
        String attachments = serialize(request.attachmentIds() == null ? List.of() : request.attachmentIds());
        if (attachments.length() > 300) {
            throw invalid("attachmentIds", "첨부 참조의 합계 길이가 허용 범위를 초과합니다.");
        }
        values.put("attachments", attachments);
        if (before == null) {
            mapper.insertHeader(values);
            if (!(values.get("id") instanceof Number)) {
                throw new IllegalStateException("Header generated key missing");
            }
            mapper.insertDetail(values);
            mapper.insertStatusHistory(values);
        } else {
            if (mapper.updateHeader(values) != 1 || mapper.updateDetail(values) != 1) {
                throw new ConflictException("CONFIRMED_DATA_LOCKED: 실적 상태가 변경되었습니다.");
            }
        }
        Long savedId = ((Number) values.get("id")).longValue();
        var after = existing(savedId, false);
        // Whole-row snapshots preserve every changed field, attachment and business date in the same transaction.
        values.put("before", before == null ? null : serialize(view(before)));
        values.put("after", serialize(view(after)));
        values.put("changeType", before == null ? "CREATE" : "UPDATE");
        mapper.insertChangeHistory(values);
        return new EmploymentRateImprovementSaveResult(view(after), warning.warning(), warning.message());
    }

    private EmploymentRateImprovementStoredRow existing(Long id, boolean lock) {
        var row = mapper.find(id, lock);
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireUser(CurrentUser user, boolean write) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || (write ? !user.roles().contains("R01")
                : user.roles().stream().noneMatch(Set.of("R01", "R02", "R04")::contains))) {
            throw new ForbiddenException();
        }
    }

    private void validate(EmploymentRateImprovementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            throw invalid("body", "실적 정보를 입력하세요.");
        }
        if (clean(request.managementItemCode()) == null || request.managementItemCode().length() > 50) {
            errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요 (최대 50자)."));
        }
        if (request.achievementDate() == null) {
            errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
        }
        if (request.specialLectureStartDate() != null && request.specialLectureEndDate() != null
                && request.specialLectureEndDate().isBefore(request.specialLectureStartDate())) {
            errors.add(new ValidationError("specialLectureEndDate", "종료일은 시작일 이전일 수 없습니다."));
        }
        if (request.mockExamQuestionPeriod() != null && request.mockExamQuestionPeriod().length() > 500) {
            errors.add(new ValidationError("mockExamQuestionPeriod", "500자 이내로 입력하세요."));
        }
        if (request.attachmentIds() != null && (request.attachmentIds().size() > 10
                || request.attachmentIds().stream().anyMatch(v -> v == null || v.isBlank() || v.length() > 200))) {
            errors.add(new ValidationError("attachmentIds", "유효한 첨부 참조를 최대 10개 입력하세요."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("취업률 제고 입력값을 확인하세요.", errors);
        }
    }

    private EmploymentRateImprovementRow view(EmploymentRateImprovementStoredRow row) {
        List<String> attachments;
        try {
            String stored = row.attachmentRef();
            attachments = stored == null || stored.isBlank() ? List.of()
                    : stored.startsWith("[") ? json.readValue(stored, new TypeReference<List<String>>() { })
                    : List.of(stored);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid persisted attachment reference", exception);
        }
        return new EmploymentRateImprovementRow(
                row.achievementId(), row.managementNo(), row.teacherUserId(), row.teacherName(),
                row.organizationCode(), row.evaluationYear(), row.managementItemCode(), row.achievementDate(),
                row.achievementStatus(), row.specialLectureStartDate(), row.specialLectureEndDate(),
                row.mockExamQuestionPeriod(), attachments, row.createdAt(), row.updatedAt());
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize achievement snapshot", exception);
        }
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
