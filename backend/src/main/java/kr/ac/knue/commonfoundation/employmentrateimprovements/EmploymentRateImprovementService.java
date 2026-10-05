package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.List;
import java.util.UUID;
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

/** Owns scoped reads and atomic source/detail/audit transactions for 취업률 제고 achievements. */
@Service
public class EmploymentRateImprovementService {
    private static final String ACHIEVEMENT_TYPE = "EMPLOYMENT_RATE_IMPROVEMENT";
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

    /** Lists only rows visible to the authenticated role and data scope. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            EmploymentRateImprovementSearchCriteria criteria,
            CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateImprovementSearchCriteria safeCriteria = criteria == null
                ? new EmploymentRateImprovementSearchCriteria(0, 20)
                : criteria;
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.count(requester.userId(), requester.roles()));
    }

    /** Returns an achievement only after the shared data-scope guard has approved the owner. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateImprovementRow row = findExisting(achievementId);
        if (requester.roles().contains("R01") && !requester.userId().equals(row.teacherUserId())) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Creates a DRAFT source/detail pair and writes lifecycle plus data-change history atomically. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriteRole(requester);
        OccurredDateValidation validation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        String managementNo = "ERI-" + UUID.randomUUID();
        mapper.insertSource(
                managementNo,
                requester.userId(),
                "KNUE-DEPT-COMP",
                String.valueOf(Year.from(request.achievementDate())),
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentRefs(request),
                requester.userId());
        EmploymentRateImprovementRow saved = findByManagementNo(managementNo);
        mapper.insertDetail(
                saved.achievementId(),
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                trimToNull(request.mockExamQuestionPeriod()),
                requester.userId());
        mapper.insertStatusHistory(saved.achievementId(), requester.userId());
        EmploymentRateImprovementRow result = findExisting(saved.achievementId());
        mapper.insertChangeHistory(
                String.valueOf(result.achievementId()),
                "CREATE",
                null,
                result.achievementName(),
                requester.userId(),
                requestId);
        return new EmploymentRateImprovementSaveResult(result, validation.warning(), validation.message());
    }

    /** Updates an owned DRAFT source/detail pair after finalization and input-period guards. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(
            Long achievementId,
            EmploymentRateImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriteRole(requester);
        EmploymentRateImprovementRow existing = findExisting(achievementId);
        OccurredDateValidation validation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(), existing.evaluationYear(), request.achievementDate()));
        mapper.updateSource(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentRefs(request),
                requester.userId());
        mapper.updateDetail(
                achievementId,
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                trimToNull(request.mockExamQuestionPeriod()),
                requester.userId());
        EmploymentRateImprovementRow result = findExisting(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                existing.achievementName(),
                result.achievementName(),
                requester.userId(),
                requestId);
        return new EmploymentRateImprovementSaveResult(result, validation.warning(), validation.message());
    }

    private EmploymentRateImprovementRow findExisting(Long achievementId) {
        EmploymentRateImprovementRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private EmploymentRateImprovementRow findByManagementNo(String managementNo) {
        EmploymentRateImprovementRow row = mapper.findByManagementNo(managementNo);
        if (row == null) {
            throw new NotFoundException("저장한 취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void validate(EmploymentRateImprovementRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "취업률 제고 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("body", "취업률 제고 실적 정보를 입력하세요.")));
        }
        if (request.specialLectureStartDate() != null
                && request.specialLectureEndDate() != null
                && request.specialLectureEndDate().isBefore(request.specialLectureStartDate())) {
            throw new BusinessValidationException(
                    "취업률 제고 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("specialLectureEndDate", "종료일은 시작일보다 빠를 수 없습니다.")));
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null
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

    private String attachmentRefs(EmploymentRateImprovementRequest request) {
        try {
            return objectMapper.writeValueAsString(
                    request.attachmentIds() == null ? List.of() : request.attachmentIds());
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 참조가 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 참조를 확인하세요.")));
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
