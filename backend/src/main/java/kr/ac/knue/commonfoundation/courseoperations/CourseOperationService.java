package kr.ac.knue.commonfoundation.courseoperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.List;
import java.util.Objects;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides caller-scoped reads and guarded, auditable mutations for COURSE_OPERATION
 * rows held in the BASIC-83 education-achievement master/detail schema.
 */
@Service
public class CourseOperationService {
    private static final String ACHIEVEMENT_TYPE = "COURSE_OPERATION";
    private static final List<String> READ_ROLES = List.of("R01", "R02", "R04");

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

    /** Lists only achievements within the authenticated caller's education-achievement data scope. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(int page, int pageSize, CurrentUser requester) {
        requireReadRole(requester);
        List<CourseOperationResponse> achievements = mapper.list(
                        pageSize,
                        page * pageSize,
                        requester.userId(),
                        requester.roles())
                .stream()
                .map(this::toResponse)
                .toList();
        return new CourseOperationSearchResponse(
                achievements,
                page,
                pageSize,
                mapper.count(requester.userId(), requester.roles()));
    }

    /** Returns one caller-scoped course-operation achievement or hides an out-of-scope row as absent. */
    @Transactional(readOnly = true)
    public CourseOperationResponse get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        CourseOperationRow row = mapper.findScoped(achievementId, requester.userId(), requester.roles());
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return toResponse(row);
    }

    /** Creates a draft achievement and its required status and audit history in one transaction. */
    @Transactional
    public CourseOperationSaveResponse create(
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriterRole(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation validation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String organizationCode = mapper.findPrimaryOrganizationCode(requester.userId());
        if (organizationCode == null) {
            throw new ForbiddenException();
        }
        Long savedId = mapper.insertAchievement(
                requester.userId(),
                organizationCode,
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                achievementName(request),
                request.performanceDetails().trim(),
                serializeAttachments(request.attachmentIds()),
                requester.userId());
        if (savedId == null) {
            throw new IllegalStateException("강좌 개설·운영 실적 식별자를 반환하지 못했습니다.");
        }
        mapper.insertAchievementDetail(savedId, request.performanceDetails().trim());
        CourseOperationRow saved = mapper.findById(savedId);
        if (saved == null) {
            throw new NotFoundException("저장한 강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        mapper.insertStatusHistory(saved.achievementId(), null, "DRAFT", requester.userId());
        mapper.insertChangeHistory(
                String.valueOf(saved.achievementId()),
                "CREATE",
                null,
                saved.performanceDetails(),
                requester.userId(),
                requestId);
        return new CourseOperationSaveResponse(
                toResponse(saved),
                validation.warning(),
                validation.message());
    }

    /** Updates a mutable achievement after the shared period, confirmation, role, and data-scope checks. */
    @Transactional
    public CourseOperationSaveResponse update(
            Long achievementId,
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriterRole(requester);
        CourseOperationRow existing = mapper.findById(achievementId);
        if (existing == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        requireWritableScope(existing, requester);
        if ("EVALUATION_CONFIRMED".equals(existing.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        OccurredDateValidation validation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        mapper.updateAchievement(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                achievementName(request),
                request.performanceDetails().trim(),
                serializeAttachments(request.attachmentIds()),
                requester.userId());
        mapper.updateDetail(achievementId, request.performanceDetails().trim());
        CourseOperationRow saved = mapper.findById(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                existing.performanceDetails(),
                saved.performanceDetails(),
                requester.userId(),
                requestId);
        return new CourseOperationSaveResponse(
                toResponse(saved),
                validation.warning(),
                validation.message());
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null
                || requester.roles().stream().noneMatch(READ_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireWriterRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    /**
     * Enforces ownership before a write because the approved contract permits
     * only R01 to mutate this resource; reading a row without a scope predicate
     * must never grant that caller write access to another teacher's data.
     */
    private void requireWritableScope(CourseOperationRow existing, CurrentUser requester) {
        if (!Objects.equals(existing.teacherUserId(), requester.userId())) {
            throw new ForbiddenException();
        }
    }

    /**
     * Uses the required performance detail as the display name because the
     * approved CourseOperationRequest has no achievementName property while
     * the shared education-achievements master requires a persisted title.
     */
    private String achievementName(CourseOperationRequest request) {
        return request.performanceDetails().trim();
    }

    private String serializeAttachments(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(attachmentIds == null ? List.of() : attachmentIds);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("첨부파일 식별자 형식이 올바르지 않습니다.", exception);
        }
    }

    private CourseOperationResponse toResponse(CourseOperationRow row) {
        return new CourseOperationResponse(
                row.achievementId(),
                row.teacherName(),
                row.organizationCode(),
                row.evaluationYear(),
                row.managementItemCode(),
                row.achievementDate(),
                row.achievementName(),
                row.performanceDetails(),
                deserializeAttachments(row.attachmentIdsJson()),
                row.achievementStatus(),
                row.createdAt(),
                row.updatedAt());
    }

    private List<String> deserializeAttachments(String attachmentIdsJson) {
        try {
            return attachmentIdsJson == null
                    ? List.of()
                    : objectMapper.readValue(attachmentIdsJson, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 첨부파일 식별자 형식이 올바르지 않습니다.", exception);
        }
    }
}
