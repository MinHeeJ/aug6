package kr.ac.knue.commonfoundation.f4_user_story_3;

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
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.Request;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.Row;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.SaveResponse;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.SearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns scoped reads and atomic create/update rules for teaching-improvement achievements. */
@Service
public class LectureImprovementService {
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

    /** Returns only rows visible to the requesting R01, R02, or R04 user. */
    @Transactional(readOnly = true)
    public SearchResponse list(int page, int pageSize, CurrentUser requester) {
        requireReadRole(requester);
        validatePage(page, pageSize);
        return new SearchResponse(
                mapper.list(page * pageSize, pageSize, requester.userId(), requester.roles()),
                page,
                pageSize,
                mapper.count(requester.userId(), requester.roles()));
    }

    /** Looks up a detail row through the same SQL scope predicate as the list operation. */
    @Transactional(readOnly = true)
    public Row get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        Row row = mapper.findById(achievementId, requester.userId(), requester.roles());
        if (row == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    /** Creates an R01-owned record after validation and the shared period/finalization guard. */
    @Transactional
    public SaveResponse create(
            Request request,
            CurrentUser requester,
            String requestId) {
        requireWriteRole(requester);
        validateRequest(request);
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        String managementNo = "LI-" + UUID.randomUUID();
        mapper.insert(
                managementNo,
                requester.userId(),
                String.valueOf(Year.from(request.achievementDate())),
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.achievementContent().trim(),
                request.academicYear(),
                request.semester(),
                attachmentJson(request),
                requester.userId());
        Row saved = findOwned(managementNo, requester);
        mapper.insertChangeHistory(
                String.valueOf(saved.achievementId()),
                "CREATE",
                null,
                saved.achievementContent(),
                requester.userId(),
                "강의개선 실적 등록",
                requestId);
        return new SaveResponse(saved, warning.warning(), warning.message());
    }

    /** Updates an R01-owned, non-finalized row; the scoped lookup prevents cross-user mutation. */
    @Transactional
    public SaveResponse update(
            Long achievementId,
            Request request,
            CurrentUser requester,
            String requestId) {
        requireWriteRole(requester);
        validateRequest(request);
        Row existing = get(achievementId, requester);
        if ("EVALUATION_CONFIRMED".equals(existing.certificationStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.targetUserId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        if (mapper.update(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.achievementContent().trim(),
                request.academicYear(),
                request.semester(),
                attachmentJson(request),
                requester.userId(),
                requester.userId()) == 0) {
            throw new ConflictException("강의개선 실적을 수정할 수 없습니다.");
        }
        Row saved = get(achievementId, requester);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                existing.achievementContent(),
                saved.achievementContent(),
                requester.userId(),
                "강의개선 실적 수정",
                requestId);
        return new SaveResponse(saved, warning.warning(), warning.message());
    }

    private Row findOwned(String managementNo, CurrentUser requester) {
        Row row = mapper.findByManagementNo(managementNo, requester.userId());
        if (row == null) {
            throw new NotFoundException("저장한 강의개선 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void validatePage(int page, int pageSize) {
        if (page < 0 || List.of(20, 50, 100).stream().noneMatch(size -> size == pageSize)) {
            throw new BusinessValidationException(
                    "목록 조건이 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private void validateRequest(Request request) {
        if (request == null) {
            throw new BusinessValidationException("강의개선 정보를 입력하세요.", List.of());
        }
        if (request.semester() == null || (request.semester() != 1 && request.semester() != 2)) {
            throw new BusinessValidationException(
                    "학기 값이 올바르지 않습니다.",
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
        if (requester == null || requester.roles() == null || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private String attachmentJson(Request request) {
        try {
            return objectMapper.writeValueAsString(
                    request.attachmentIds() == null
                            ? List.of()
                            : request.attachmentIds());
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부파일 식별자가 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부파일을 확인하세요.")));
        }
    }
}
