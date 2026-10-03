package kr.ac.knue.commonfoundation.employmentrateimprovements;

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
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns scoped 취업률 제고 reads and atomic writes. It applies shared period and
 * finalization guards before persisting source data and its required change history.
 */
@Service
public class EmploymentRateImprovementService {
    private static final String TARGET_BUSINESS = "employment_rate_improvement_achievements";
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Returns only the rows permitted by the caller's role and data scope. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            EmploymentRateImprovementSearchCriteria criteria,
            CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateImprovementSearchCriteria safeCriteria = criteria == null
                ? new EmploymentRateImprovementSearchCriteria(0, 20, null, null, null, null)
                : criteria;
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.count(safeCriteria, requester.userId(), requester.roles()));
    }

    /** Returns a detail row after the same server-side scope rule as the list endpoint. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateImprovementRow row = requireExisting(achievementId);
        if (mapper.countAccessible(achievementId, requester.userId(), requester.roles()) == 0) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Creates an owned record only after validation, active-period, scope, and lock guards succeed. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriteRole(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation validation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String managementNo = "ERI-" + UUID.randomUUID();
        mapper.insert(
                managementNo,
                requester.userId(),
                evaluationYear,
                request,
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        EmploymentRateImprovementRow saved = requireSaved(managementNo);
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                "EMPLOYMENT_RATE_IMPROVEMENT",
                saved.achievementId(),
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "취업률 제고 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()));
        mapper.insertChangeHistory(
                TARGET_BUSINESS,
                String.valueOf(saved.achievementId()),
                "CREATE",
                null,
                serializeRow(saved),
                requester.userId(),
                "취업률 제고 실적 저장",
                requestId);
        return new EmploymentRateImprovementSaveResult(
                saved,
                validation.warning(),
                validation.message());
    }

    /** Updates only an owned, non-finalized record after all guards complete before the mapper write. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(
            Long achievementId,
            EmploymentRateImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriteRole(requester);
        EmploymentRateImprovementRow existing = requireExisting(achievementId);
        if (!requester.userId().equals(existing.targetUserId())) {
            throw new ForbiddenException();
        }
        if ("EVALUATION_CONFIRMED".equals(existing.certificationStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        String requestedEvaluationYear = String.valueOf(Year.from(request.achievementDate()));
        if (!existing.evaluationYear().equals(requestedEvaluationYear)) {
            throw new ConflictException(
                    "EVALUATION_YEAR_MISMATCH: 업적발생일은 기존 평가연도 내에서만 수정할 수 있습니다.");
        }
        OccurredDateValidation validation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.targetUserId(),
                        existing.evaluationYear(),
                        request.achievementDate()));
        mapper.update(
                achievementId,
                request,
                request.attachmentIds() == null
                        ? mapper.findAttachmentRefsById(achievementId)
                        : serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        EmploymentRateImprovementRow saved = requireExisting(achievementId);
        mapper.insertChangeHistory(
                TARGET_BUSINESS,
                String.valueOf(achievementId),
                "UPDATE",
                serializeRow(existing),
                serializeRow(saved),
                requester.userId(),
                "취업률 제고 실적 수정",
                requestId);
        return new EmploymentRateImprovementSaveResult(
                saved,
                validation.warning(),
                validation.message());
    }

    private EmploymentRateImprovementRow requireExisting(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        EmploymentRateImprovementRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private EmploymentRateImprovementRow requireSaved(String managementNo) {
        EmploymentRateImprovementRow row = mapper.findByManagementNo(managementNo);
        if (row == null) {
            throw new NotFoundException("저장한 취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void validate(EmploymentRateImprovementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "취업률 제고 실적 정보를 입력하세요."));
        } else {
            if (request.managementItemCode() == null || request.managementItemCode().isBlank()) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.achievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
            if (request.specialLectureStartDate() != null
                    && request.specialLectureEndDate() != null
                    && request.specialLectureEndDate().isBefore(request.specialLectureStartDate())) {
                errors.add(new ValidationError("specialLectureEndDate", "특강 종료일은 시작일보다 빠를 수 없습니다."));
            }
            validateAttachmentIds(request.attachmentIds(), errors);
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(
                    "취업률 제고 실적 저장 요청이 올바르지 않습니다. " + errors.get(0).message(),
                    errors);
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null
                || requester.userId() == null
                || requester.roles() == null
                || requester.roles().stream().noneMatch(List.of("R01", "R02", "R04")::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser requester) {
        if (requester == null
                || requester.userId() == null
                || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private String serializeAttachmentIds(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(
                    attachmentIds == null
                            ? List.of()
                            : attachmentIds.stream().filter(value -> value != null && !value.isBlank()).toList());
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 참조값이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 참조값을 확인하세요.")));
        }
    }

    private String serializeRow(EmploymentRateImprovementRow row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("변경 이력 직렬화에 실패했습니다.", exception);
        }
    }

    private void validateAttachmentIds(List<String> attachmentIds, List<ValidationError> errors) {
        if (attachmentIds == null) {
            return;
        }
        java.util.Set<String> references = new java.util.HashSet<>();
        for (int index = 0; index < attachmentIds.size(); index++) {
            String attachmentId = attachmentIds.get(index);
            if (attachmentId == null || attachmentId.isBlank()) {
                errors.add(new ValidationError("attachmentIds[" + index + "]", "첨부 참조값을 입력하세요."));
                continue;
            }
            if (!references.add(attachmentId.trim())) {
                errors.add(new ValidationError("attachmentIds[" + index + "]", "중복된 첨부 참조값은 사용할 수 없습니다."));
            }
        }
    }
}
