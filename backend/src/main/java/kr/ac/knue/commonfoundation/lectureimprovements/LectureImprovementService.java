package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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

/** Owns FR-031 scoped reads and atomic header/detail, initial status and before/after audit writes. */
@Service
public class LectureImprovementService {
    private final LectureImprovementMapper mapper;
    private final EducationAchievementGuardService guard;
    private final EducationAchievementGuardMapper scope;
    private final ObjectMapper json;

    public LectureImprovementService(
            LectureImprovementMapper mapper,
            EducationAchievementGuardService guard,
            EducationAchievementGuardMapper scope,
            ObjectMapper json) {
        this.mapper = mapper;
        this.guard = guard;
        this.scope = scope;
        this.json = json;
    }

    /** Reads the union of every allowed role's scope, including the matching total. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(LectureImprovementSearchCriteria criteria, CurrentUser user) {
        requireRole(user, false);
        return new LectureImprovementSearchResponse(
                mapper.list(criteria, user.userId(), user.roles()), criteria.page(), criteria.pageSize(),
                mapper.count(criteria, user.userId(), user.roles()));
    }

    /** Enforces the same union scope as the list before returning a type-specific detail. */
    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long id, CurrentUser user) {
        requireRole(user, false);
        LectureImprovementRow row = existing(mapper.find(id));
        boolean allowed = user.roles().contains("R01") && Objects.equals(user.userId(), row.userId());
        allowed |= user.roles().contains("R02") && scope.countSharedActiveOrganization(user.userId(), row.userId()) > 0;
        allowed |= user.roles().contains("R04") && scope.countCertificationScope(user.userId(), row.userId()) > 0;
        if (!allowed) throw new ForbiddenException();
        return row;
    }

    /** Inserts the header with a generated key before inserting its detail. Any failure rolls back all writes. */
    @Transactional
    public LectureImprovementSaveResult create(LectureImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        String year = String.valueOf(body.achievementDate().getYear());
        OccurredDateValidation warning = guard.validateMutation(
                user, new EducationAchievementMutationContext(user.userId(), year, body.achievementDate()));
        validate(body);
        String organization = mapper.organization(user.userId());
        if (organization == null) throw new ForbiddenException();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("managementNo", "LI-" + UUID.randomUUID());
        header.put("userId", user.userId());
        header.put("organization", organization);
        header.put("evaluationYear", year);
        header.put("body", body);
        header.put("attachment", attachment(body));
        header.put("requestId", requestId);
        mapper.insertHeader(header);
        Long id = ((Number) header.get("id")).longValue();
        mapper.insertDetail(id, body, user.userId());
        mapper.statusHistory(id, user.userId(), requestId);
        LectureImprovementRow saved = existing(mapper.find(id));
        mapper.changeHistory(id, "CREATE", null, snapshot(saved), user.userId(), requestId);
        return result(saved, warning);
    }

    /** Retains the original evaluation year, locks the source and refuses non-editable states before mutation. */
    @Transactional
    public LectureImprovementSaveResult update(
            Long id, LectureImprovementRequest body, CurrentUser user, String requestId) {
        requireRole(user, true);
        LectureImprovementRow before = existing(mapper.lock(id));
        if (!Objects.equals(before.userId(), user.userId())) throw new ForbiddenException();
        if (!List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED").contains(before.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 제출 또는 확정 실적은 수정할 수 없습니다.");
        }
        OccurredDateValidation warning = guard.validateMutation(user, new EducationAchievementMutationContext(
                before.userId(), before.evaluationYear(), body.achievementDate()));
        validate(body);
        if (mapper.updateHeader(id, body, attachment(body), user.userId(), requestId) != 1) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 실적 상태가 변경되었습니다.");
        }
        mapper.updateDetail(id, body, user.userId());
        LectureImprovementRow saved = existing(mapper.find(id));
        mapper.changeHistory(id, "UPDATE", snapshot(before), snapshot(saved), user.userId(), requestId);
        return result(saved, warning);
    }

    /** Service-level role enforcement also protects non-HTTP callers; R09 is not a business bypass. */
    public static void requireRole(CurrentUser user, boolean write) {
        if (user == null || user.userId() == null) throw new UnauthenticatedException();
        if (user.roles() == null || (write ? !user.roles().contains("R01")
                : user.roles().stream().noneMatch(List.of("R01", "R02", "R04")::contains))) {
            throw new ForbiddenException();
        }
    }

    private void validate(LectureImprovementRequest body) {
        if (Integer.parseInt(body.academicYear()) < 2000) {
            throw invalid("academicYear", "학년도는 2000년 이후 YYYY 형식으로 입력하세요.");
        }
        if (mapper.activeManagementItems(body.managementItemCode().trim()) != 1) {
            throw invalid("managementItemCode", "사용 가능한 관리항목을 선택하세요. 중복 설정은 사용할 수 없습니다.");
        }
        if (body.attachmentIds() != null && body.attachmentIds().size() > 1) {
            throw invalid("attachmentIds", "현재 첨부 참조 계약은 하나의 파일만 지원합니다.");
        }
        if (body.attachmentRef() != null && body.attachmentIds() != null && !body.attachmentIds().isEmpty()
                && !Objects.equals(body.attachmentRef(), body.attachmentIds().get(0))) {
            throw invalid("attachmentRef", "첨부 참조가 일치하지 않습니다.");
        }
    }

    private String attachment(LectureImprovementRequest body) {
        String ref = body.attachmentRef();
        if (ref == null && body.attachmentIds() != null && !body.attachmentIds().isEmpty()) {
            ref = body.attachmentIds().get(0);
        }
        return ref == null || ref.isBlank() ? null : ref.trim();
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private LectureImprovementRow existing(LectureImprovementRow row) {
        if (row == null) throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        return row;
    }

    private String snapshot(LectureImprovementRow row) {
        try {
            return json.writeValueAsString(row);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("실적 이력 직렬화 실패", exception);
        }
    }

    private LectureImprovementSaveResult result(LectureImprovementRow row, OccurredDateValidation warning) {
        return new LectureImprovementSaveResult(row, warning.warning(), warning.message());
    }
}
