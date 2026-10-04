package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates course-operation reads and atomic header/detail/history writes.
 * The shared guard protects the target user's scope, input period, and finalized evaluation state.
 */
@Service
public class CourseOperationService {
    private static final String ACHIEVEMENT_TYPE = "COURSE_OPERATION";
    private final CourseOperationMapper mapper;
    private final EducationAchievementGuardService guardService;

    public CourseOperationService(
            CourseOperationMapper mapper,
            EducationAchievementGuardService guardService) {
        this.mapper = mapper;
        this.guardService = guardService;
    }

    /** Lists only dynamically scoped course-operation records for an authorized reader. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(
            CourseOperationSearchCriteria criteria,
            CurrentUser requester) {
        requireReader(requester);
        CourseOperationSearchCriteria safe = criteria == null
                ? new CourseOperationSearchCriteria(0, 20)
                : criteria;
        return new CourseOperationSearchResponse(
                mapper.list(safe, requester.userId(), requester.roles()),
                safe.safePage(),
                safe.safePageSize(),
                mapper.count(safe, requester.userId(), requester.roles()));
    }

    /** Returns one record only if it is within the caller's established data scope. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long achievementId, CurrentUser requester) {
        requireReader(requester);
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        CourseOperationRow row = mapper.findScoped(achievementId, requester.userId(), requester.roles());
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Creates a DRAFT header/detail pair and both required history records in one transaction. */
    @Transactional
    public CourseOperationRow create(
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriter(requester);
        validateRequest(request);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String organizationCode = mapper.findActiveOrganizationCode(requester.userId());
        if (organizationCode == null) {
            throw new BusinessValidationException(
                    "실적 대상자의 활성 소속을 찾을 수 없습니다.",
                    List.of(new ValidationError("organizationCode", "활성 소속을 확인하세요.")));
        }
        String managementNo = "CO-" + UUID.randomUUID();
        mapper.insertHeader(
                managementNo,
                requester.userId(),
                organizationCode,
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.performanceDetails().trim(),
                attachmentReference(request.attachmentIds()),
                requester.userId());
        CourseOperationRow saved = mapper.findByManagementNo(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        mapper.insertDetail(saved.achievementId(), request.performanceDetails().trim(), requester.userId());
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                ACHIEVEMENT_TYPE,
                saved.achievementId(),
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "강좌 개설·운영 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()));
        mapper.insertChangeHistory(
                "education_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "performance_detail",
                null,
                request.performanceDetails().trim(),
                requester.userId(),
                "강좌 개설·운영 실적 저장",
                requestId);
        return get(saved.achievementId(), requester);
    }

    /**
     * Updates a mutable record only after scope and the request-derived target evaluation year are validated.
     */
    @Transactional
    public CourseOperationRow update(
            Long achievementId,
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriter(requester);
        validateRequest(request);
        CourseOperationRow existing = get(achievementId, requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        evaluationYear,
                        request.achievementDate()));
        mapper.updateHeader(
                existing.achievementId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentReference(request.attachmentIds()),
                requester.userId());
        mapper.updateDetail(
                existing.achievementId(),
                request.performanceDetails().trim(),
                requester.userId());
        CourseOperationRow saved = get(existing.achievementId(), requester);
        mapper.insertChangeHistory(
                "education_achievements",
                String.valueOf(existing.achievementId()),
                "UPDATE",
                "performance_detail",
                existing.performanceDetails(),
                saved.performanceDetails(),
                requester.userId(),
                "강좌 개설·운영 실적 수정",
                requestId);
        return saved;
    }

    private void requireReader(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireWriter(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private void validateRequest(CourseOperationRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "강좌 개설·운영 실적 정보를 입력하세요.",
                    List.of(new ValidationError("body", "강좌 개설·운영 실적 정보를 입력하세요.")));
        }
    }

    private String attachmentReference(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return null;
        }
        List<String> nonBlankIds = attachmentIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
        String reference = String.join(",", nonBlankIds);
        return reference.isBlank() ? null : reference;
    }
}
