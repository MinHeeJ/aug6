package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.type.TypeReference;
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

/** Owns caller-scoped reads and atomic header/detail/history mutations for employment improvements. */
@Service
public class EmploymentRateImprovementService {
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardMapper guard;
    private final ObjectMapper json;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper, EducationAchievementGuardMapper guard, ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.json = json;
    }

    /** Returns the same scope/filter predicate for rows and total, plus server-owned management metadata. */
    @Transactional(readOnly = true)
    public Map<String, Object> list(EmploymentRateImprovementSearchCriteria criteria, CurrentUser user) {
        requireRole(user, false);
        if (criteria.page() < 0 || !List.of(20, 50, 100).contains(criteria.pageSize())) {
            invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        return Map.of(
                "achievements", mapper.list(criteria, user.userId(), user.roles()).stream().map(this::project).toList(),
                "page", criteria.page(), "pageSize", criteria.pageSize(),
                "totalElements", mapper.count(criteria, user.userId(), user.roles()),
                "managementItems", mapper.managementItems(null));
    }

    /** Detail and list have identical ownership boundaries; an absent resource is distinct from forbidden. */
    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id, CurrentUser user) {
        requireRole(user, false);
        Map<String, Object> row = existing(id, false);
        if (mapper.inScope(id, user.userId(), user.roles()) == 0) {
            throw new ForbiddenException();
        }
        return project(row);
    }

    /** Inserts the generated header key before detail, initial status and field-level audit in one transaction. */
    @Transactional
    public Map<String, Object> create(EmploymentRateImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(body);
        List<String> organizations = mapper.organizations(user.userId());
        if (organizations.size() != 1) {
            invalid("organizationCode", "활성 소속이 없거나 여러 개입니다. 소속 설정을 확인하세요.");
        }
        String organization = organizations.get(0);
        List<String> years = mapper.years(organization);
        if (years.isEmpty()) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        if (years.size() != 1) {
            invalid("evaluationYear", "입력기간 설정이 여러 개입니다. 평가년도 설정을 확인하세요.");
        }
        String year = years.get(0);
        OccurredDateValidation warning = validatePeriod(user.userId(), year, body);
        validateItem(body, year);
        String attachments = attachments(body);
        Map<String, Object> insert = new LinkedHashMap<>();
        insert.put("managementNo", "ERI-" + UUID.randomUUID());
        insert.put("teacherUserId", user.userId());
        insert.put("organizationCode", organization);
        insert.put("evaluationYear", year);
        insert.put("managementItemCode", body.managementItemCode().trim());
        insert.put("achievementDate", body.achievementDate());
        insert.put("attachmentRef", attachments);
        insert.put("actor", user.userId());
        mapper.insertHeader(insert);
        Long id = ((Number) insert.get("achievementId")).longValue();
        mapper.insertDetail(id, body);
        mapper.initialStatus(id, user.userId(), requestId);
        Map<String, Object> saved = existing(id, false);
        audit(id, null, saved, user, requestId);
        return result(saved, warning, requestId);
    }

    /** Locks existing rows and preserves owner, organization and evaluation year even when occurrence date changes. */
    @Transactional
    public Map<String, Object> update(
            Long id, EmploymentRateImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(body);
        Map<String, Object> old = existing(id, true);
        Long owner = ((Number) old.get("teacherUserId")).longValue();
        if (!user.roles().contains("R09") && !owner.equals(user.userId())) {
            throw new ForbiddenException();
        }
        String status = String.valueOf(old.get("certificationStatus"));
        if ("EVALUATION_CONFIRMED".equals(status)) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        if (!List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED").contains(status)) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 현재 상태에서는 수정할 수 없습니다.");
        }
        String year = String.valueOf(old.get("evaluationYear"));
        OccurredDateValidation warning = validatePeriod(owner, year, body);
        validateItem(body, year);
        String attachments = attachments(body);
        mapper.updateHeader(id, body, attachments, user.userId());
        mapper.updateDetail(id, body);
        Map<String, Object> saved = existing(id, false);
        audit(id, old, saved, user, requestId);
        return result(saved, warning, requestId);
    }

    private OccurredDateValidation validatePeriod(Long owner, String year, EmploymentRateImprovementRequest body) {
        if (guard.countEvaluationConfirmations(owner, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 자료는 변경할 수 없습니다.");
        }
        if (guard.countActiveInputPeriods(year, owner) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        return guard.countEvaluationDatePeriods(year, owner, body.achievementDate()) == 0
                ? OccurredDateValidation.outsideEvaluationPeriod() : OccurredDateValidation.accepted();
    }

    private void validateItem(EmploymentRateImprovementRequest body, String year) {
        List<Map<String, Object>> items = mapper.managementItems(year).stream()
                .filter(item -> body.managementItemCode().trim().equals(item.get("managementItemCode"))).toList();
        if (items.size() != 1 || !"Y".equals(items.get(0).get("teacherEditableYn"))) {
            invalid("managementItemCode", "활성 교육영역의 교원 입력가능 관리항목을 하나로 지정해야 합니다.");
        }
    }

    private void validate(EmploymentRateImprovementRequest body) {
        List<ValidationError> fields = new ArrayList<>();
        if (body == null) {
            invalid("body", "실적 정보를 입력하세요.");
        }
        if (body.managementItemCode() == null || body.managementItemCode().isBlank()
                || body.managementItemCode().length() > 50) {
            fields.add(new ValidationError("managementItemCode", "관리항목을 선택하세요."));
        }
        if (body.achievementDate() == null) {
            fields.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
        }
        if (body.specialLectureStartDate() != null && body.specialLectureEndDate() != null
                && body.specialLectureEndDate().isBefore(body.specialLectureStartDate())) {
            fields.add(new ValidationError("specialLectureEndDate", "종료일은 시작일 이후여야 합니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("입력값을 확인하세요.", fields);
        }
    }

    private String attachments(EmploymentRateImprovementRequest body) {
        // No general faculty upload/storage metadata contract is available in the merged foundation.
        // Fail closed rather than persisting unverified opaque references or inventing an upload API.
        if (body.attachmentIds() != null && !body.attachmentIds().isEmpty()) {
            invalid("attachmentIds", "첨부 저장소 참조 검증 계약이 없어 신규 첨부 저장을 지원하지 않습니다.");
        }
        return "[]";
    }

    private Map<String, Object> existing(Long id, boolean lock) {
        Map<String, Object> row = mapper.find(id, lock);
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private Map<String, Object> project(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        try {
            result.put("attachmentIds", json.readValue(
                    String.valueOf(row.getOrDefault("attachmentRef", "[]")), new TypeReference<List<String>>() {}));
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 첨부 참조 형식이 올바르지 않습니다.", exception);
        }
        return result;
    }

    private Map<String, Object> result(Map<String, Object> row, OccurredDateValidation warning, String requestId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("achievementId", row.get("achievementId"));
        result.put("achievement", project(row));
        result.put("occurredDateWarning", warning.warning());
        result.put("warningMessage", warning.message());
        result.put("requestId", requestId);
        return result;
    }

    private void audit(Long id, Map<String, Object> old, Map<String, Object> saved, CurrentUser user, String requestId) {
        for (String field : List.of("managementItemCode", "achievementDate", "specialLectureStartDate",
                "specialLectureEndDate", "mockExamQuestionPeriod", "attachmentRef", "achievementDetail")) {
            Object before = old == null ? null : old.get(field);
            Object after = saved.get(field);
            if (old == null || !Objects.equals(before, after)) {
                mapper.audit(id, old == null ? "CREATE" : "UPDATE", field,
                        before == null ? null : before.toString(), after == null ? null : after.toString(),
                        user.userId(), requestId);
            }
        }
    }

    public static void requireRole(CurrentUser user, boolean write) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        List<String> allowed = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) {
            throw new ForbiddenException();
        }
    }

    private static void invalid(String field, String message) {
        throw new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }
}
