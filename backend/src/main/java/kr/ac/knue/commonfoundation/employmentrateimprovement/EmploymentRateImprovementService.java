package kr.ac.knue.commonfoundation.employmentrateimprovement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns caller-scoped 취업률 제고 reads and guarded, audited mutations. */
@Service
public class EmploymentRateImprovementService {
    private static final List<String> READ_ROLES = List.of("R01", "R02", "R04");
    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public EmploymentRateImprovementService(EmploymentRateImprovementMapper mapper,
            EducationAchievementGuardService guardService, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Returns only rows allowed by the caller's R01/R02/R04 data scope. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(int page, int pageSize, CurrentUser requester) {
        requireReadRole(requester);
        int safePage = Math.max(page, 0);
        int safePageSize = pageSize == 50 || pageSize == 100 ? pageSize : 20;
        return new EmploymentRateImprovementSearchResponse(
                mapper.list(safePageSize, safePage * safePageSize, requester.userId(), requester.roles()),
                safePage, safePageSize, mapper.count(requester.userId(), requester.roles()));
    }

    /** Reads one row using the same data-scope predicate as the list operation. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateImprovementRow row = mapper.findScoped(achievementId, requester.userId(), requester.roles());
        if (row == null) throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        return row;
    }

    /** Creates a DRAFT source row plus status and change histories atomically. */
    @Transactional
    public EmploymentRateImprovementSaveResult create(EmploymentRateImprovementRequest request,
            CurrentUser requester, String requestId) {
        validate(request);
        requireWriter(requester);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        guardService.validateMutation(requester, new EducationAchievementMutationContext(
                requester.userId(), evaluationYear, request.achievementDate()));
        String managementNo = "ERI-" + UUID.randomUUID();
        mapper.insert(managementNo, requester.userId(), evaluationYear, request.managementItemCode().trim(),
                request.achievementDate(), request.specialLectureStartDate(), request.specialLectureEndDate(),
                trimToNull(request.mockExamQuestionPeriod()),
                serializeAttachmentIds(request.attachmentIds()),
                requester.userId());
        EmploymentRateImprovementRow saved = mapper.findByManagementNo(managementNo);
        if (saved == null) throw new NotFoundException("저장한 취업률 제고 실적을 찾을 수 없습니다.");
        mapper.insertStatusHistory(saved.achievementId(), requester.userId());
        mapper.insertChangeHistory(String.valueOf(saved.achievementId()), "CREATE", null, snapshot(saved),
                requester.userId(), requestId);
        return new EmploymentRateImprovementSaveResult(saved);
    }

    /** Updates only an R01-owned row after scope, active-period, and finalization guards. */
    @Transactional
    public EmploymentRateImprovementSaveResult update(Long achievementId, EmploymentRateImprovementRequest request,
            CurrentUser requester, String requestId) {
        validate(request);
        requireWriter(requester);
        EmploymentRateImprovementRow existing = get(achievementId, requester);
        guardService.validateMutation(requester, new EducationAchievementMutationContext(existing.targetUserId(),
                existing.evaluationYear(), request.achievementDate()));
        mapper.update(achievementId, request.managementItemCode().trim(), request.achievementDate(),
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                trimToNull(request.mockExamQuestionPeriod()),
                serializeAttachmentIds(request.attachmentIds()), requester.userId());
        EmploymentRateImprovementRow saved = mapper.findById(achievementId);
        mapper.insertChangeHistory(String.valueOf(achievementId), "UPDATE", snapshot(existing), snapshot(saved),
                requester.userId(), requestId);
        return new EmploymentRateImprovementSaveResult(saved);
    }

    private void validate(EmploymentRateImprovementRequest request) {
        if (request == null) throw new BusinessValidationException("취업률 제고 실적 정보를 입력하세요.",
                List.of(new ValidationError("body", "저장 정보를 입력하세요.")));
        if (request.specialLectureStartDate() != null && request.specialLectureEndDate() != null
                && request.specialLectureEndDate().isBefore(request.specialLectureStartDate())) {
            throw new BusinessValidationException("특강 기간이 올바르지 않습니다.",
                    List.of(new ValidationError("specialLectureEndDate", "종료일은 시작일보다 빠를 수 없습니다.")));
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(READ_ROLES::contains)) throw new ForbiddenException();
    }

    private void requireWriter(CurrentUser requester) {
        if (requester == null || requester.roles() == null || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private String serializeAttachmentIds(List<String> attachmentIds) {
        try { return objectMapper.writeValueAsString(attachmentIds == null ? List.of() : attachmentIds); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("첨부 참조값 직렬화에 실패했습니다.", exception); }
    }

    private String snapshot(EmploymentRateImprovementRow row) {
        try { return objectMapper.writeValueAsString(row); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("감사 이력 직렬화에 실패했습니다.", exception); }
    }

    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
