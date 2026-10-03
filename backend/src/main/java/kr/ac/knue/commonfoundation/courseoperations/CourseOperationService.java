package kr.ac.knue.commonfoundation.courseoperations;

import java.time.Year;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates caller-scoped course-operation reads and atomic source/detail
 * writes. Shared education guards run before mapper mutations to preserve the
 * active-period and confirmed-evaluation invariants.
 */
@Service
public class CourseOperationService {
    private final CourseOperationMapper mapper;
    private final EducationAchievementGuardService guardService;

    public CourseOperationService(
            CourseOperationMapper mapper,
            EducationAchievementGuardService guardService) {
        this.mapper = mapper;
        this.guardService = guardService;
    }

    /** Lists only records visible to the caller's approved education data scope. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(int page, int pageSize, CurrentUser requester) {
        requireReadRole(requester);
        int safePage = Math.max(page, 0);
        int safePageSize = pageSize == 50 || pageSize == 100 ? pageSize : 20;
        boolean selfOnly = requester.roles().contains("R01");
        return new CourseOperationSearchResponse(
                mapper.list(requester.userId(), selfOnly, safePageSize, safePage * safePageSize),
                safePage,
                safePageSize,
                mapper.count(requester.userId(), selfOnly));
    }

    /** Returns a record after enforcing its visibility to the current principal. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        CourseOperationRow row = requireExisting(achievementId);
        if (requester.roles().contains("R01") && !requester.userId().equals(row.getTeacherUserId())) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Creates an owned course-operation record together with its required detail and audit row. */
    @Transactional
    public CourseOperationRow create(
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriteRole(requester);
        validate(request);
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        CourseOperationCommand command = command(null, requester.userId(), request);
        mapper.insertAchievement(command);
        mapper.insertDetails(command);
        mapper.insertChangeHistory(
                command.getAchievementId(),
                "CREATE",
                null,
                request.performanceDetails().trim(),
                requester.userId(),
                warning.warning() ? warning.message() : "강좌 개설·운영 실적 등록",
                requestId);
        return requireExisting(command.getAchievementId());
    }

    /** Updates an owned unconfirmed record only after all guards have succeeded. */
    @Transactional
    public CourseOperationRow update(
            Long achievementId,
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriteRole(requester);
        validate(request);
        CourseOperationRow existing = requireExisting(achievementId);
        if (!requester.userId().equals(existing.getTeacherUserId())) {
            throw new ForbiddenException();
        }
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.getTeacherUserId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        CourseOperationCommand command = command(achievementId, existing.getTeacherUserId(), request);
        mapper.updateAchievement(command);
        mapper.updateDetails(command);
        mapper.insertChangeHistory(
                achievementId,
                "UPDATE",
                existing.getPerformanceDetails(),
                request.performanceDetails().trim(),
                requester.userId(),
                "강좌 개설·운영 실적 수정",
                requestId);
        return requireExisting(achievementId);
    }

    private CourseOperationCommand command(
            Long achievementId,
            Long teacherUserId,
            CourseOperationRequest request) {
        return new CourseOperationCommand(
                achievementId,
                teacherUserId,
                teacherUserId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.performanceDetails().trim(),
                request.attachmentIds() == null
                        ? new String[0]
                        : request.attachmentIds().toArray(String[]::new));
    }

    private CourseOperationRow requireExisting(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        CourseOperationRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void validate(CourseOperationRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "강좌 개설·운영 실적 정보를 입력하세요.",
                    List.of(new ValidationError("body", "요청 본문을 입력하세요.")));
        }
        if (request.performanceDetails() == null || request.performanceDetails().isBlank()) {
            throw new BusinessValidationException(
                    "강좌 개설·운영 실적 정보가 올바르지 않습니다.",
                    List.of(new ValidationError("performanceDetails", "실적내역을 입력하세요.")));
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }
}
