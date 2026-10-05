package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * Implements caller-scoped individual employment-rate achievement operations.
 * Excel and bulk execution remain policy-gated because their shared adapters and
 * OQ-83-01 decision are not present in this repository slice.
 */
@Service
public class EmploymentRateAchievementService {
    private static final List<String> READ_ROLES = List.of("R01", "R02", "R04");
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Returns only the caller's permitted rows using dynamic SQL filters. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementSearchResponse list(
            CurrentUser requester,
            int page,
            int pageSize,
            String managementItemCode,
            String achievementStatus) {
        requireReadRole(requester);
        String normalizedManagementItemCode = blankToNull(managementItemCode);
        String normalizedAchievementStatus = blankToNull(achievementStatus);
        List<EmploymentRateAchievementRow> rows = mapper.list(
                requester.userId(),
                requester.roles(),
                normalizedManagementItemCode,
                normalizedAchievementStatus,
                pageSize,
                page * pageSize);
        long total = mapper.count(
                requester.userId(),
                requester.roles(),
                normalizedManagementItemCode,
                normalizedAchievementStatus);
        return new EmploymentRateAchievementSearchResponse(rows, page, pageSize, total);
    }

    /** Retrieves one row after enforcing the caller's list-equivalent data scope. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        return requiredRow(achievementId, requester);
    }

    /** Creates a draft achievement after the shared period and finalization guards. */
    @Transactional
    public EmploymentRateAchievementSaveResponse create(
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        requireFacultyRole(requester);
        validateRequest(request);
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        String managementNo = "ERA-" + UUID.randomUUID();
        mapper.insert(command(null, managementNo, requester.userId(), request));
        EmploymentRateAchievementRow saved = mapper.findByManagementNo(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 취업률 실적을 찾을 수 없습니다.");
        }
        mapper.insertInitialStatusHistory(saved.achievementId(), requester.userId());
        mapper.insertChangeHistory(
                String.valueOf(saved.achievementId()),
                "CREATE",
                null,
                describe(saved),
                requester.userId(),
                "취업률 실적 등록");
        return new EmploymentRateAchievementSaveResponse(saved, warning.warning(), warning.message());
    }

    /** Updates an owned draft row without permitting target-user or status mutation. */
    @Transactional
    public EmploymentRateAchievementSaveResponse update(
            Long achievementId,
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        requireFacultyRole(requester);
        validateRequest(request);
        EmploymentRateAchievementRow existing = requiredRow(achievementId, requester);
        if (!requester.userId().equals(existing.teacherUserId())) {
            throw new ForbiddenException();
        }
        OccurredDateValidation warning = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        existing.evaluationYear(),
                        request.achievementDate()));
        EmploymentRateAchievementCommand command = command(
                achievementId,
                existing.managementNo(),
                existing.teacherUserId(),
                request);
        if (mapper.update(command) == 0) {
            throw new NotFoundException("수정할 취업률 실적을 찾을 수 없습니다.");
        }
        EmploymentRateAchievementRow updated = requiredRow(achievementId, requester);
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                describe(existing),
                describe(updated),
                requester.userId(),
                "취업률 실적 수정");
        return new EmploymentRateAchievementSaveResponse(updated, warning.warning(), warning.message());
    }

    /** Produces an Excel-compatible CSV download from the same scoped list query. */
    @Transactional(readOnly = true)
    public byte[] download(CurrentUser requester, int page, int pageSize) {
        EmploymentRateAchievementSearchResponse result = list(requester, page, pageSize, null, null);
        StringBuilder csv = new StringBuilder("관리번호,관리항목,업적발생일,실적명,상태\n");
        for (EmploymentRateAchievementRow row : result.achievements()) {
            csv.append(csvValue(row.managementNo())).append(',')
                    .append(csvValue(row.managementItemCode())).append(',')
                    .append(csvValue(String.valueOf(row.achievementDate()))).append(',')
                    .append(csvValue(row.achievementName())).append(',')
                    .append(csvValue(row.achievementStatus())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Rejects uploads until the mandated existing Excel and FileStoragePort
     * adapters are registered; this avoids a partial or non-durable upload.
     */
    public EmploymentRateExcelUploadResponse upload(MultipartFile file, CurrentUser requester, String requestId) {
        requireExcelRole(requester);
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException(
                    "업로드 파일을 선택하세요.",
                    List.of(new ValidationError("file", "업로드 파일을 선택하세요.")));
        }
        throw new ConflictException("EMPLOYMENT_RATE_EXCEL_ADAPTER_REQUIRED: 공통 Excel 및 파일 저장 연동이 필요합니다.");
    }

    /** Prevents unapproved OQ-83-01 generation or deletion jobs from being recorded. */
    public EmploymentRateBulkJobResponse createBulkJob(
            EmploymentRateBulkJobRequest request,
            CurrentUser requester,
            String requestId) {
        requireExcelRole(requester);
        validateBulkRequest(request);
        throw new ConflictException("BULK_POLICY_PENDING: OQ-83-01 승인 전에는 일괄 작업을 접수할 수 없습니다.");
    }

    /** Retrieves an R07-visible bulk-job aggregate result. */
    @Transactional(readOnly = true)
    public EmploymentRateBulkJobResponse getBulkJob(String jobId, CurrentUser requester) {
        requireExcelRole(requester);
        EmploymentRateBulkJobResponse result = mapper.findBulkJob(jobId);
        if (result == null) {
            throw new NotFoundException("일괄 작업을 찾을 수 없습니다.");
        }
        return result;
    }

    private EmploymentRateAchievementCommand command(
            Long achievementId,
            String managementNo,
            Long teacherUserId,
            EmploymentRateAchievementRequest request) {
        try {
            return new EmploymentRateAchievementCommand(
                    achievementId,
                    managementNo,
                    teacherUserId,
                    String.valueOf(Year.from(request.achievementDate())),
                    request.managementItemCode().trim(),
                    request.achievementDate(),
                    blankToNull(request.achievementName()),
                    objectMapper.writeValueAsString(
                            request.attachmentIds() == null ? List.of() : request.attachmentIds()),
                    teacherUserId);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "첨부 식별자 형식이 올바르지 않습니다.",
                    List.of(new ValidationError("attachmentIds", "첨부 식별자를 확인하세요.")));
        }
    }

    /**
     * Resolves one row through the same role precedence and organization scope as list queries.
     * An out-of-scope identifier is deliberately indistinguishable from a missing row.
     */
    private EmploymentRateAchievementRow requiredRow(Long achievementId, CurrentUser requester) {
        if (achievementId == null || achievementId <= 0) {
            throw new BusinessValidationException(
                    "실적 식별자가 올바르지 않습니다.",
                    List.of(new ValidationError("achievementId", "실적 식별자를 확인하세요.")));
        }
        EmploymentRateAchievementRow row = mapper.findScoped(
                achievementId,
                requester.userId(),
                requester.roles());
        if (row == null) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void validateRequest(EmploymentRateAchievementRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "취업률 실적 정보를 입력하세요.",
                    List.of(new ValidationError("body", "취업률 실적 정보를 입력하세요.")));
        }
    }

    private void validateBulkRequest(EmploymentRateBulkJobRequest request) {
        if (request == null || blankToNull(request.evaluationYear()) == null) {
            throw new BusinessValidationException(
                    "일괄 작업 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("evaluationYear", "평가연도를 입력하세요.")));
        }
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null
                || requester.roles().stream().noneMatch(READ_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireFacultyRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private void requireExcelRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null || !requester.roles().contains("R07")) {
            throw new ForbiddenException();
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String csvValue(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }

    private String describe(EmploymentRateAchievementRow row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("취업률 실적 변경이력을 직렬화할 수 없습니다.", exception);
        }
    }
}
