package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped teaching-improvement reads and atomic header/detail/full-image history writes. */
@Service
public class LectureImprovementService {
    private final LectureImprovementMapper mapper;
    private final EducationAchievementGuardService guard;
    private final ObjectMapper json;

    public LectureImprovementService(
            LectureImprovementMapper mapper, EducationAchievementGuardService guard, ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.json = json;
    }

    /** The mapper uses one shared union-of-scopes predicate for both page and total. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(LectureImprovementSearchCriteria criteria, CurrentUser user) {
        requireRole(user, false);
        if (criteria.page() < 0 || !List.of(20, 50, 100).contains(criteria.pageSize())) {
            invalid("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.");
        }
        return new LectureImprovementSearchResponse(
                mapper.list(criteria, user.userId(), user.roles()), criteria.page(), criteria.pageSize(),
                mapper.count(criteria, user.userId(), user.roles()),
                mapper.academicYears(), mapper.semesters(), mapper.inputOptions(user.userId()));
    }

    /** Detail admission is identical to the list predicate, including multi-role scope unions. */
    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        LectureImprovementRow row = existing(mapper.find(id));
        if (mapper.canRead(id, user.userId(), user.roles()) == 0) throw new ForbiddenException();
        return row;
    }

    /** POST is create-only; PUT preserves owner, organization, evaluation year and lifecycle state. */
    @Transactional
    public LectureImprovementSaveResult save(
            Long id, LectureImprovementRequest request, CurrentUser user, String requestId) {
        requireRole(user, true);
        LectureImprovementRow before = id == null ? null : existing(mapper.lock(id));
        Long owner = before == null ? user.userId() : before.teacherUserId();
        if (!owner.equals(user.userId()) && !user.roles().contains("R09")) throw new ForbiddenException();
        String year = before == null ? String.valueOf(request.achievementDate().getYear()) : before.evaluationYear();
        // Serialize against existing finalization rows before re-checking the shared DB guards.
        mapper.lockFinalizations(owner, year);
        CurrentUser guardUser = user.roles().contains("R09")
                ? new CurrentUser(owner, user.loginId(), user.employeeNo(), user.name(), List.of("R01"), user.menus())
                : user;
        OccurredDateValidation warning = guard.validateMutation(
                guardUser, new EducationAchievementMutationContext(owner, year, request.achievementDate()));
        if (before != null && "EVALUATION_CONFIRMED".equals(before.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        if (before != null && !List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED")
                .contains(before.achievementStatus())) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 현재 상태에서는 수정할 수 없습니다.");
        }
        if (!mapper.academicYears().contains(String.valueOf(request.academicYear()))) {
            invalid("academicYear", "DB에 설정된 학년도를 선택하세요.");
        }
        if (!mapper.semesters().contains(String.valueOf(request.semester()))) {
            invalid("semester", "학기 코드가 없거나 허용되지 않습니다. 코드 설정을 확인하세요.");
        }
        if (mapper.allowedItem(owner, year, request.managementItemCode()) == 0) {
            invalid("managementItemCode", "해당 평가연도·소속에서 교원 입력 가능한 관리항목을 선택하세요.");
        }
        Map<String, Object> values = new HashMap<>();
        values.put("id", id);
        values.put("owner", owner);
        values.put("organization", before == null ? mapper.organization(owner) : before.organizationCode());
        if (values.get("organization") == null) throw new ForbiddenException();
        values.put("year", year);
        values.put("request", request);
        try {
            values.put("attachments", json.writeValueAsString(Map.of("attachmentIds",
                    request.attachmentIds() == null ? List.of() : request.attachmentIds())));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("첨부참조 형식 오류");
        }
        values.put("actor", user.userId());
        values.put("requestId", requestId);
        if (before == null) {
            // Generated key is assigned before inserting the detail; no joined read is used to obtain it.
            mapper.insertHeader(values);
            mapper.insertDetail(values);
            mapper.statusHistory(values);
        } else {
            if (mapper.updateHeader(values) != 1) throw new ConflictException("INVALID_STATE_TRANSITION");
            mapper.updateDetail(values);
        }
        LectureImprovementRow after = existing(mapper.find(((Number) values.get("id")).longValue()));
        values.put("changeType", before == null ? "CREATE" : "UPDATE");
        values.put("before", before == null ? null : serialize(before));
        values.put("after", serialize(after));
        mapper.history(values);
        return new LectureImprovementSaveResult(after, warning.warning(), warning.message());
    }

    public static void requireRole(CurrentUser user, boolean write) {
        if (user == null) throw new UnauthenticatedException();
        List<String> allowed = write ? List.of("R01", "R09") : List.of("R01", "R02", "R04", "R09");
        if (user.roles() == null || user.roles().stream().noneMatch(allowed::contains)) throw new ForbiddenException();
    }

    private LectureImprovementRow existing(LectureImprovementRow row) {
        if (row == null) throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        return row;
    }

    private void invalid(String field, String message) {
        throw new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private String serialize(LectureImprovementRow row) {
        try {
            return json.writeValueAsString(row);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("강의개선 변경이력 직렬화 실패", exception);
        }
    }
}
