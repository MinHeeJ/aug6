package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.common.storage.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped reads and atomic course header/detail/snapshot writes without changing shared education behavior. */
@Service
public class CourseOperationService {
    private static final String SCREEN = "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT";
    private static final List<String> EDITABLE = List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED");
    private final CourseOperationMapper mapper;
    private final EducationAchievementGuardMapper guards;
    private final FunctionPermissionService permissions;
    private final FileStoragePort storage;
    private final ObjectMapper json;

    public CourseOperationService(CourseOperationMapper mapper, EducationAchievementGuardMapper guards,
            FunctionPermissionService permissions, FileStoragePort storage, ObjectMapper json) {
        this.mapper = mapper;
        this.guards = guards;
        this.permissions = permissions;
        this.storage = storage;
        this.json = json;
    }

    /** List and count share the exact same union-scope and normalized filter criteria. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(CourseOperationSearchCriteria input, CurrentUser user, String year) {
        requireRole(user, false);
        if (input.page() < 0 || !List.of(20, 50, 100).contains(input.pageSize())) {
            throw invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        function(user, "READ", "DRAFT");
        CourseOperationSearchCriteria criteria = new CourseOperationSearchCriteria(
                input.page(), input.pageSize(), text(input.managementNo()),
                text(input.managementItemCode()), text(input.achievementStatus()));
        String evaluationYear = year == null ? String.valueOf(LocalDate.now().getYear()) : year;
        if (!evaluationYear.matches("[0-9]{4}")) throw invalid("evaluationYear", "평가연도를 확인하세요.");
        return new CourseOperationSearchResponse(
                mapper.list(criteria, user.userId(), user.roles(), (long) criteria.page() * criteria.pageSize()),
                criteria.page(), criteria.pageSize(), mapper.count(criteria, user.userId(), user.roles()),
                mapper.managementItems(evaluationYear, user.userId()));
    }

    /** Detail applies the same scope union as the list; no ID-only ownership bypass. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        CourseOperationRow row = existing(id, false);
        if (mapper.inScope(id, user.userId(), user.roles()) == 0) throw new ForbiddenException();
        function(user, "READ", row.achievementStatus());
        return row;
    }

    /** Creates only; the generated header key is used before any joined detail read. */
    @Transactional
    public CourseOperationSaveResult create(CourseOperationRequest request, CurrentUser user, String requestId) {
        requireRole(user, true);
        validate(request);
        String year = String.valueOf(request.achievementDate().getYear());
        OccurredDateValidation warning = mutationGuard(user.userId(), year, request.achievementDate(), null);
        function(user, "CREATE", "DRAFT");
        validateReferences(request, year, user.userId());
        String organization = mapper.organization(user.userId());
        if (organization == null) throw invalid("organizationCode", "활성 소속이 필요합니다.");
        Map<String, Object> command = command(request, user, requestId);
        command.put("managementNo", "CO-" + UUID.randomUUID());
        command.put("ownerId", user.userId());
        command.put("organization", organization);
        command.put("year", year);
        mapper.insertHeader(command);
        mapper.insertDetail(command);
        mapper.insertStatus(command);
        CourseOperationRow saved = existing(((Number) command.get("id")).longValue(), false);
        history(command, null, saved, "CREATE");
        return new CourseOperationSaveResult(saved, warning.warning(), warning.message());
    }

    /** Locks the stored owner/year/status and audits complete old and new snapshots in this transaction. */
    @Transactional
    public CourseOperationSaveResult update(Long id, CourseOperationRequest request,
            CurrentUser user, String requestId) {
        requireRole(user, true);
        CourseOperationRow before = existing(id, true);
        if (!user.roles().contains("R09") && !before.teacherUserId().equals(user.userId())) {
            throw new ForbiddenException();
        }
        validate(request);
        OccurredDateValidation warning = mutationGuard(before.teacherUserId(), before.evaluationYear(),
                request.achievementDate(), before.achievementStatus());
        function(user, "UPDATE", before.achievementStatus());
        validateReferences(request, before.evaluationYear(), before.teacherUserId());
        Map<String, Object> command = command(request, user, requestId);
        command.put("id", id);
        if (mapper.updateHeader(command) != 1) throw new ConflictException("실적 상태가 변경되었습니다.");
        mapper.updateDetail(command);
        CourseOperationRow after = existing(id, false);
        history(command, before, after, "UPDATE");
        return new CourseOperationSaveResult(after, warning.warning(), warning.message());
    }

