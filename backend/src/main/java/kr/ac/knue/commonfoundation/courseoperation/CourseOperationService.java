package kr.ac.knue.commonfoundation.courseoperation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements course-operation reads and guarded mutations. Writes use the
 * shared education guard before persistence so inactive periods and finalized
 * evaluations cannot leave partial source or audit records.
 */
@Service
public class CourseOperationService {
    private static final Set<String> READ_ROLES = Set.of("R01", "R02", "R04");
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

    /** Returns only rows visible to the caller's education-achievement data scope. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(int page, int pageSize, CurrentUser requester) {
        requireReadable(requester);
        int safePage = Math.max(page, 0);
        int safePageSize = pageSize == 50 || pageSize == 100 ? pageSize : 20;
        List<CourseOperationResponse> rows = mapper.list(
                        requester.userId(),
                        requester.roles(),
                        safePageSize,
                        safePage * safePageSize)
                .stream()
                .map(this::toResponse)
                .toList();
        return new CourseOperationSearchResponse(
                rows,
                safePage,
                safePageSize,
                mapper.count(requester.userId(), requester.roles()));
    }

    /** Finds one record and rejects callers outside its data scope instead of exposing its contents. */
    @Transactional(readOnly = true)
    public CourseOperationResponse get(Long achievementId, CurrentUser requester) {
        requireReadable(requester);
        CourseOperationRow row = find(achievementId);
        requireDataScope(row, requester);
        return toResponse(row);
    }

    /** Creates a draft course-operation achievement with its audit history in one transaction. */
    @Transactional
    public CourseOperationResponse create(
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriter(requester);
        String attachmentIdsJson = attachmentIdsJson(request.attachmentIds());
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String managementNo = "CO-" + UUID.randomUUID();
        mapper.insert(
                managementNo,
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.performanceDetails().trim(),
                attachmentIdsJson,
                requester.userId());
        CourseOperationRow saved = findByManagementNo(managementNo);
        mapper.insertChangeHistory(
                String.valueOf(saved.achievementId()),
                "CREATE",
                null,
                saved.performanceDetails(),
                requester.userId(),
                "강좌 개설·운영 실적 저장",
                requestId);
        return toResponse(saved);
    }

    /** Updates only a caller-owned draft/editable course-operation row after shared guards run. */
    @Transactional
    public CourseOperationResponse update(
            Long achievementId,
            CourseOperationRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriter(requester);
        CourseOperationRow existing = find(achievementId);
        requireDataScope(existing, requester);
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.targetUserId(),
                        existing.evaluationYear(),
                        request.achievementDate()));
        int changed = mapper.update(
                existing.achievementId(),
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.performanceDetails().trim(),
                attachmentIdsJson(request.attachmentIds()),
                requester.userId());
        if (changed != 1) {
            throw new NotFoundException("수정할 강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        CourseOperationRow saved = find(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(saved.achievementId()),
                "UPDATE",
                existing.performanceDetails(),
                saved.performanceDetails(),
                requester.userId(),
                "강좌 개설·운영 실적 수정",
                requestId);
        return toResponse(saved);
    }

    private CourseOperationRow find(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new BusinessValidationException(
                    "강좌 개설·운영 실적 식별자가 올바르지 않습니다.",
                    List.of(new ValidationError("achievementId", "실적 식별자를 입력하세요.")));
        }
        CourseOperationRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private CourseOperationRow findByManagementNo(String managementNo) {
        CourseOperationRow saved = mapper.findByManagementNo(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return saved;
    }

    private void requireReadable(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(READ_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireWriter(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private void requireDataScope(CourseOperationRow row, CurrentUser requester) {
        if (mapper.countAccessible(
                row.achievementId(),
                requester.userId(),
                requester.roles()) == 0) {
            throw new ForbiddenException();
        }
    }

    private void validate(CourseOperationRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "강좌 개설·운영 실적 정보를 입력하세요.",
                    List.of(new ValidationError("body", "저장 정보를 입력하세요.")));
        }
        List<ValidationError> errors = new ArrayList<>();
        if (blankToNull(request.managementItemCode()) == null) {
            errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
        }
        if (request.achievementDate() == null) {
            errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
        }
        if (blankToNull(request.performanceDetails()) == null) {
            errors.add(new ValidationError("performanceDetails", "실적내역을 입력하세요."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("강좌 개설·운영 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String attachmentIdsJson(List<String> attachmentIds) {
        try {
            List<String> normalized = attachmentIds == null
                    ? List.of()
                    : attachmentIds.stream()
                            .filter(id -> id != null && !id.isBlank())
                            .map(String::trim)
                            .toList();
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 참조값이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 참조값을 확인하세요.")));
        }
    }

    private CourseOperationResponse toResponse(CourseOperationRow row) {
        try {
            List<String> attachmentIds = row.attachmentIdsJson() == null
                    ? List.of()
                    : objectMapper.readValue(row.attachmentIdsJson(), new TypeReference<List<String>>() { });
            return new CourseOperationResponse(
                    row.achievementId(),
                    row.managementNo(),
                    row.teacherName(),
                    row.evaluationYear(),
                    row.managementItemCode(),
                    row.achievementDate(),
                    row.performanceDetails(),
                    attachmentIds,
                    row.achievementStatus(),
                    row.createdAt(),
                    row.updatedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 첨부 참조값을 읽을 수 없습니다.", exception);
        }
    }
}
