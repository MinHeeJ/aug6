package kr.ac.knue.commonfoundation.lectureimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates caller-scoped lecture-improvement reads and atomic writes so
 * validation, ownership, finalization protection, and audit history remain
 * consistent.
 */
@Service
public class LectureImprovementService {
    private final LectureImprovementMapper mapper;
    private final ObjectMapper objectMapper;

    public LectureImprovementService(LectureImprovementMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /** Returns only the current faculty member's visible lecture-improvement rows. */
    @Transactional(readOnly = true)
    public LectureImprovementSearchResponse list(int page, int pageSize, CurrentUser currentUser) {
        requireReadRole(currentUser);
        int safePage = Math.max(page, 0);
        return new LectureImprovementSearchResponse(
                mapper.listForOwner(currentUser.userId(), pageSize, safePage * pageSize),
                safePage,
                pageSize,
                mapper.countForOwner(currentUser.userId()));
    }

    /** Returns a visible achievement detail and prevents R01 from reading another faculty member's row. */
    @Transactional(readOnly = true)
    public LectureImprovementAchievement get(Long achievementId, CurrentUser currentUser) {
        requireReadRole(currentUser);
        LectureImprovementAchievement achievement = find(achievementId);
        if (currentUser.roles().contains("R01")
                && !currentUser.userId().equals(achievement.targetUserId())) {
            throw new ForbiddenException();
        }
        return achievement;
    }

    /** Creates an editable DRAFT row and records the resulting business change in one transaction. */
    @Transactional
    public LectureImprovementAchievement create(
            LectureImprovementRequest request,
            CurrentUser currentUser) {
        requireWriteRole(currentUser);
        validate(request);
        String managementNo = "LI-" + UUID.randomUUID();
        mapper.insert(currentUser.userId(), managementNo, request, serializeAttachments(request.attachmentIds()));
        LectureImprovementAchievement created = findByManagementNo(managementNo);
        mapper.insertChangeHistory(
                created.achievementId(),
                "CREATE",
                null,
                serializeAchievement(created),
                currentUser.userId(),
                "강의개선 실적 등록");
        return created;
    }

    /** Updates an own, non-finalized row and records its before/after values atomically. */
    @Transactional
    public LectureImprovementAchievement update(
            Long achievementId,
            LectureImprovementRequest request,
            CurrentUser currentUser) {
        requireWriteRole(currentUser);
        validate(request);
        LectureImprovementAchievement existing = find(achievementId);
        if (!currentUser.userId().equals(existing.targetUserId())) {
            throw new ForbiddenException();
        }
        if ("EVALUATION_CONFIRMED".equals(existing.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        if (mapper.update(
                achievementId,
                currentUser.userId(),
                request,
                serializeAttachments(request.attachmentIds())) == 0) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        LectureImprovementAchievement updated = find(achievementId);
        mapper.insertChangeHistory(
                achievementId,
                "UPDATE",
                serializeAchievement(existing),
                serializeAchievement(updated),
                currentUser.userId(),
                "강의개선 실적 수정");
        return updated;
    }

    private LectureImprovementAchievement find(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        LectureImprovementAchievement achievement = mapper.findById(achievementId);
        if (achievement == null) {
            throw new NotFoundException("강의개선 실적을 찾을 수 없습니다.");
        }
        return achievement;
    }

    private LectureImprovementAchievement findByManagementNo(String managementNo) {
        LectureImprovementAchievement achievement = mapper.findByManagementNo(managementNo);
        if (achievement == null) {
            throw new NotFoundException("저장한 강의개선 실적을 찾을 수 없습니다.");
        }
        return achievement;
    }

    private void validate(LectureImprovementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "강의개선 실적 정보를 입력하세요."));
        } else {
            if (isBlank(request.managementItemCode())) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.achievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
            if (isBlank(request.achievementContent())) {
                errors.add(new ValidationError("achievementContent", "실적내용을 입력하세요."));
            }
            if (request.academicYear() == null || request.academicYear() < 2000) {
                errors.add(new ValidationError("academicYear", "학년도를 확인하세요."));
            }
            if (request.semester() == null || (request.semester() != 1 && request.semester() != 2)) {
                errors.add(new ValidationError("semester", "학기는 1 또는 2만 입력할 수 있습니다."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("강의개선 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private void requireReadRole(CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null || currentUser.roles() == null
                || currentUser.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireWriteRole(CurrentUser currentUser) {
        if (currentUser == null || currentUser.userId() == null || currentUser.roles() == null
                || !currentUser.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private String serializeAttachments(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(attachmentIds == null ? List.of() : attachmentIds);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 참조가 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 참조를 확인하세요.")));
        }
    }

    private String serializeAchievement(LectureImprovementAchievement achievement) {
        try {
            return objectMapper.writeValueAsString(achievement);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("강의개선 이력을 기록할 수 없습니다.", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
