package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns course CRUD, role-union reads and atomic header/detail/full-snapshot audit writes. */
@Service
public class CourseOperationService {
    private final CourseOperationMapper mapper;
    private final EducationAchievementGuardService guard;
    private final EducationAchievementGuardMapper scopes;
    private final ObjectMapper json;

    public CourseOperationService(CourseOperationMapper mapper, EducationAchievementGuardService guard,
            EducationAchievementGuardMapper scopes, ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.scopes = scopes;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(CourseOperationSearchCriteria criteria, CurrentUser user) {
        requireRole(user, false);
        if (criteria.page() < 0 || !List.of(20, 50, 100).contains(criteria.pageSize())) {
            throw invalid("pageSize", "표시 건수는 20/50/100, 페이지는 0 이상이어야 합니다.");
        }
        CourseOperationSearchCriteria normalized = new CourseOperationSearchCriteria(
                criteria.page(), criteria.pageSize(), (long) criteria.page() * criteria.pageSize(),
                trim(criteria.managementNo()), trim(criteria.teacherName()),
                trim(criteria.managementItemCode()), trim(criteria.achievementStatus()));
        return new CourseOperationSearchResponse(
                mapper.list(normalized, user.userId(), user.roles()), normalized.page(), normalized.pageSize(),
                mapper.count(normalized, user.userId(), user.roles()), mapper.items(user.userId()));
    }

    @Transactional(readOnly = true)
    public CourseOperationRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        CourseOperationRow row = find(id, false);
        requireScope(user, row);
        return row;
    }

    /** Locks existing rows and finalizations, then writes every business field and audit together. */
    @Transactional
    public CourseOperationSaveResult save(Long id, CourseOperationRequest request, CurrentUser user, String requestId) {
        requireRole(user, true);
        if (request == null || trim(request.managementItemCode()) == null) {
            throw invalid("managementItemCode", "관리항목을 입력하세요.");
        }
        if (request.achievementDate() == null) throw invalid("achievementDate", "업적발생일을 입력하세요.");
        if (trim(request.performanceDetails()) == null) throw invalid("performanceDetails", "실적내역을 입력하세요.");
        CourseOperationRow before = id == null ? null : find(id, true);
        Long owner = before == null ? user.userId() : before.teacherUserId();
        if (!user.roles().contains("R09") && !owner.equals(user.userId())) throw new ForbiddenException();
        String year = before == null ? String.valueOf(request.achievementDate().getYear()) : before.evaluationYear();
        // R09 is explicitly admitted by this request, without changing the legacy shared guard.
        CurrentUser guardActor = user.roles().contains("R09")
                ? new CurrentUser(owner, user.loginId(), user.employeeNo(), user.name(), List.of("R01"), user.menus())
                : user;
        if (before != null && "EVALUATION_CONFIRMED".equals(before.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 변경할 수 없습니다.");
        }
        if (before != null && !List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED")
                .contains(before.achievementStatus())) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 현재 상태에서는 수정할 수 없습니다.");
        }
        if (!mapper.lockFinalizations(owner, year).isEmpty()) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 변경할 수 없습니다.");
        }
        OccurredDateValidation warning = guard.validateMutation(guardActor,
                new EducationAchievementMutationContext(owner, year, request.achievementDate()));
        if (mapper.allowedItem(owner, year, request.managementItemCode().trim()) == 0) {
            throw invalid("managementItemCode", "해당 평가연도·소속의 교원 입력 가능한 관리항목을 선택하세요.");
        }
        String organization = before == null ? mapper.organization(owner) : before.organizationCode();
        if (organization == null) throw new ForbiddenException();
        List<String> attachments = request.attachmentIds() == null ? List.of() : request.attachmentIds();
        if (attachments.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw invalid("attachmentIds", "첨부 식별자가 올바르지 않습니다.");
        }
        attachments = List.copyOf(attachments);
        String encoded = serialize(attachments);
        if (encoded.length() > 300) throw invalid("attachmentIds", "첨부 참조값의 저장 길이를 초과했습니다.");
        Map<String, Object> values = new HashMap<>();
        values.put("id", id);
        values.put("owner", owner);
        values.put("organization", organization);
        values.put("year", year);
        values.put("code", request.managementItemCode().trim());
        values.put("date", request.achievementDate());
        values.put("performance", request.performanceDetails());
        values.put("attachments", encoded);
        values.put("actor", user.userId());
        values.put("requestId", requestId);
        if (before == null) {
            mapper.insertHeader(values);
            id = ((Number) Objects.requireNonNull(values.get("id"), "Generated achievement key required")).longValue();
            mapper.insertDetail(values);
            mapper.insertStatus(values);
        } else {
            if (mapper.updateHeader(values) != 1) throw new ConflictException("INVALID_STATE_TRANSITION");
            mapper.updateDetail(values);
        }
        CourseOperationRow after = find(id, false);
        mapper.insertHistory(id, before == null ? "CREATE" : "UPDATE",
                before == null ? null : serialize(before), serialize(after), user.userId(), requestId);
        return new CourseOperationSaveResult(after, warning.warning(), warning.message());
    }

    private CourseOperationRow find(Long id, boolean lock) {
        CourseOperationRow row = mapper.find(id, lock);
        if (row == null) throw new NotFoundException("강좌 운영 실적을 찾을 수 없습니다.");
        return row;
    }

    private void requireScope(CurrentUser user, CourseOperationRow row) {
        if (user.roles().contains("R09")) return;
        if (user.roles().contains("R01") && user.userId().equals(row.teacherUserId())) return;
        if (user.roles().contains("R02")
                && scopes.countSharedActiveOrganization(user.userId(), row.teacherUserId()) > 0) return;
        if (user.roles().contains("R04")
                && scopes.countCertificationScope(user.userId(), row.teacherUserId()) > 0) return;
        throw new ForbiddenException();
    }

    static void requireRole(CurrentUser user, boolean write) {
        if (user == null || user.userId() == null) throw new UnauthenticatedException();
        List<String> allowed = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) throw new ForbiddenException();
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw invalid("body", "실적 입력값을 직렬화할 수 없습니다.");
        }
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
