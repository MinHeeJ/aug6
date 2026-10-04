package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.CreateCommand;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.Request;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.Row;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.SaveResult;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.SearchCriteria;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.SearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements caller-scoped reads and atomic lecture-improvement write commands.
 * It uses the shared education guard before mutation and records required audit
 * and lifecycle side effects in the same transaction as header/detail changes.
 */
@Service
public class LectureImprovementAchievementService {
    private static final String ACHIEVEMENT_TYPE = "LECTURE_IMPROVEMENT";
    private final LectureImprovementAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public LectureImprovementAchievementService(
            LectureImprovementAchievementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Returns only rows within the authenticated caller's established data scope. */
    @Transactional(readOnly = true)
    public SearchResponse list(SearchCriteria criteria, CurrentUser requester) {
        requireReadRole(requester);
        SearchCriteria safeCriteria = criteria == null ? new SearchCriteria(0, 20) : criteria;
        return new SearchResponse(
                mapper.list(safeCriteria, requester.userId(), requester.roles()),
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.count(safeCriteria, requester.userId(), requester.roles()));
    }

    /** Reads a single lecture-improvement row only if it remains in caller scope. */
    @Transactional(readOnly = true)
    public Row get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        Row row = mapper.findScoped(achievementId, requester.userId(), requester.roles());
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Creates a lecture-improvement header/detail pair after all shared write guards pass. */
    @Transactional
    public SaveResult create(Request request, CurrentUser requester, String requestId) {
        validate(request);
        requireWriteRole(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String organizationCode = mapper.findOrganizationCode(requester.userId());
        if (organizationCode == null) {
            throw new ForbiddenException();
        }
        String attachmentIds = serializeAttachmentIds(request.attachmentIds());
        CreateCommand command = new CreateCommand(
                requester.userId(),
                organizationCode,
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                achievementName(request),
                attachmentIds,
                requester.userId());
        mapper.insertAchievement(command);
        Long achievementId = command.getAchievementId();
        if (achievementId == null) {
            throw new NotFoundException("저장한 강의개선 실적을 찾을 수 없습니다.");
        }
        mapper.insertDetail(
                achievementId,
                request.achievementContent().trim(),
                request.academicYear(),
                request.semester());
        mapper.insertStatusHistory(
                achievementId,
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                "강의개선 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now());
        Row saved = requireRow(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "CREATE",
                null,
                changeSnapshot(saved),
                requester.userId(),
                "강의개선 실적 저장",
                requestId);
        return new SaveResult(saved, warning.warning(), warning.message());
    }

    /** Updates a draft-capable row only after ownership, period, and finalization checks. */
    @Transactional
    public SaveResult update(Long achievementId, Request request, CurrentUser requester, String requestId) {
        validate(request);
        requireWriteRole(requester);
        Row existing = requireRow(achievementId);
        requireOwner(requester, existing);
        if (EducationAchievementStatus.EVALUATION_CONFIRMED.name().equals(existing.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        OccurredDateValidation warning = guardService.validateMutation(
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
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        mapper.updateDetail(
                achievementId,
                request.achievementContent().trim(),
                request.academicYear(),
                request.semester());
        Row saved = requireRow(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                changeSnapshot(existing),
                changeSnapshot(saved),
                requester.userId(),
                "강의개선 실적 수정",
                requestId);
        return new SaveResult(saved, warning.warning(), warning.message());
    }

    private Row requireRow(Long achievementId) {
        Row row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /**
     * Enforces the R01 self-service boundary before a row's state is revealed
     * or any update guard can run. The shared guard applies the same rule, but
     * this explicit check keeps cross-owner commands from observing lock state.
     */
    private void requireOwner(CurrentUser requester, Row row) {
        if (!requester.userId().equals(row.teacherUserId())) {
            throw new ForbiddenException();
        }
    }

    private void validate(Request request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "강의개선 실적 정보를 입력하세요.",
                    List.of(new ValidationError("body", "저장 정보를 입력하세요.")));
        }
        if (request.semester() != null && request.semester() != 1 && request.semester() != 2) {
            throw new BusinessValidationException(
                    "강의개선 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("semester", "학기는 1 또는 2여야 합니다.")));
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

    private String serializeAttachmentIds(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(attachmentIds == null ? List.of() : attachmentIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 식별자 형식이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 식별자를 확인하세요.")));
        }
    }

    private String achievementName(Request request) {
        return request.academicYear() + "학년도 " + request.semester() + "학기 강의개선";
    }

    private String changeSnapshot(Row row) {
        return row.achievementContent() + "|" + row.academicYear() + "|" + row.semester();
    }
}
