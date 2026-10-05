package kr.ac.knue.commonfoundation.employmentrateimprovements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates caller-scoped reads and atomic 취업률 제고 writes against the
 * shared education-achievement header while recording each successful change.
 */
@Service
public class EmploymentRateImprovementService {
    private static final List<String> READ_ROLES = List.of("R01", "R02", "R04");
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

    /** Returns only records permitted by the caller's education-achievement data scope. */
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

    /** Returns one permitted row or hides an out-of-scope row behind the normal forbidden response. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateImprovementRow row = findExisting(achievementId);
        requireAccessible(row, requester);
        return row;
    }

    /** Creates a draft record after shared scope, input-period, and finalization checks. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(
            EmploymentRateImprovementRequest request,
            CurrentUser requester) {
        validateRequest(request);
        requireWriteRole(requester);
        OccurredDateValidation validation = validateMutation(request, requester, requester.userId(), null);
        String managementNo = "ERI-" + UUID.randomUUID();
        String detailJson = detailJson(request);
        mapper.insert(
                managementNo,
                requester.userId(),
                String.valueOf(Year.from(request.achievementDate())),
                request.managementItemCode().trim(),
                request.achievementDate(),
                detailJson,
                attachmentIdsJson(request),
                requester.userId());
        EmploymentRateImprovementRow saved = mapper.findByManagementNo(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 취업률 제고 실적을 찾을 수 없습니다.");
        }
        mapper.insertChangeHistory(
                String.valueOf(saved.achievementId()),
                null,
                snapshot(request),
                requester.userId(),
                "취업률 제고 실적 등록");
        return result(saved, validation);
    }

    /** Updates an owned draft row and preserves complete comparable snapshots in data-change history. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(
            Long achievementId,
            EmploymentRateImprovementRequest request,
            CurrentUser requester) {
        validateRequest(request);
        requireWriteRole(requester);
        EmploymentRateImprovementRow existing = findExisting(achievementId);
        requireOwner(existing, requester);
        OccurredDateValidation validation = validateMutation(
                request,
                requester,
                existing.teacherUserId(),
                existing.evaluationYear());
        String detailJson = detailJson(request);
        int changed = mapper.update(
                existing.achievementId(),
                request.managementItemCode().trim(),
                request.achievementDate(),
                detailJson,
                attachmentIdsJson(request),
                requester.userId());
        if (changed == 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        EmploymentRateImprovementRow saved = findExisting(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(saved.achievementId()),
                snapshot(existing),
                snapshot(request),
                requester.userId(),
                "취업률 제고 실적 수정");
        return result(saved, validation);
    }

    private OccurredDateValidation validateMutation(
            EmploymentRateImprovementRequest request,
            CurrentUser requester,
            Long targetUserId,
            String existingEvaluationYear) {
        String evaluationYear = existingEvaluationYear == null
                ? String.valueOf(Year.from(request.achievementDate()))
                : existingEvaluationYear;
        return guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        targetUserId,
                        evaluationYear,
                        request.achievementDate()));
    }

    private EmploymentRateImprovementSaveResult result(
            EmploymentRateImprovementRow saved,
            OccurredDateValidation validation) {
        return new EmploymentRateImprovementSaveResult(
                saved,
                validation.warning(),
                validation.message());
    }

    private EmploymentRateImprovementRow findExisting(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        EmploymentRateImprovementRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
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

    private void requireAccessible(EmploymentRateImprovementRow row, CurrentUser requester) {
        if (mapper.countAccessible(row.achievementId(), requester.userId(), requester.roles()) == 0) {
            throw new ForbiddenException();
        }
    }

    private void requireOwner(EmploymentRateImprovementRow row, CurrentUser requester) {
        if (!requester.userId().equals(row.teacherUserId())) {
            throw new ForbiddenException();
        }
    }

    private void validateRequest(EmploymentRateImprovementRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "취업률 제고 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("body", "저장 정보를 입력하세요.")));
        }
        if (request.managementItemCode() == null || request.managementItemCode().isBlank()) {
            throw new BusinessValidationException(
                    "취업률 제고 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("managementItemCode", "관리항목을 입력하세요.")));
        }
        if (request.achievementDate() == null) {
            throw new BusinessValidationException(
                    "취업률 제고 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("achievementDate", "업적발생일을 입력하세요.")));
        }
        if (request.specialLectureStartDate() != null
                && request.specialLectureEndDate() != null
                && request.specialLectureStartDate().isAfter(request.specialLectureEndDate())) {
            throw new BusinessValidationException(
                    "특강 기간이 올바르지 않습니다.",
                    List.of(new ValidationError("specialLectureEndDate", "종료일은 시작일보다 빠를 수 없습니다.")));
        }
    }

    private String detailJson(EmploymentRateImprovementRequest request) {
        ObjectNode detail = objectMapper.createObjectNode();
        if (request.specialLectureStartDate() != null) {
            detail.put("specialLectureStartDate", request.specialLectureStartDate().toString());
        }
        if (request.specialLectureEndDate() != null) {
            detail.put("specialLectureEndDate", request.specialLectureEndDate().toString());
        }
        if (request.mockExamQuestionPeriod() != null && !request.mockExamQuestionPeriod().isBlank()) {
            detail.put("mockExamQuestionPeriod", request.mockExamQuestionPeriod().trim());
        }
        return serialize(detail);
    }

    private String attachmentIdsJson(EmploymentRateImprovementRequest request) {
        ArrayNode attachments = objectMapper.createArrayNode();
        if (request.attachmentIds() != null) {
            request.attachmentIds().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .forEach(attachments::add);
        }
        return serialize(attachments);
    }

    /**
     * Serializes one stable shape for audit comparisons so header, detail, and
     * attachment changes can be reviewed together.
     */
    private String snapshot(EmploymentRateImprovementRequest request) {
        return snapshot(
                request.managementItemCode().trim(),
                request.achievementDate(),
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                request.mockExamQuestionPeriod(),
                request.attachmentIds());
    }

    private String snapshot(EmploymentRateImprovementRow row) {
        return snapshot(
                row.managementItemCode(),
                row.achievementDate(),
                row.specialLectureStartDate(),
                row.specialLectureEndDate(),
                row.mockExamQuestionPeriod(),
                row.attachmentIds());
    }

    private String snapshot(
            String managementItemCode,
            java.time.LocalDate achievementDate,
            java.time.LocalDate specialLectureStartDate,
            java.time.LocalDate specialLectureEndDate,
            String mockExamQuestionPeriod,
            List<String> attachmentIds) {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("managementItemCode", managementItemCode);
        putDate(value, "achievementDate", achievementDate);
        putDate(value, "specialLectureStartDate", specialLectureStartDate);
        putDate(value, "specialLectureEndDate", specialLectureEndDate);
        value.put("mockExamQuestionPeriod", mockExamQuestionPeriod);
        ArrayNode attachments = value.putArray("attachmentIds");
        if (attachmentIds != null) {
            attachmentIds.stream()
                    .filter(attachmentId -> attachmentId != null && !attachmentId.isBlank())
                    .map(String::trim)
                    .forEach(attachments::add);
        }
        return serialize(value);
    }

    private void putDate(ObjectNode target, String fieldName, java.time.LocalDate value) {
        if (value == null) {
            target.putNull(fieldName);
            return;
        }
        target.put(fieldName, value.toString());
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "취업률 제고 실적 상세 입력값이 올바르지 않습니다.",
                    List.of(new ValidationError("body", "상세 입력값을 확인하세요.")));
        }
    }
}
