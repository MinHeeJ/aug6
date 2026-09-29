package kr.ac.knue.commonfoundation.lectureachievements;

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
 * Persists lecture achievements through the established education-achievement lifecycle.
 * It prevents final-result mutations and writes status/change evidence in the same transaction.
 */
@Service
public class LectureAchievementService {
    private static final Set<String> ALLOWED_ROLES = Set.of("R01", "R02", "R04");
    private static final Set<Integer> PAGE_SIZES = Set.of(20, 50, 100);
    private final LectureAchievementMapper mapper;

    public LectureAchievementService(LectureAchievementMapper mapper) { this.mapper = mapper; }

    /** Returns data-backed rows, adding only populated optional predicates in the mapper. */
    @Transactional(readOnly = true)
    public LectureAchievementSearchResponse list(LectureAchievementSearchCriteria criteria, int page, int pageSize, CurrentUser user) {
        requireRole(user);
        int safePage = Math.max(0, page);
        int safeSize = PAGE_SIZES.contains(pageSize) ? pageSize : 20;
        LectureAchievementSearchCriteria normalized = new LectureAchievementSearchCriteria(
                text(criteria == null ? null : criteria.managementNo()), text(criteria == null ? null : criteria.teacherName()),
                text(criteria == null ? null : criteria.managementItemCode()), text(criteria == null ? null : criteria.certificationStatus()));
        return new LectureAchievementSearchResponse(mapper.list(normalized, safeSize, safePage * safeSize), safePage, safeSize, mapper.count(normalized));
    }

    /** Saves a draft/update, attachment reference, lifecycle transition, and audit evidence atomically. */
    @Transactional
    public LectureAchievementRow save(LectureAchievementSaveRequest request, CurrentUser user) {
        requireRole(user);
        validate(request);
        LectureAchievementRow before = request.getAchievementId() == null ? null : mapper.find(request.getAchievementId());
        if (request.getAchievementId() != null && before == null) throw new NotFoundException("강의실적을 찾을 수 없습니다.");
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
        LectureAchievementRow saved = mapper.find(request.getAchievementId());
        if (saved == null) throw new IllegalStateException("저장한 강의실적을 다시 조회할 수 없습니다.");
        if (previousStatus != null && !previousStatus.equals(nextStatus)) {
            mapper.insertStatusHistory(saved.achievementId(), previousStatus, nextStatus, actionFor(nextStatus), text(request.getChangeReason()), user.userId());
        }
        mapper.insertChangeHistory(saved.achievementId(), before == null ? "CREATE" : "UPDATE",
                before == null ? null : before.achievementDetail(), saved.achievementDetail(), user.userId(),
                text(request.getChangeReason()) == null ? "강의실적 저장" : text(request.getChangeReason()));
        return saved;
    }

    private void requireRole(CurrentUser user) {
        if (user == null || user.roles() == null || user.roles().stream().noneMatch(ALLOWED_ROLES::contains)) throw new ForbiddenException();
    }

    private void validate(LectureAchievementSaveRequest request) {
        if (request == null) throw new BusinessValidationException("강의실적 저장 요청이 필요합니다.", List.of());
        List<ValidationError> errors = new java.util.ArrayList<>();
        if (text(request.getManagementItemCode()) == null) errors.add(new ValidationError("managementItemCode", "관리항목 코드는 필수입니다."));
        if (request.getOccurredDate() == null) errors.add(new ValidationError("occurredDate", "업적발생일은 필수입니다."));
        if (text(request.getOrganizationCode()) == null) errors.add(new ValidationError("organizationCode", "소속 조직 코드는 필수입니다."));
        if (text(request.getAchievementDetail()) == null) errors.add(new ValidationError("achievementDetail", "강의실적 내용은 필수입니다."));
        if (!errors.isEmpty()) throw new BusinessValidationException("강의실적 저장 요청이 올바르지 않습니다.", errors);
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

    private String actionFor(String status) {
        return switch (status) {
            case "SUBMITTED" -> "SUBMIT";
            case "DEPARTMENT_CONFIRMED" -> "CONFIRM";
            case "DEPARTMENT_REJECTED" -> "REJECT";
            case "CERTIFIED" -> "CERTIFY";
            case "CERTIFICATION_RETURNED" -> "RETURN";
            default -> "SUBMIT";
        };
    }

    private String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
