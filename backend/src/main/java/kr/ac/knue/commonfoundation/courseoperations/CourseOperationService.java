package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates scoped Course Operations reads and atomic header, detail, lifecycle,
 * and audit persistence for the BASIC-83 course-operation workflow.
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

    /** Returns only rows visible through the shared R01/R02/R04 achievement data scope. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(
            CourseOperationSearchCriteria criteria,
            CurrentUser requester) {
        requireReadableRole(requester);
        CourseOperationSearchCriteria safeCriteria = criteria == null
                ? new CourseOperationSearchCriteria(0, 20)
                : criteria;
        return new CourseOperationSearchResponse(
                mapper.listCourseOperations(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.countCourseOperations(requester.userId(), requester.roles()));
    }

    /** Resolves one row only after proving the caller can read the row's teacher scope. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long achievementId, CurrentUser requester) {
        requireReadableRole(requester);
        CourseOperationRow row = findExisting(achievementId);
        requireReadableScope(achievementId, requester);
        return row;
    }

    /**
     * Creates the common header and detail together after the shared period and
     * finalization guards have approved the R01 caller's own achievement.
     */
    @Transactional
    public CourseOperationRow create(
            CourseOperationSaveRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriter(requester);
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        String organizationCode = mapper.findActiveOrganizationCode(requester.userId());
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new ConflictException("ORGANIZATION_SCOPE_REQUIRED: 활성 소속이 없어 실적을 저장할 수 없습니다.");
        }
        Long achievementId = mapper.insertCourseOperation(
                "B83-CO-" + UUID.randomUUID(),
                requester.userId(),
                organizationCode,
                String.valueOf(Year.from(request.achievementDate())),
                request.managementItemCode().trim(),
                request.achievementDate(),
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        mapper.insertCourseOperationDetail(achievementId, request.performanceDetails().trim());
        mapper.insertStatusHistory(
                achievementId,
                null,
                "DRAFT",
                "CREATE",
                "강좌 개설·운영 실적 최초 입력",
                requester.userId());
        CourseOperationRow saved = findExisting(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "CREATE",
                null,
                saved.performanceDetails(),
                requester.userId(),
                requestId);
        return saved;
    }

    /**
     * Updates mutable header and performance-detail fields in one transaction;
     * the guard rejects evaluation-confirmed and out-of-period mutations first.
     */
    @Transactional
    public CourseOperationRow update(
            Long achievementId,
            CourseOperationSaveRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriter(requester);
        CourseOperationRow existing = findExisting(achievementId);
        requireReadableScope(achievementId, requester);
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        existing.evaluationYear(),
                        request.achievementDate()));
        mapper.updateCourseOperationHeader(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        mapper.updateCourseOperationDetail(achievementId, request.performanceDetails().trim());
        CourseOperationRow saved = findExisting(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                existing.performanceDetails(),
                saved.performanceDetails(),
                requester.userId(),
                requestId);
        return saved;
    }

    private CourseOperationRow findExisting(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        CourseOperationRow row = mapper.findCourseOperation(achievementId);
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireReadableScope(Long achievementId, CurrentUser requester) {
        if (mapper.countReadableScope(achievementId, requester.userId(), requester.roles()) == 0) {
            throw new ForbiddenException();
        }
    }

    private void requireReadableRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(List.of("R01", "R02", "R04")::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireWriter(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private void validateRequest(CourseOperationSaveRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "강좌 개설·운영 실적 정보를 입력하세요."));
        } else {
            if (request.managementItemCode() == null || request.managementItemCode().isBlank()) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.achievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
            if (request.performanceDetails() == null || request.performanceDetails().isBlank()) {
                errors.add(new ValidationError("performanceDetails", "실적내역을 입력하세요."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("강좌 개설·운영 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private String serializeAttachmentIds(List<String> attachmentIds) {
        List<String> safeIds = attachmentIds == null
                ? List.of()
                : attachmentIds.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
        try {
            return objectMapper.writeValueAsString(safeIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 식별자 형식이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 식별자를 확인하세요.")));
        }
    }
}
