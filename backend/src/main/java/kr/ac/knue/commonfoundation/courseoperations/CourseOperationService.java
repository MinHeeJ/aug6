package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
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

/**
 * Owns caller-scoped course-operation reads and atomic source/detail writes.
 * It invokes shared guards before mutations, then records lifecycle and audit
 * effects in the same transaction as the business data.
 */
@Service
public class CourseOperationService {
    private static final String ACHIEVEMENT_TYPE = "COURSE_OPERATION";
    private final CourseOperationMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public CourseOperationService(
            CourseOperationMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Lists only rows permitted by the shared R01/R02/R04 data scope. */
    @Transactional(readOnly = true)
    public CourseOperationListResponse list(int page, int pageSize, CurrentUser requester) {
        requireReadRole(requester);
        return new CourseOperationListResponse(
                mapper.listCourseOperations(
                        requester.userId(),
                        requester.roles(),
                        pageSize,
                        Math.multiplyExact(page, pageSize)),
                page,
                pageSize,
                mapper.countCourseOperations(requester.userId(), requester.roles()));
    }

    /** Reads one row after applying the same service-level ownership/data-scope guard. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        CourseOperationRow row = find(achievementId);
        authorizeRowAccess(row, requester);
        return row;
    }

    /**
     * Creates a course-operation achievement for the authenticated R01 actor.
     * The source, detail, initial status, and change history succeed or roll
     * back together.
     */
    @Transactional
    public CourseOperationSaveResponse create(
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriteRole(requester);
        String organizationCode = mapper.findActiveOrganizationCode(requester.userId());
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new ForbiddenException();
        }
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation validation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        CourseOperationCreateRow source = new CourseOperationCreateRow(
                "CO-" + UUID.randomUUID(),
                requester.userId(),
                organizationCode,
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentRefs(request.attachmentIds()),
                requester.userId());
        Long achievementId = mapper.insertAchievement(source);
        if (achievementId == null) {
            throw new NotFoundException("저장한 강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        mapper.insertDetails(achievementId, request.performanceDetails().trim(), requester.userId());
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                ACHIEVEMENT_TYPE,
                achievementId,
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "강좌 개설·운영 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()));
        CourseOperationRow saved = find(achievementId);
        mapper.insertChangeHistory(
                "education_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "course_operation",
                null,
                saved.performanceDetails(),
                requester.userId(),
                "강좌 개설·운영 실적 저장 (" + requestId + ")");
        return new CourseOperationSaveResponse(saved, validation.warning(), validation.message());
    }

    /** Updates an owned draft row after the shared period and confirmation guards. */
    @Transactional
    public CourseOperationSaveResponse update(
            Long achievementId,
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriteRole(requester);
        CourseOperationRow existing = find(achievementId);
        authorizeWriteOwnership(existing, requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation validation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        evaluationYear,
                        request.achievementDate()));
        mapper.updateAchievement(
                achievementId,
                request.managementItemCode().trim(),
                evaluationYear,
                request.achievementDate(),
                attachmentRefs(request.attachmentIds()),
                requester.userId());
        mapper.updateDetails(achievementId, request.performanceDetails().trim(), requester.userId());
        CourseOperationRow saved = find(achievementId);
        mapper.insertChangeHistory(
                "education_achievements",
                String.valueOf(achievementId),
                "UPDATE",
                "course_operation",
                existing.performanceDetails(),
                saved.performanceDetails(),
                requester.userId(),
                "강좌 개설·운영 실적 수정 (" + requestId + ")");
        return new CourseOperationSaveResponse(saved, validation.warning(), validation.message());
    }

    private CourseOperationRow find(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        CourseOperationRow row = mapper.findCourseOperation(achievementId);
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void authorizeRowAccess(CourseOperationRow row, CurrentUser requester) {
        if (mapper.countCourseOperationAccess(
                row.achievementId(),
                requester.userId(),
                requester.roles()) == 0) {
            throw new ForbiddenException();
        }
    }

    private void authorizeWriteOwnership(CourseOperationRow row, CurrentUser requester) {
        if (!requester.userId().equals(row.teacherUserId())) {
            throw new ForbiddenException();
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private void validate(CourseOperationRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "강좌 개설·운영 실적 정보를 입력하세요."));
        } else {
            if (blankToNull(request.managementItemCode()) == null) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.achievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
            if (blankToNull(request.performanceDetails()) == null) {
                errors.add(new ValidationError("performanceDetails", "실적내역을 입력하세요."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("강좌 개설·운영 실적 요청이 올바르지 않습니다.", errors);
        }
    }

    private String attachmentRefs(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(
                    attachmentIds == null
                            ? List.of()
                            : attachmentIds.stream()
                                    .filter(value -> value != null && !value.isBlank())
                                    .map(String::trim)
                                    .toList());
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 참조값이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 참조값을 확인하세요.")));
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
