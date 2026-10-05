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
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns caller-scoped course-operation reads and the atomic header/detail/audit
 * write flow required for the education-achievement lifecycle.
 */
@Service
public class CourseOperationService {
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

    /** Returns only rows in the authenticated caller's education-achievement scope. */
    @Transactional(readOnly = true)
    public CourseOperationSearchResponse list(int page, int pageSize, CurrentUser requester) {
        requireReadRole(requester);
        int safePage = Math.max(page, 0);
        return new CourseOperationSearchResponse(
                mapper.list(requester.userId(), requester.roles(), pageSize, safePage * pageSize),
                safePage,
                pageSize,
                mapper.count(requester.userId(), requester.roles()));
    }

    /** Returns a visible course-operation row without exposing a row outside caller scope. */
    @Transactional(readOnly = true)
    public CourseOperationRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        CourseOperationRow row = mapper.findScopedById(achievementId, requester.userId(), requester.roles());
        if (row == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Creates a DRAFT course-operation header and detail after all write guards pass. */
    @Transactional
    public CourseOperationSaveResponse create(
            CourseOperationRequest request,
            CurrentUser requester) {
        validate(request);
        requireWriteRole(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation dateValidation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String managementNo = "CO-" + UUID.randomUUID();
        Long achievementId = mapper.insertHeader(
                managementNo,
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.performanceDetails().trim(),
                attachmentIdsJson(request.attachmentIds()),
                requester.userId());
        if (achievementId == null) {
            throw new NotFoundException("저장한 강좌 개설·운영 실적 식별자를 찾을 수 없습니다.");
        }
        mapper.insertDetail(achievementId, request.performanceDetails().trim(), requester.userId());
        mapper.insertInitialStatusHistory(achievementId, requester.userId());
        CourseOperationRow reread = mapper.findById(achievementId);
        if (reread == null) {
            throw new NotFoundException("저장한 강좌 개설·운영 상세를 찾을 수 없습니다.");
        }
        mapper.insertChangeHistory(
                String.valueOf(reread.achievementId()),
                "CREATE",
                null,
                reread.performanceDetails(),
                requester.userId(),
                "강좌 개설·운영 실적 저장");
        return new CourseOperationSaveResponse(
                reread,
                dateValidation.warning(),
                dateValidation.message());
    }

    /** Updates an R01-owned unfinalized row and records the previous detail value. */
    @Transactional
    public CourseOperationSaveResponse update(
            Long achievementId,
            CourseOperationRequest request,
            CurrentUser requester) {
        validate(request);
        requireWriteRole(requester);
        CourseOperationRow existing = mapper.findById(achievementId);
        if (existing == null) {
            throw new NotFoundException("강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        if (!requester.userId().equals(existing.teacherUserId())) {
            throw new ForbiddenException();
        }
        requireDraftStatus(existing);
        requireMatchingEvaluationYear(existing, request);
        OccurredDateValidation dateValidation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        existing.evaluationYear(),
                        request.achievementDate()));
        mapper.updateHeader(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.performanceDetails().trim(),
                attachmentIdsJson(request.attachmentIds()),
                requester.userId());
        mapper.updateDetail(achievementId, request.performanceDetails().trim(), requester.userId());
        CourseOperationRow reread = mapper.findById(achievementId);
        if (reread == null) {
            throw new NotFoundException("수정한 강좌 개설·운영 실적을 찾을 수 없습니다.");
        }
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                existing.performanceDetails(),
                reread.performanceDetails(),
                requester.userId(),
                "강좌 개설·운영 실적 수정");
        return new CourseOperationSaveResponse(
                reread,
                dateValidation.warning(),
                dateValidation.message());
    }

    private void validate(CourseOperationRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("body", "강좌 개설·운영 실적 정보를 입력하세요."));
        } else {
            if (request.managementItemCode() == null || request.managementItemCode().isBlank()) {
                fields.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.achievementDate() == null) {
                fields.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
            if (request.performanceDetails() == null || request.performanceDetails().isBlank()) {
                fields.add(new ValidationError("performanceDetails", "실적내역을 입력하세요."));
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("강좌 개설·운영 실적 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    /**
     * Keeps the header evaluation year immutable because this request has no
     * evaluation-year field; changing only the date would corrupt that identity.
     */
    private void requireMatchingEvaluationYear(
            CourseOperationRow existing,
            CourseOperationRequest request) {
        String requestedYear = String.valueOf(request.achievementDate().getYear());
        if (!existing.evaluationYear().equals(requestedYear)) {
            throw new BusinessValidationException(
                    "업적발생일은 기존 평가연도와 같아야 합니다.",
                    List.of(new ValidationError(
                            "achievementDate",
                            "기존 평가연도 " + existing.evaluationYear() + " 내의 날짜를 입력하세요.")));
        }
    }

    /** Only DRAFT rows may be edited; later lifecycle states are immutable. */
    private void requireDraftStatus(CourseOperationRow existing) {
        if (!"DRAFT".equals(existing.achievementStatus())) {
            throw new ConflictException(
                    "ACHIEVEMENT_NOT_EDITABLE: 작성중 상태의 실적만 수정할 수 있습니다.");
        }
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

    private String attachmentIdsJson(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(attachmentIds == null ? List.of() : attachmentIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 식별자 형식이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 식별자 목록을 확인하세요.")));
        }
    }
}
