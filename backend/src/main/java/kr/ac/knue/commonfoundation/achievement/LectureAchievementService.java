package kr.ac.knue.commonfoundation.achievement;

import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists and retrieves lecture achievements through the shared BASIC-79 authorization guard.
 */
@Service
public class LectureAchievementService {
    private final LectureAchievementMapper mapper;
    private final LectureAchievementFoundationService foundationService;

    public LectureAchievementService(LectureAchievementMapper mapper,
                                                LectureAchievementFoundationService foundationService) {
        this.mapper = mapper;
        this.foundationService = foundationService;
    }

    /** Returns only achievement rows within the caller's established data scope. */
    @Transactional(readOnly = true)
    public LectureAchievementDtos.SearchResponse list(LectureAchievementDtos.SearchCriteria criteria,
                                                                  CurrentUser user) {
        return new LectureAchievementDtos.SearchResponse(mapper.list(criteria, user.userId()), criteria.safePage(),
                criteria.safeSize(), mapper.count(criteria, user.userId()));
    }

    /** Validates the common mutation preconditions, saves one row, and records before/after business values atomically. */
    @Transactional
    public LectureAchievementDtos.SaveResponse save(LectureAchievementDtos.SaveRequest request, CurrentUser user) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) throw new BusinessValidationException("강의실적 실적 입력값을 확인하세요.", fields);
        String functionType = request.achievementId() == null ? "CREATE" : "UPDATE";
        List<String> warnings = foundationService.validateMutation(new EducationAchievementMutationContext(request.achievementId(),
                request.evaluationYear().trim(), request.organizationCode().trim(), request.managementItemCode().trim(),
                request.occurredDate(), user, functionType));
        LectureAchievementDtos.Row before = request.achievementId() == null ? null : mapper.findById(request.achievementId());
        Long achievementId;
        if (request.achievementId() == null) achievementId = mapper.insert(request, user.userId(), user.userId());
        else {
            if (mapper.update(request.achievementId(), request, user.userId()) != 1) {
                throw new NotFoundException("강의실적 실적을 찾을 수 없습니다.");
            }
            achievementId = request.achievementId();
        }
        if (achievementId == null) throw new NotFoundException("저장한 강의실적 실적을 찾을 수 없습니다.");
        LectureAchievementDtos.Row after = mapper.findById(achievementId);
        if (after == null) throw new NotFoundException("저장한 강의실적 실적을 찾을 수 없습니다.");
        mapper.insertChangeHistory(String.valueOf(achievementId), before == null ? "CREATE" : "UPDATE", "achievement_detail",
                before == null ? null : String.valueOf(before.achievementDetail()), String.valueOf(after.achievementDetail()),
                user.userId(), request.changeReason().trim());
        return new LectureAchievementDtos.SaveResponse(after, warnings);
    }

    /** Changes only an opaque attachment reference after the shared confirmed-data lock has been checked. */
    @Transactional
    public LectureAchievementDtos.Row saveAttachmentReference(Long achievementId,
                                                                          LectureAchievementDtos.AttachmentReferenceRequest request,
                                                                          CurrentUser user) {
        LectureAchievementDtos.Row before = mapper.findById(achievementId);
        if (before == null) throw new NotFoundException("강의실적 실적을 찾을 수 없습니다.");
        foundationService.validateMutation(new EducationAchievementMutationContext(achievementId, before.evaluationYear(),
                before.organizationCode(), before.managementItemCode(), before.occurredDate(), user, "UPDATE"));
        if (mapper.updateAttachmentReference(achievementId, request.attachmentReference().trim(), user.userId()) != 1) {
            throw new NotFoundException("강의실적 실적을 찾을 수 없습니다.");
        }
        LectureAchievementDtos.Row after = mapper.findById(achievementId);
        mapper.insertChangeHistory(String.valueOf(achievementId), "UPDATE", "attachment_ref", before.attachmentReference(),
                after.attachmentReference(), user.userId(), request.changeReason().trim());
        return after;
    }


    private List<ValidationError> validate(LectureAchievementDtos.SaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || request.evaluationYear() == null || !request.evaluationYear().trim().matches("^[0-9]{4}$")) fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식으로 입력하세요."));
        if (request == null || request.organizationCode() == null || request.organizationCode().isBlank()) fields.add(new ValidationError("organizationCode", "소속 조직을 선택하세요."));
        if (request == null || request.managementItemCode() == null || request.managementItemCode().isBlank()) fields.add(new ValidationError("managementItemCode", "관리항목을 선택하세요."));
        if (request == null || request.occurredDate() == null) fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        if (request == null || request.changeReason() == null || request.changeReason().isBlank()) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        return fields;
    }
}
