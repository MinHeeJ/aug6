package kr.ac.knue.commonfoundation.lectureimprovements;

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
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates scoped lecture-improvement reads and atomic header/detail writes,
 * preserving mandatory lifecycle and data-change history for each mutation.
 */
@Service
public class LectureImprovementService {
    private static final String ACHIEVEMENT_TYPE = "LECTURE_IMPROVEMENT";
    private final LectureImprovementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public LectureImprovementService(
            LectureImprovementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Returns only lecture-improvement records visible in the caller's approved data scope. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(
            LectureImprovementSearchCriteria criteria,
            CurrentUser requester) {
        requireReader(requester);
        LectureImprovementSearchCriteria safeCriteria = criteria == null
                ? new LectureImprovementSearchCriteria(0, 20)
                : criteria;
        return new LectureImprovementSearchResponse(
                mapper.list(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.count(requester.userId(), requester.roles()));
    }

    /** Retrieves one record only after applying the same role and data-scope restriction as the list. */
    @Transactional(readOnly = true)
    public LectureImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReader(requester);
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        LectureImprovementRow row = mapper.findVisible(achievementId, requester.userId(), requester.roles());
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Creates a draft header/detail pair and all required audit records in one transaction. */
    @Transactional
    public LectureImprovementRow create(
            LectureImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriter(requester);
        String organizationCode = requireOrganization(requester.userId());
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String managementNo = "LI-" + UUID.randomUUID();
        mapper.insertHeader(
                managementNo,
                requester.userId(),
                organizationCode,
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.achievementContent().trim(),
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        Long achievementId = mapper.findIdByManagementNo(managementNo);
        if (achievementId == null) {
            throw new NotFoundException("저장한 강의개선 실적을 찾을 수 없습니다.");
        }
        mapper.insertDetail(
                achievementId,
                request.achievementContent().trim(),
                request.academicYear(),
                request.semester(),
                requester.userId());
        LectureImprovementRow materialized = mapper.findByManagementNo(managementNo);
        if (materialized == null) {
            throw new NotFoundException("저장한 강의개선 실적을 찾을 수 없습니다.");
        }
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                ACHIEVEMENT_TYPE,
                materialized.achievementId(),
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "강의개선 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()));
        mapper.insertChangeHistory(
                "education_achievements",
                String.valueOf(materialized.achievementId()),
                "CREATE",
                "lecture_improvement_achievement_details",
                null,
                auditValue(materialized),
                requester.userId(),
                "강의개선 실적 저장",
                requestId);
        return materialized;
    }

    /** Updates an editable record atomically; finalized records are rejected before any mapper mutation. */
    @Transactional
    public LectureImprovementRow update(
            Long achievementId,
            LectureImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireWriter(requester);
        LectureImprovementRow existing = getForWrite(achievementId, requester);
        if (EducationAchievementStatus.EVALUATION_CONFIRMED.name().equals(existing.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        evaluationYear,
                        request.achievementDate()));
        mapper.updateHeader(
                existing.achievementId(),
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        mapper.updateDetail(
                existing.achievementId(),
                request.achievementContent().trim(),
                request.academicYear(),
                request.semester(),
                requester.userId());
        LectureImprovementRow saved = mapper.findByManagementNo(existing.managementNo());
        if (saved == null) {
            throw new NotFoundException("수정한 강의개선 실적을 찾을 수 없습니다.");
        }
        mapper.insertChangeHistory(
                "education_achievements",
                String.valueOf(existing.achievementId()),
                "UPDATE",
                "lecture_improvement_achievement_details",
                auditValue(existing),
                auditValue(saved),
                requester.userId(),
                "강의개선 실적 수정",
                requestId);
        return saved;
    }

    private LectureImprovementRow getForWrite(Long achievementId, CurrentUser requester) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        LectureImprovementRow row = mapper.findVisible(achievementId, requester.userId(), requester.roles());
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private String requireOrganization(Long userId) {
        String organizationCode = mapper.findActiveOrganizationCode(userId);
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new ConflictException("소속 조직을 찾을 수 없어 강의개선 실적을 저장할 수 없습니다.");
        }
        return organizationCode;
    }

    private void validate(LectureImprovementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "강의개선 실적 정보를 입력하세요."));
        } else {
            if (blankToNull(request.managementItemCode()) == null) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.achievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
            if (blankToNull(request.achievementContent()) == null) {
                errors.add(new ValidationError("achievementContent", "강의개선 내용을 입력하세요."));
            }
            if (request.academicYear() == null || request.academicYear() < 2000) {
                errors.add(new ValidationError("academicYear", "학년도는 2000년 이상이어야 합니다."));
            }
            if (request.semester() == null || (request.semester() != 1 && request.semester() != 2)) {
                errors.add(new ValidationError("semester", "학기는 1 또는 2여야 합니다."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("강의개선 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private void requireReader(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireWriter(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private String serializeAttachmentIds(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attachmentIds.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .toList());
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 참조값이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 참조값을 확인하세요.")));
        }
    }

    private String auditValue(LectureImprovementRow row) {
        return "academicYear=" + row.academicYear()
                + ",semester=" + row.semester()
                + ",achievementContent=" + row.achievementContent();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
