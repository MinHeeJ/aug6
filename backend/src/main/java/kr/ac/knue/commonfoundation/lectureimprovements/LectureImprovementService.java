package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped reads and atomic header/detail writes; reuses existing scope, period and finalization queries. */
@Service
public class LectureImprovementService {
    private final LectureImprovementMapper mapper;
    private final EducationAchievementGuardMapper guards;
    private final ObjectMapper json;

    public LectureImprovementService(
            LectureImprovementMapper mapper, EducationAchievementGuardMapper guards, ObjectMapper json) {
        this.mapper = mapper;
        this.guards = guards;
        this.json = json;
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

    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(LectureImprovementSearchCriteria criteria, CurrentUser user) {
        requireRole(user, false);
        if (criteria.page() < 0 || !List.of(20, 50, 100).contains(criteria.pageSize())) {
            invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        return new LectureImprovementSearchResponse(
                mapper.list(criteria, user.userId(), user.roles()), criteria.page(), criteria.pageSize(),
                mapper.count(criteria, user.userId(), user.roles()), mapper.managementItems());
    }

    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        LectureImprovementRow row = existing(id, false);
        requireReadScope(user, row);
        return row;
    }

    /** Uses generated header identity before inserting detail; history failure rolls back both. */
    @Transactional
    public LectureImprovementSaveResult create(LectureImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(body);
        List<String> organizations = mapper.organizations(user.userId());
        if (organizations.size() != 1) {
            invalid("organizationCode", "활성 소속 조직이 없거나 여러 개입니다.");
        }
        String organization = organizations.get(0);
        List<String> years = mapper.evaluationYears(organization);
        if (years.isEmpty()) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 없습니다.");
        }
        if (years.size() != 1) {
            invalid("evaluationYear", "평가년도 입력기간 설정이 여러 개입니다.");
        }
        String year = years.get(0);
        validateManagementItem(body.managementItemCode(), year);
        OccurredDateValidation warning = validatePeriod(user.userId(), year, body);
        String attachments = serialize(body.attachmentIds() == null ? List.of() : body.attachmentIds());
        Map<String, Object> header = new HashMap<>();
        header.put("managementNo", "LI-" + UUID.randomUUID());
        header.put("userId", user.userId());
        header.put("organizationCode", organization);
        header.put("evaluationYear", year);
        header.put("body", body);
        header.put("attachments", attachments);
        mapper.insertHeader(header);
        Long id = ((Number) header.get("achievementId")).longValue();
        mapper.insertDetail(id, body);
        mapper.statusHistory(id, user.userId(), requestId);
        audit(id, null, body, attachments, user, requestId);
        return result(id, warning, requestId);
    }

    /** Locks the source before checking owner/state; never derives a replacement evaluation year from input. */
    @Transactional
    public LectureImprovementSaveResult update(
            Long id, LectureImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(body);
        LectureImprovementRow old = existing(id, true);
        if (!user.roles().contains("R09") && !old.teacherUserId().equals(user.userId())) {
            throw new ForbiddenException();
        }
        if ("EVALUATION_CONFIRMED".equals(old.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 변경할 수 없습니다.");
        }
        if (!List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED").contains(old.achievementStatus())) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 현재 상태에서는 수정할 수 없습니다.");
        }
        validateManagementItem(body.managementItemCode(), old.evaluationYear());
        OccurredDateValidation warning = validatePeriod(old.teacherUserId(), old.evaluationYear(), body);
        String attachments = serialize(body.attachmentIds() == null ? List.of() : body.attachmentIds());
        mapper.updateHeader(id, body, attachments, user.userId());
        mapper.updateDetail(id, body);
        audit(id, old, body, attachments, user, requestId);
        return result(id, warning, requestId);
    }

    private LectureImprovementSaveResult result(Long id, OccurredDateValidation warning, String requestId) {
        return new LectureImprovementSaveResult(id, existing(id, false), warning.warning(), warning.message(), requestId);
    }

