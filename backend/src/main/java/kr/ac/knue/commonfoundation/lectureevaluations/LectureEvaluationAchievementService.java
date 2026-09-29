package kr.ac.knue.commonfoundation.lectureevaluations;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies the lecture-evaluation persistence invariant: allowed roles can save non-final rows,
 * and each save leaves both source and change-history evidence in the same transaction.
 */
@Service
public class LectureEvaluationAchievementService {
    private static final Set<String> ALLOWED_ROLES = Set.of("R01", "R02", "R04");
    private static final Set<Integer> PAGE_SIZES = Set.of(20, 50, 100);
    private final LectureEvaluationAchievementMapper mapper;

    public LectureEvaluationAchievementService(LectureEvaluationAchievementMapper mapper) { this.mapper = mapper; }

    /** Returns data-backed rows; filters are emitted only when their value is supplied by MyBatis. */
    @Transactional(readOnly = true)
    public LectureEvaluationAchievementSearchResponse list(LectureEvaluationAchievementSearchCriteria criteria, int page, int pageSize, CurrentUser user) {
        requireRole(user);
        int safePage = Math.max(0, page);
        int safeSize = PAGE_SIZES.contains(pageSize) ? pageSize : 20;
        LectureEvaluationAchievementSearchCriteria normalized = new LectureEvaluationAchievementSearchCriteria(
                text(criteria == null ? null : criteria.managementNo()), text(criteria == null ? null : criteria.teacherName()),
                text(criteria == null ? null : criteria.managementItemCode()), criteria == null ? null : criteria.occurredDateFrom(),
                criteria == null ? null : criteria.occurredDateTo(), text(criteria == null ? null : criteria.certificationStatus()));
        return new LectureEvaluationAchievementSearchResponse(mapper.list(normalized, safeSize, safePage * safeSize), safePage, safeSize, mapper.count(normalized));
    }

    /** Saves a draft/update and records status transitions and before/after values atomically. */
    @Transactional
    public LectureEvaluationAchievementRow save(LectureEvaluationAchievementSaveRequest request, CurrentUser user) {
        requireRole(user);
        validate(request);
        LectureEvaluationAchievementRow before = request.getAchievementId() == null ? null : mapper.find(request.getAchievementId());
        if (request.getAchievementId() != null && before == null) throw new NotFoundException("강의평가 실적을 찾을 수 없습니다.");
        if (before != null && "EVALUATION_CONFIRMED".equals(before.certificationStatus())) {
            throw new ConflictException("평가확정 실적은 수정하거나 삭제할 수 없습니다.");
        }
        String requestedStatus = text(request.getCertificationStatus());
        String nextStatus = requestedStatus == null ? (before == null ? "DRAFTING" : before.certificationStatus()) : requestedStatus;
        String previousStatus = before == null ? null : before.certificationStatus();
        if (previousStatus != null && !previousStatus.equals(nextStatus)) validateTransition(previousStatus, nextStatus, text(request.getChangeReason()), user);
        request.setCertificationStatus(nextStatus);
        if (before == null) mapper.insert(request, String.valueOf(request.getOccurredDate().getYear()), user.userId());
        else mapper.update(request, user.userId());
        LectureEvaluationAchievementRow saved = mapper.find(request.getAchievementId());
        if (saved == null) throw new IllegalStateException("저장한 강의평가 실적을 다시 조회할 수 없습니다.");
        if (previousStatus != null && !previousStatus.equals(nextStatus)) {
            mapper.insertStatusHistory(saved.achievementId(), previousStatus, nextStatus, actionFor(nextStatus), text(request.getChangeReason()), user.userId());
        }
        mapper.insertChangeHistory(saved.achievementId(), before == null ? "CREATE" : "UPDATE",
                before == null ? null : before.achievementDetail(), saved.achievementDetail(), user.userId(),
                text(request.getChangeReason()) == null ? "강의평가 실적 저장" : text(request.getChangeReason()));
        return saved;
    }

    private void requireRole(CurrentUser user) {
        if (user == null || user.roles() == null || user.roles().stream().noneMatch(ALLOWED_ROLES::contains)) throw new ForbiddenException();
    }
    private void validate(LectureEvaluationAchievementSaveRequest request) {
        if (request == null) throw new BusinessValidationException("강의평가 저장 요청이 필요합니다.", List.of());
        List<ValidationError> errors = new java.util.ArrayList<>();
        if (text(request.getManagementItemCode()) == null) errors.add(new ValidationError("managementItemCode", "관리항목 코드는 필수입니다."));
        if (request.getOccurredDate() == null) errors.add(new ValidationError("occurredDate", "업적발생일은 필수입니다."));
        if (text(request.getOrganizationCode()) == null) errors.add(new ValidationError("organizationCode", "소속 조직 코드는 필수입니다."));
        if (text(request.getAchievementDetail()) == null) errors.add(new ValidationError("achievementDetail", "강의평가 실적 내용은 필수입니다."));
        if (!errors.isEmpty()) throw new BusinessValidationException("강의평가 저장 요청이 올바르지 않습니다.", errors);
    }
    private void validateTransition(String from, String to, String reason, CurrentUser user) {
        boolean allowed = ("DRAFTING".equals(from) && "SUBMITTED".equals(to) && user.roles().contains("R01"))
                || ("SUBMITTED".equals(from) && ("DEPARTMENT_CONFIRMED".equals(to) || "DEPARTMENT_REJECTED".equals(to)) && user.roles().contains("R02"))
                || ("DEPARTMENT_CONFIRMED".equals(from) && ("CERTIFIED".equals(to) || "CERTIFICATION_RETURNED".equals(to)) && user.roles().contains("R04"));
        if (!allowed) throw new ConflictException("허용되지 않은 인증 상태 전이입니다.");
        if (("DEPARTMENT_REJECTED".equals(to) || "CERTIFICATION_RETURNED".equals(to)) && text(reason) == null) {
            throw new ConflictException("반려 처리에는 사유 또는 의견을 입력해야 합니다.");
        }
    }
    private String actionFor(String status) { return switch (status) { case "SUBMITTED" -> "SUBMIT"; case "DEPARTMENT_CONFIRMED" -> "CONFIRM"; case "DEPARTMENT_REJECTED" -> "REJECT"; case "CERTIFIED" -> "CERTIFY"; case "CERTIFICATION_RETURNED" -> "RETURN"; default -> "SUBMIT"; }; }
    private String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
