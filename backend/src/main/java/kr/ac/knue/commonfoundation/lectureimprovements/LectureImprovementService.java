package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped FR-031 reads and atomic ledger/detail/full-snapshot writes, not shared workflow transitions. */
@Service
public class LectureImprovementService {
    private static final String SCREEN = "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";
    private static final Set<String> READ_ROLES = Set.of("R01", "R02", "R04");
    private final LectureImprovementMapper mapper;
    private final EducationAchievementGuardService guard;
    private final FunctionPermissionService permissions;
    private final ObjectMapper json;

    public LectureImprovementService(
            LectureImprovementMapper mapper,
            EducationAchievementGuardService guard,
            FunctionPermissionService permissions,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.permissions = permissions;
        this.json = json;
    }

    /** List and total share the same SQL scope and filters, including multi-role union. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(LectureImprovementSearchCriteria criteria, CurrentUser user) {
        requireRead(user);
        requireReadPermission(user);
        if (criteria.page() < 0 || !Set.of(20, 50, 100).contains(criteria.pageSize())) {
            throw invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        List<LectureImprovementOption> items = mapper.managementItems(null).stream()
                .filter(item -> "Y".equals(item.teacherEditableYn()))
                .map(item -> new LectureImprovementOption(item.code(), item.name()))
                .distinct()
                .toList();
        return new LectureImprovementSearchResponse(
                mapper.list(criteria, user),
                criteria.page(),
                criteria.pageSize(),
                mapper.count(criteria, user),
                mapper.codeOptions("ACADEMIC_YEAR"),
                mapper.codeOptions("SEMESTER"),
                items,
                canWrite(user, "CREATE"),
                canWrite(user, "UPDATE"));
    }

    /** Explicit detail read checks the same SQL ownership union as list. */
    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long id, CurrentUser user) {
        requireRead(user);
        requireReadPermission(user);
        LectureImprovementRow row = find(id, false);
        if (mapper.canRead(id, user) == 0) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Create-only: the header-generated key precedes the FK detail and both audit writes. */
    @Transactional
    public LectureImprovementSaveResult create(
            LectureImprovementRequest request, CurrentUser user, String requestId) {
        requireWrite(user, "CREATE");
        validateRequired(request);
        String year = String.valueOf(request.achievementDate().getYear());
        lockContext(user.userId(), year);
        OccurredDateValidation warning = guard.validateMutation(
                user, new EducationAchievementMutationContext(user.userId(), year, request.achievementDate()));
        validateConfiguredFields(request, year);
        List<String> attachments = checkedAttachments(request.attachmentIds(), List.of());
        String organization = mapper.organization(user.userId());
        if (organization == null) {
            throw new ForbiddenException();
        }
        Long id = mapper.insertHeader(
                request,
                user.userId(),
                year,
                organization,
                "LI-" + UUID.randomUUID(),
                detailJson(request),
                serialize(attachments));
        if (id == null) {
            throw new IllegalStateException("실적 식별자 생성 실패");
        }
        mapper.insertDetail(id, request);
        mapper.insertStatusHistory(id, user.userId());
        mapper.insertChangeHistory(id, null, mapper.snapshot(id), user.userId(), requestId);
        return result(find(id, false), warning);
    }

    /** Update-only: identity/year/status stay server-owned; snapshot and mutations commit or roll back together. */
    @Transactional
    public LectureImprovementSaveResult update(
            Long id, LectureImprovementRequest request, CurrentUser user, String requestId) {
        requireWrite(user, "UPDATE");
        validateRequired(request);
        // Owner lock serializes this feature's writes, and FK locks also block new finalization inserts.
        mapper.lockOwner(user.userId());
        LectureImprovementRow before = find(id, true);
        if (!user.userId().equals(before.teacherUserId())) {
            throw new ForbiddenException();
        }
        mapper.lockFinalizations(user.userId(), before.evaluationYear());
        mapper.lockInputPeriods(before.evaluationYear());
        // Preserve BASIC81's original evaluation year even when achievementDate crosses a year boundary.
        OccurredDateValidation warning = guard.validateMutation(
                user,
                new EducationAchievementMutationContext(
                        before.teacherUserId(), before.evaluationYear(), request.achievementDate()));
        if ("EVALUATION_CONFIRMED".equals(before.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 변경할 수 없습니다.");
        }
        if (!"DRAFT".equals(before.achievementStatus())) {
            throw new ConflictException("DATA_NOT_EDITABLE: 작성중 실적만 수정할 수 있습니다.");
        }
        validateConfiguredFields(request, before.evaluationYear());
        List<String> attachments = checkedAttachments(request.attachmentIds(), before.attachmentIds());
        String snapshot = mapper.snapshot(id);
        if (mapper.updateHeader(id, request, user.userId(), detailJson(request), serialize(attachments)) != 1) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 실적 상태가 변경되었습니다.");
        }
        mapper.updateDetail(id, request);
        mapper.insertChangeHistory(id, snapshot, mapper.snapshot(id), user.userId(), requestId);
        return result(find(id, false), warning);
    }

    private void lockContext(Long owner, String year) {
        mapper.lockOwner(owner);
        mapper.lockFinalizations(owner, year);
        mapper.lockInputPeriods(year);
    }

    private LectureImprovementRow find(Long id, boolean lock) {
        LectureImprovementRow row = mapper.find(id, lock);
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireRead(CurrentUser user) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || user.roles().stream().noneMatch(READ_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireReadPermission(CurrentUser user) {
        for (String role : user.roles()) {
            if (READ_ROLES.contains(role) && allowed(role, "READ")) {
                return;
            }
        }
        throw new ForbiddenException();
    }

    private boolean allowed(String role, String function) {
        try {
            return permissions.evaluate(new FunctionPermissionEvaluateRequest(
                    SCREEN, role, function, "DRAFT", null)).allowed();
        } catch (ForbiddenException exception) {
            return false;
        }
    }

    private boolean canWrite(CurrentUser user, String function) {
        return user.roles().contains("R01") && allowed("R01", function);
    }

    private void requireWrite(CurrentUser user, String function) {
        requireRead(user);
        if (!canWrite(user, function)) {
            throw new ForbiddenException();
        }
    }

    private void validateRequired(LectureImprovementRequest request) {
        if (request == null) {
            throw invalid("body", "강의개선 실적 정보를 입력하세요.");
        }
        if (request.managementItemCode() == null || request.managementItemCode().isBlank()
                || request.managementItemCode().length() > 50) {
            throw invalid("managementItemCode", "관리항목을 선택하세요.");
        }
        if (request.achievementDate() == null) {
            throw invalid("achievementDate", "업적발생일을 입력하세요.");
        }
        if (request.achievementContent() == null || request.achievementContent().isBlank()) {
            throw invalid("achievementContent", "실적내용을 입력하세요.");
        }
        if (request.academicYear() == null || request.academicYear() < 2000 || request.academicYear() > 9999) {
            throw invalid("academicYear", "유효한 학년도를 선택하세요.");
        }
        if (request.semester() == null || request.semester() < 1 || request.semester() > 2) {
            throw invalid("semester", "학기를 선택하세요.");
        }
    }

    private void validateConfiguredFields(LectureImprovementRequest request, String year) {
        requireCode("ACADEMIC_YEAR", request.academicYear(), "academicYear");
        requireCode("SEMESTER", request.semester(), "semester");
        List<LectureImprovementManagementItem> matches = mapper.managementItems(year).stream()
                .filter(item -> item.code().equals(request.managementItemCode()))
                .toList();
        if (matches.size() != 1 || !"Y".equals(matches.get(0).teacherEditableYn())) {
            throw invalid("managementItemCode", "활성·확정 관리항목을 선택하세요. 중복 코드나 입력 불가 항목은 저장할 수 없습니다.");
        }
        LectureImprovementManagementItem item = matches.get(0);
        String value = request.achievementContent();
        if ("Y".equals(item.requiredYn()) && value.isBlank()) {
            throw invalid("achievementContent", "필수 실적내용을 입력하세요.");
        }
        try {
            switch (item.dataType()) {
                case "TEXT" -> { /* Representative FR-031 content is textual. */ }
                case "NUMBER" -> new BigDecimal(value);
                case "DATE" -> LocalDate.parse(value);
                case "BOOLEAN" -> {
                    if (!Set.of("true", "false").contains(value)) {
                        throw new IllegalArgumentException();
                    }
                }
                default -> throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException exception) {
            throw invalid("achievementContent", "관리항목 데이터형식과 실적내용이 일치하지 않습니다.");
        }
    }

    private void requireCode(String group, Integer value, String field) {
        if (mapper.codeOptions(group).stream().noneMatch(option -> option.value().equals(String.valueOf(value)))) {
            throw invalid(field, "현재 사용할 수 있는 등록된 코드를 선택하세요.");
        }
    }

    private List<String> checkedAttachments(List<String> requested, List<String> existing) {
        List<String> retained = existing == null ? List.of() : existing;
        if (requested == null) {
            return retained;
        }
        // No verified shared FileStoragePort is merged yet. Never claim arbitrary tokens are stored/owned files.
        if (!requested.equals(retained)) {
            throw invalid("attachmentIds", "첨부 서비스 연동 전에는 첨부 참조를 변경할 수 없습니다.");
        }
        return retained;
    }

    private String detailJson(LectureImprovementRequest request) {
        return serialize(java.util.Map.of(
                "achievementContent", request.achievementContent(),
                "academicYear", request.academicYear(),
                "semester", request.semester()));
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw invalid("body", "실적 입력값을 확인하세요.");
        }
    }

    private LectureImprovementSaveResult result(LectureImprovementRow row, OccurredDateValidation warning) {
        return new LectureImprovementSaveResult(row, warning.warning(), warning.message());
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException("강의개선 입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }
}