    private LectureImprovementRow existing(Long id, boolean lock) {
        LectureImprovementRow row = mapper.find(id, lock);
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireReadScope(CurrentUser user, LectureImprovementRow row) {
        if (user.roles().contains("R09")
                || (user.roles().contains("R01") && row.teacherUserId().equals(user.userId()))
                || (user.roles().contains("R02")
                    && guards.countSharedActiveOrganization(user.userId(), row.teacherUserId()) > 0)
                || (user.roles().contains("R04")
                    && guards.countCertificationScope(user.userId(), row.teacherUserId()) > 0)) {
            return;
        }
        throw new ForbiddenException();
    }

    private OccurredDateValidation validatePeriod(Long owner, String year, LectureImprovementRequest body) {
        if (guards.countEvaluationConfirmations(owner, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 변경할 수 없습니다.");
        }
        if (guards.countActiveInputPeriods(year, owner) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        return guards.countEvaluationDatePeriods(year, owner, body.achievementDate()) == 0
                ? OccurredDateValidation.outsideEvaluationPeriod() : OccurredDateValidation.accepted();
    }

    private void validateManagementItem(String code, String year) {
        List<LectureImprovementManagementItem> candidates = mapper.managementItems().stream()
                .filter(item -> code.trim().equals(item.managementItemCode()) && year.equals(item.evaluationYear()))
                .toList();
        if (candidates.size() != 1 || !"Y".equals(candidates.get(0).teacherEditableYn())) {
            invalid("managementItemCode", "입력 가능한 교육영역 관리항목이 없거나 모호합니다.");
        }
    }

    private void validate(LectureImprovementRequest body) {
        List<ValidationError> fields = new ArrayList<>();
        if (body == null) {
            invalid("body", "입력 정보가 필요합니다.");
        }
        if (body.managementItemCode() == null || body.managementItemCode().isBlank()) {
            fields.add(new ValidationError("managementItemCode", "관리항목을 선택하세요."));
        }
        if (body.achievementDate() == null) {
            fields.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
        }
        if (body.achievementContent() == null || body.achievementContent().isBlank()) {
            fields.add(new ValidationError("achievementContent", "실적내용을 입력하세요."));
        }
        if (body.academicYear() == null || body.academicYear() < 2000) {
            fields.add(new ValidationError("academicYear", "학년도는 2000 이상이어야 합니다."));
        }
        if (body.semester() == null || !List.of(1, 2).contains(body.semester())) {
            fields.add(new ValidationError("semester", "학기는 1 또는 2여야 합니다."));
        }
        // No approved teacher-upload adapter exists in this baseline. Never persist unverified tokens.
        if (body.attachmentIds() != null && !body.attachmentIds().isEmpty()) {
            fields.add(new ValidationError("attachmentIds", "검증 가능한 파일 저장 계약이 없어 첨부 등록을 차단합니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("입력값을 확인하세요.", fields);
        }
    }

    private void audit(Long id, LectureImprovementRow old, LectureImprovementRequest body,
            String attachments, CurrentUser user, String requestId) {
        String[] fields = {"managementItemCode", "achievementDate", "achievementContent",
                "academicYear", "semester", "attachmentIds"};
        Object[] before = old == null ? new Object[6] : new Object[]{old.managementItemCode(), old.achievementDate(),
                old.achievementContent(), old.academicYear(), old.semester(), old.attachmentRef()};
        Object[] after = {body.managementItemCode().trim(), body.achievementDate(), body.achievementContent(),
                body.academicYear(), body.semester(), attachments};
        for (int i = 0; i < fields.length; i++) {
            if (old == null || !Objects.equals(before[i], after[i])) {
                mapper.changeHistory(id, old == null ? "CREATE" : "UPDATE", fields[i],
                        before[i] == null ? null : before[i].toString(), after[i].toString(), user.userId(), requestId);
            }
        }
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize validated achievement", exception);
        }
    }

    private static void invalid(String field, String message) {
        throw new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }
}
