package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped course-operation reads and atomic guarded persistence with required audit history. */
@Service
public class CourseOperationAchievementService {
    private static final String TYPE = "COURSE_OPERATION";
    private final CourseOperationAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;

    public CourseOperationAchievementService(
            CourseOperationAchievementMapper mapper,
            EducationAchievementGuardService guardService) {
        this.mapper = mapper;
        this.guardService = guardService;
    }

    /** Lists only education achievements visible to the caller's established role scope. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(CourseOperationSearchCriteria criteria, CurrentUser requester) {
        requireReadRole(requester);
        CourseOperationSearchCriteria safe = criteria == null ? new CourseOperationSearchCriteria(0, 20) : criteria;
        return new CourseOperationSearchResponse(
                mapper.list(safe, requester.userId(), requester.roles()),
                safe.safePage(), safe.safePageSize(),
                mapper.count(safe, requester.userId(), requester.roles()));
    }

    /** Finds one achievement only after applying the same caller data scope as the list. */
    @Transactional(readOnly = true)
    public CourseOperationAchievementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        CourseOperationAchievementRow row = mapper.findInScope(achievementId, requester.userId(), requester.roles());
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Creates an R01-owned DRAFT source row and its lifecycle and change-history records atomically. */
    @Transactional
    public CourseOperationSaveResult create(CourseOperationRequest request, CurrentUser requester, String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation warning = guardService.validateMutation(requester,
                new EducationAchievementMutationContext(requester.userId(), evaluationYear, request.achievementDate()));
        String managementNo = "COA-" + UUID.randomUUID();
        mapper.insert(managementNo, requester.userId(), evaluationYear, request.managementItemCode().trim(),
                request.achievementDate(), request.performanceDetails().trim(), attachmentRef(request.attachmentIds()), requester.userId());
        CourseOperationAchievementRow saved = findByManagementNo(managementNo);
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(TYPE, saved.achievementId(), null,
                EducationAchievementStatus.DRAFT, "CREATE", null, "강좌 개설·운영 실적 최초 입력",
                requester.userId(), LocalDateTime.now()));
        mapper.insertChangeHistory("course_offering_operation_achievements", String.valueOf(saved.achievementId()),
                "CREATE", "achievement", null, saved.managementNo(), requester.userId(), "강좌 개설·운영 실적 저장", requestId);
        return result(saved, warning);
    }

    /** Updates an R01-owned row only after the shared period, scope, and finalization guards succeed. */
    @Transactional
    public CourseOperationSaveResult update(Long achievementId, CourseOperationRequest request, CurrentUser requester, String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        CourseOperationAchievementRow existing = find(achievementId);
        OccurredDateValidation warning = guardService.validateMutation(requester, new EducationAchievementMutationContext(
                existing.targetUserId(), existing.evaluationYear(), request.achievementDate()));
        mapper.update(existing.achievementId(), request.managementItemCode().trim(), request.achievementDate(),
                request.performanceDetails().trim(), attachmentRef(request.attachmentIds()), requester.userId());
        CourseOperationAchievementRow saved = find(achievementId);
        mapper.insertChangeHistory("course_offering_operation_achievements", String.valueOf(saved.achievementId()),
                "UPDATE", "achievement", existing.managementNo(), saved.managementNo(), requester.userId(),
                "강좌 개설·운영 실적 수정", requestId);
        return result(saved, warning);
    }

    private CourseOperationAchievementRow find(Long id) {
        CourseOperationAchievementRow row = mapper.findById(id);
        if (row == null) throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        return row;
    }

    private CourseOperationAchievementRow findByManagementNo(String managementNo) {
        CourseOperationAchievementRow row = mapper.findByManagementNo(managementNo);
        if (row == null) throw new NotFoundException("저장한 강좌 개설·운영 실적을 찾을 수 없습니다.");
        return row;
    }

    private CourseOperationSaveResult result(CourseOperationAchievementRow row, OccurredDateValidation warning) {
        return new CourseOperationSaveResult(row, warning.warning(), warning.message());
    }

    private void validateRequest(CourseOperationRequest request) {
        if (request == null) throw new BusinessValidationException("강좌 개설·운영 실적 정보를 입력하세요.", List.of(new ValidationError("body", "필수입니다.")));
        if (request.managementItemCode() == null || request.managementItemCode().isBlank()
                || request.achievementDate() == null || request.performanceDetails() == null || request.performanceDetails().isBlank()) {
            throw new BusinessValidationException("강좌 개설·운영 실적 저장 요청이 올바르지 않습니다.", List.of());
        }
        if (request.attachmentIds() != null && request.attachmentIds().size() > 1) {
            throw new BusinessValidationException("강좌 개설·운영 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "현재 첨부 참조는 한 건만 저장할 수 있습니다.")));
        }
    }

    private void requireReadRole(CurrentUser user) {
        if (user == null || user.userId() == null || user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04", "R09").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser user) {
        if (user == null || user.userId() == null || user.roles() == null || !user.roles().contains("R01")) throw new ForbiddenException();
    }

    private String attachmentRef(List<String> ids) {
        return ids == null || ids.isEmpty() || ids.get(0) == null || ids.get(0).isBlank() ? null : ids.get(0).trim();
    }
}