    private OccurredDateValidation mutationGuard(Long owner, String year, LocalDate date, String status) {
        if (guards.countActiveInputPeriods(year, owner) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        if ("EVALUATION_CONFIRMED".equals(status) || guards.countEvaluationConfirmations(owner, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        if (status != null && !EDITABLE.contains(status)) {
            throw new ConflictException("실적이 수정 가능한 상태가 아닙니다.");
        }
        return guards.countEvaluationDatePeriods(year, owner, date) == 0
                ? OccurredDateValidation.outsideEvaluationPeriod() : OccurredDateValidation.accepted();
    }

    private void validateReferences(CourseOperationRequest request, String year, Long owner) {
        if (mapper.validManagementItem(request.managementItemCode().trim(), year, owner) != 1) {
            throw invalid("managementItemCode", "활성 교육영역 관리항목이 없거나 여러 평가요소로 모호합니다.");
        }
        for (String id : request.attachmentIds() == null ? List.<String>of() : request.attachmentIds()) {
            try {
                if (id == null || id.isBlank() || storage.find(id, owner) == null) {
                    throw invalid("attachmentIds", "실제 본인 소유 첨부파일만 참조할 수 있습니다.");
                }
                storage.read(id, owner);
            } catch (IOException | IllegalArgumentException exception) {
                throw invalid("attachmentIds", "첨부파일을 확인할 수 없습니다.");
            }
        }
    }

    private void function(CurrentUser user, String action, String status) {
        for (String role : user.roles()) {
            if (!List.of("R01", "R02", "R04", "R09").contains(role)) continue;
            if (!"READ".equals(action) && !List.of("R01", "R09").contains(role)) continue;
            try {
                if (permissions.evaluate(new FunctionPermissionEvaluateRequest(SCREEN, role, action, status, null))
                        .allowed()) return;
            } catch (ForbiddenException exception) {
                // Multi-role admission is a union, not the first role's verdict.
            }
        }
        throw new ForbiddenException();
    }

    static void requireRole(CurrentUser user, boolean write) {
        if (user == null) throw new UnauthenticatedException();
        List<String> allowed = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) throw new ForbiddenException();
    }

    private CourseOperationRow existing(Long id, boolean lock) {
        CourseOperationRow row = mapper.find(id, lock);
        if (row == null) throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        return row;
    }

    private void validate(CourseOperationRequest request) {
        if (request == null) throw invalid("body", "실적 정보를 입력하세요.");
        if (text(request.managementItemCode()) == null) throw invalid("managementItemCode", "관리항목을 선택하세요.");
        if (request.achievementDate() == null) throw invalid("achievementDate", "업적발생일을 입력하세요.");
        if (text(request.performanceDetails()) == null) throw invalid("performanceDetails", "실적내역을 입력하세요.");
    }

    private Map<String, Object> command(CourseOperationRequest request, CurrentUser user, String requestId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", request.managementItemCode().trim());
        result.put("date", request.achievementDate());
        result.put("detail", request.performanceDetails());
        result.put("attachments", serialize(request.attachmentIds() == null ? List.of() : request.attachmentIds()));
        result.put("actorId", user.userId());
        result.put("requestId", requestId);
        return result;
    }

    private void history(Map<String, Object> command, CourseOperationRow before, CourseOperationRow after, String type) {
        command.put("before", before == null ? null : serialize(before));
        command.put("after", serialize(after));
        command.put("changeType", type);
        mapper.insertHistory(command);
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw invalid("body", "실적 데이터를 직렬화할 수 없습니다.");
        }
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }
}
