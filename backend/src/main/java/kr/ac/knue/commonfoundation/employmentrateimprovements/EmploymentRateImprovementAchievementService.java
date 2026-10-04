package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
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
 * Owns caller-scoped 취업률 제고 reads and atomic writes to the BASIC-83 master,
 * detail, status-history, and data-change-history records.
 */
@Service
public class EmploymentRateImprovementAchievementService {
    private static final String CONFIRMED_STATUS = "EVALUATION_CONFIRMED";
    private final EmploymentRateImprovementAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public EmploymentRateImprovementAchievementService(
            EmploymentRateImprovementAchievementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Returns the authenticated user's permitted page of 취업률 제고 achievement rows. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            int page,
            int pageSize,
            CurrentUser requester) {
        requireReadRole(requester);
        int safePage = Math.max(0, page);
        List<EmploymentRateImprovementAchievementResponse> rows = mapper.list(
                requester.userId(),
                requester.roles(),
                pageSize,
                Math.multiplyExact(safePage, pageSize)).stream().map(this::toResponse).toList();
        return new EmploymentRateImprovementSearchResponse(
                rows,
                safePage,
                pageSize,
                mapper.count(requester.userId(), requester.roles()));
    }

    /** Returns one row only when it is inside the current user's permitted data scope. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementAchievementResponse get(
            Long achievementId,
            CurrentUser requester) {
        requireReadRole(requester);
        return toResponse(findScoped(achievementId, requester));
    }

    /**
     * Creates a DRAFT master/detail record after shared period and data-scope
     * guards; status and audit records are persisted in the same transaction.
     */
    @Transactional
    public EmploymentRateImprovementSaveResponse create(
            EmploymentRateImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String organizationCode = mapper.findActiveOrganizationCode(requester.userId());
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new BusinessValidationException(
                    "취업률 제고 실적의 소속 정보를 찾을 수 없습니다.",
                    List.of(new ValidationError("organizationCode", "활성 소속이 필요합니다.")));
        }
        String attachmentIdsJson = serializeAttachmentIds(request.attachmentIds());
        Long achievementId = mapper.insertAchievement(
                requester.userId(),
                organizationCode,
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentIdsJson,
                requester.userId());
        if (achievementId == null) {
            throw new IllegalStateException("취업률 제고 실적 식별자를 생성하지 못했습니다.");
        }
        mapper.insertDetail(
                achievementId,
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                blankToNull(request.mockExamQuestionPeriod()));
        mapper.insertCreateStatusHistory(
                achievementId,
                "취업률 제고 실적 최초 입력",
                requester.userId());
        EmploymentRateImprovementAchievementEntity saved = findScoped(achievementId, requester);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "CREATE",
                null,
                auditJson(saved),
                requester.userId(),
                "취업률 제고 실적 저장",
                requestId);
        return new EmploymentRateImprovementSaveResponse(
                toResponse(saved),
                warning.warning(),
                warning.message());
    }

    /**
     * Updates only a caller-owned non-confirmed row after common scope, period,
     * and finalization guards have passed, preserving the prior audit snapshot.
     */
    @Transactional
    public EmploymentRateImprovementSaveResponse update(
            Long achievementId,
            EmploymentRateImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        requireWriteRole(requester);
        EmploymentRateImprovementAchievementEntity existing = findScoped(achievementId, requester);
        if (CONFIRMED_STATUS.equals(existing.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        String attachmentIdsJson = serializeAttachmentIds(request.attachmentIds());
        mapper.updateAchievement(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentIdsJson,
                requester.userId());
        mapper.updateDetail(
                achievementId,
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                blankToNull(request.mockExamQuestionPeriod()));
        EmploymentRateImprovementAchievementEntity saved = findScoped(achievementId, requester);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                auditJson(existing),
                auditJson(saved),
                requester.userId(),
                "취업률 제고 실적 수정",
                requestId);
        return new EmploymentRateImprovementSaveResponse(
                toResponse(saved),
                warning.warning(),
                warning.message());
    }

    private EmploymentRateImprovementAchievementEntity findScoped(
            Long achievementId,
            CurrentUser requester) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        EmploymentRateImprovementAchievementEntity entity = mapper.findScoped(
                achievementId,
                requester.userId(),
                requester.roles());
        if (entity == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return entity;
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null
                || requester.userId() == null
                || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
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

    private void validateRequest(EmploymentRateImprovementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "취업률 제고 실적 정보를 입력하세요."));
        } else {
            if (blankToNull(request.managementItemCode()) == null) {
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
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("취업률 제고 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private EmploymentRateImprovementAchievementResponse toResponse(
            EmploymentRateImprovementAchievementEntity entity) {
        return new EmploymentRateImprovementAchievementResponse(
                entity.achievementId(),
                entity.teacherLoginId(),
                entity.organizationCode(),
                entity.evaluationYear(),
                entity.managementItemCode(),
                entity.achievementDate(),
                entity.achievementStatus(),
                entity.specialLectureStartDate(),
                entity.specialLectureEndDate(),
                entity.mockExamQuestionPeriod(),
                deserializeAttachmentIds(entity.attachmentIdsJson()),
                entity.createdAt(),
                entity.updatedAt());
    }

    private String serializeAttachmentIds(List<String> attachmentIds) {
        List<String> normalized = attachmentIds == null
                ? List.of()
                : attachmentIds.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 식별자 형식이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 식별자를 확인하세요.")));
        }
    }

    private List<String> deserializeAttachmentIds(String attachmentIdsJson) {
        if (attachmentIdsJson == null || attachmentIdsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(attachmentIdsJson, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 첨부 식별자 형식이 올바르지 않습니다.", exception);
        }
    }

    private String auditJson(EmploymentRateImprovementAchievementEntity entity) {
        try {
            return objectMapper.writeValueAsString(toResponse(entity));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("취업률 제고 실적 감사 정보를 만들 수 없습니다.", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
