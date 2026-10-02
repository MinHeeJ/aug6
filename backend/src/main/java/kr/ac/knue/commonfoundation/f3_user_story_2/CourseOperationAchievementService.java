package kr.ac.knue.commonfoundation.f3_user_story_2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns caller-scoped course-operation reads and atomic writes. It applies the
 * existing education-period and evaluation-finalization guards before mutation.
 */
@Service
public class CourseOperationAchievementService {
    private static final List<String> READ_ROLES = List.of("R01", "R02", "R04");
    private final CourseOperationAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public CourseOperationAchievementService(
            CourseOperationAchievementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Returns only records in the caller's role-derived data scope. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(
            CourseOperationSearchCriteria criteria,
            CurrentUser requester) {
        requireReadRole(requester);
        CourseOperationSearchCriteria safeCriteria = criteria == null
                ? new CourseOperationSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        List<CourseOperationResponse> rows = mapper.list(
                        safeCriteria,
                        requester.userId(),
                        requester.roles())
                .stream()
                .map(this::toResponse)
                .toList();
        return new CourseOperationSearchResponse(
                rows,
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.count(safeCriteria, requester.userId(), requester.roles()));
    }

    /** Finds a single visible record after enforcing its owner/scope boundary. */
    @Transactional(readOnly = true)
    public CourseOperationResponse get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        CourseOperationRow row = requireRow(achievementId);
        requireReadableRow(row, requester);
        return toResponse(row);
    }

    /** Creates a draft achievement and its audit entry in one database transaction. */
    @Transactional
    public CourseOperationResponse create(
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriteRole(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String managementNo = "COO-" + UUID.randomUUID();
        mapper.insert(
                managementNo,
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.performanceDetails().trim(),
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        CourseOperationRow saved = mapper.findByManagementNo(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        mapper.insertChangeHistory(
                String.valueOf(saved.achievementId()),
                "CREATE",
                null,
                saved.performanceDetail(),
                requester.userId(),
                "강좌 개설·운영 실적 등록",
                requestId);
        return toResponse(saved);
    }

    /** Updates an editable own record while preserving confirmed data unchanged. */
    @Transactional
    public CourseOperationResponse update(
            Long achievementId,
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriteRole(requester);
        CourseOperationRow existing = requireRow(achievementId);
        if (!requester.userId().equals(existing.targetUserId())) {
            throw new ForbiddenException();
        }
        if ("EVALUATION_CONFIRMED".equals(existing.certificationStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.targetUserId(),
                        existing.evaluationYear(),
                        request.achievementDate()));
        if (mapper.update(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.performanceDetails().trim(),
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId()) != 1) {
            throw new NotFoundException("수정할 강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        CourseOperationRow saved = requireRow(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                existing.performanceDetail(),
                saved.performanceDetail(),
                requester.userId(),
                "강좌 개설·운영 실적 수정",
                requestId);
        return toResponse(saved);
    }

    private CourseOperationRow requireRow(Long achievementId) {
        CourseOperationRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(READ_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private void requireReadableRow(CourseOperationRow row, CurrentUser requester) {
        if (mapper.countReadableById(
                row.achievementId(),
                requester.userId(),
                requester.roles()) != 1) {
            throw new ForbiddenException();
        }
    }

    private CourseOperationResponse toResponse(CourseOperationRow row) {
        return new CourseOperationResponse(
                row.achievementId(),
                row.managementNo(),
                row.teacherName(),
                row.evaluationYear(),
                row.managementItemCode(),
                row.occurredDate(),
                row.performanceDetail(),
                row.certificationStatus(),
                deserializeAttachmentIds(row.attachmentIds()));
    }

    private String serializeAttachmentIds(List<String> attachmentIds) {
        try {
            List<String> safeAttachmentIds = attachmentIds == null
                    ? List.of()
                    : attachmentIds.stream()
                            .filter(value -> value != null && !value.isBlank())
                            .toList();
            return objectMapper.writeValueAsString(safeAttachmentIds);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("첨부파일 식별자를 처리할 수 없습니다.", exception);
        }
    }

    private List<String> deserializeAttachmentIds(String attachmentIds) {
        if (attachmentIds == null || attachmentIds.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(attachmentIds, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 첨부파일 식별자를 읽을 수 없습니다.", exception);
        }
    }
}
